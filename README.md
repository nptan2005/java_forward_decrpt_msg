# Yêu cầu Giải pháp Gateway Forward và Decrypt Message
## Giải pháp gateway TCP/IP Socket - viết bằng java, chạy trên docker: 
### 1. nhận encryption data (AES-256)
### 2. descrytion data nhận được và forword đến 1 gateway khác để đảm bảo PCI DSS.
### 3. Gateway phải có perfermance cao.
### 4. cấu hình được key AES theo port nhận được.
### 5. Cấu hình chế độ debug, khi debug on: sẽ parse message sau khi decryption ra iso dump.
### 6. Chạy được song song, và scal in/out theo cơ chế K8S, đề xuất của tôi là thêm redis cache để tư message nhận và điều phối cho worker (đảm nhận decryption, đảm nhận fw, đảm nhận log khi debug)

# Thiết kế kiến trúc giải pháp
## Nhận dữ liệu mã hóa AES-256:

Sử dụng thư viện mã hóa phổ biến như javax.crypto để giải mã dữ liệu nhận được.

Cấu hình socket TCP/IP lắng nghe trên các cổng được chỉ định.

## Giải mã dữ liệu và chuyển tiếp đến gateway khác:

Mỗi thông điệp nhận được sẽ được giải mã bằng khóa AES được cấu hình theo cổng nhận.

Sau khi giải mã, dữ liệu sẽ được chuyển tiếp đến gateway khác qua HTTP hoặc TCP, đảm bảo giao tiếp an toàn (ví dụ: TLS).

## Hiệu suất cao:

Tận dụng luồng không đồng bộ (asynchronous I/O) với java.nio hoặc framework như Netty để giảm độ trễ.

Sử dụng công cụ tối ưu hóa như JIT của JVM và profiling để đảm bảo hiệu suất.

## Cấu hình AES key theo port:

Lưu trữ và quản lý các khóa AES trong Redis hoặc một cơ chế lưu trữ an toàn như HashiCorp Vault.

Map cổng với key tương ứng.

## Chế độ debug:

Khi debug mode bật, parse dữ liệu đã giải mã thành ISO dump (ví dụ: thông điệp ISO 8583).

Log thông tin với các công cụ như Logback hoặc SLF4J, lưu trữ log trong hệ thống quản lý log tập trung (ví dụ: ELK stack).

## Khả năng chạy song song & scaling:

Sử dụng Docker để containerize ứng dụng.

Triển khai trên Kubernetes, cấu hình autoscaling dựa trên CPU/Memory.

Redis Cache sẽ điều phối thông điệp đến các worker chuyên biệt:

Worker 1: Giải mã dữ liệu.

Worker 2: Chuyển tiếp dữ liệu.

Worker 3: Ghi log (khi debug).

```java
public class TCPGateway {
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    
    public void start(int port, String aesKey) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket, aesKey));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void handleClient(Socket clientSocket, String aesKey) {
        try (InputStream input = clientSocket.getInputStream()) {
            byte[] encryptedData = input.readAllBytes();
            byte[] decryptedData = AESUtil.decrypt(encryptedData, aesKey);
            
            forwardToNextGateway(decryptedData);
            if (DebugMode.isOn()) {
                logISOMessage(decryptedData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void forwardToNextGateway(byte[] data) {
        // Logic chuyển tiếp dữ liệu đến gateway khác
    }

    private void logISOMessage(byte[] data) {
        // Logic parse và log dữ liệu dạng ISO
    }
}

```

## Triển khai Docker & Kubernetes:
### Dockerfile:
``` Dockerfile
FROM openjdk:17-jdk-alpine
COPY . /app
WORKDIR /app
RUN javac TCPGateway.java
CMD ["java", "TCPGateway"]
```

### Redis Cache:
Redis được dùng để lưu trữ các thông điệp nhận và đảm bảo việc điều phối giữa các worker.

### Kubernetes:

Tạo Deployment với liveness và readiness probes.

Sử dụng Horizontal Pod Autoscaler (HPA) để scale.

# Thực hiện:

## 1. Quản lý khóa AES theo port
Bạn cần có cơ chế quản lý các khóa AES và ánh xạ theo port cụ thể. Redis là một lựa chọn lý tưởng cho mục đích này nhờ hiệu suất cao và khả năng truy cập dữ liệu nhanh.

Lưu trữ khóa AES trong Redis:

Redis Hash được sử dụng để ánh xạ giữa port và AES key.

### Cấu trúc lưu trữ ví dụ:
```plaintext
HSET aes_keys 8080 "my-secret-key-for-port-8080"
HSET aes_keys 9090 "my-secret-key-for-port-9090"
```
### Lấy khóa AES trong mã Java:

```java
import redis.clients.jedis.Jedis;

public class AESKeyManager {
    private Jedis jedis;

    public AESKeyManager(String redisHost) {
        jedis = new Jedis(redisHost);
    }

    public String getKeyForPort(int port) {
        return jedis.hget("aes_keys", String.valueOf(port));
    }
}
```
## 2. Luồng xử lý dữ liệu song song
Sử dụng Netty hoặc Java NIO để xây dựng hệ thống xử lý song song, tối ưu hóa việc xử lý I/O.

Sử dụng Netty: Netty là một framework mạnh mẽ, hiệu suất cao cho TCP/IP.

```java
public class NettyServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new AESDecryptionHandler(), new ForwardHandler());
                 }
             });

            b.bind(8080).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```
### Đảm bảo song song hóa:

Với Netty, mỗi kết nối được xử lý trên một thread riêng biệt, tận dụng luồng không đồng bộ.

Các worker đảm nhận từng bước (decryption, forwarding, logging) sẽ hoạt động độc lập nhưng được điều phối thông qua Redis Queue.

## 3. Debug và Logging dữ liệu dạng ISO
ISO 8583 là một định dạng phổ biến trong giao dịch tài chính. Khi chế độ debug được bật, bạn cần parse và log dữ liệu theo chuẩn ISO.

Thư viện hỗ trợ:

Sử dụng thư viện jPOS để xử lý thông điệp ISO 8583.

Mã ví dụ parse ISO:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

public class ISOParser {
    public static void parseISO(byte[] data) throws Exception {
        GenericPackager packager = new GenericPackager("path/to/iso8583.xml");
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(packager);
        isoMsg.unpack(data);

        for (int i = 0; i <= isoMsg.getMaxField(); i++) {
            if (isoMsg.hasField(i)) {
                System.out.println("Field (" + i + "): " + isoMsg.getString(i));
            }
        }
    }
}
```
Khi debug bật:

Gọi hàm parseISO và lưu log bằng công cụ Logback.

## 4. Scaling với Redis và Kubernetes
Đảm bảo ứng dụng có thể scale bằng cách sử dụng Redis để phân chia công việc và Kubernetes để quản lý pods.

### Redis Queue:

Worker đọc thông điệp từ hàng đợi Redis, mỗi nhiệm vụ được giao độc lập.

```java
Jedis jedis = new Jedis("localhost");
String message = jedis.lpop("message_queue");
processMessage(message);
```
### Kubernetes Config:

Deployment file:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tcp-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: tcp-gateway
  template:
    metadata:
      labels:
        app: tcp-gateway
    spec:
      containers:
      - name: tcp-gateway
        image: your-docker-image
        ports:
        - containerPort: 8080
        env:
        - name: REDIS_HOST
          value: redis-service
```
### HPA (Horizontal Pod Autoscaler):

```yaml
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: tcp-gateway
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: tcp-gateway
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

