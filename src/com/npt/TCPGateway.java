package com.npt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Redis client
import redis.clients.jedis.Jedis;

public class TCPGateway {
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Jedis redisClient;

    public TCPGateway(String redisHost) {
        // Kết nối đến Redis
        this.redisClient = new Jedis(redisHost);
    }

    public void start(int port) {
        System.out.println("TCP Gateway started on port: " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (InputStream input = clientSocket.getInputStream()) {
            // Đọc dữ liệu được mã hóa từ client
            byte[] encryptedData = input.readAllBytes();
            System.out.println("Received encrypted data: " + Base64.getEncoder().encodeToString(encryptedData));

            // Lấy AES key từ Redis (map theo cổng hoặc ID)
            String aesKey = redisClient.get("aes_key");
            if (aesKey == null) {
                throw new RuntimeException("AES Key not found in Redis");
            }

            // Giải mã dữ liệu
            byte[] decryptedData = decryptAES(encryptedData, aesKey);
            System.out.println("Decrypted data: " + new String(decryptedData));

            // Chuyển tiếp dữ liệu tới Gateway kế tiếp
            forwardToNextGateway(decryptedData);

            // Nếu chế độ debug được bật, log message ISO
            if (DebugMode.isOn()) {
                logISOMessage(decryptedData);
            }
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private byte[] decryptAES(byte[] data, String aesKey) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(aesKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    private void forwardToNextGateway(byte[] data) {
        String nextGatewayHost = redisClient.get("next_gateway_host");
        String nextGatewayPort = redisClient.get("next_gateway_port");

        if (nextGatewayHost == null || nextGatewayPort == null) {
            System.err.println("Next gateway information not found in Redis");
            return;
        }

        try (Socket socket = new Socket(nextGatewayHost, Integer.parseInt(nextGatewayPort));
             OutputStream output = socket.getOutputStream()) {
            output.write(data);
            output.flush();
            System.out.println("Forwarded data to gateway: " + nextGatewayHost + ":" + nextGatewayPort);
        } catch (IOException e) {
            System.err.println("Failed to forward data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logISOMessage(byte[] data) {
        try {
            String message = new String(data);
            System.out.println("Logging ISO message: " + message);

            // Giả lập ISO parse hoặc thay bằng thư viện jPOS
            String isoDump = parseToISO(message);
            System.out.println("ISO Dump:\n" + isoDump);
        } catch (Exception e) {
            System.err.println("Failed to log ISO message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String parseToISO(String message) {
        // Giả lập parse ISO 8583
        return "Parsed ISO Data: [" + message + "]";
    }
}
