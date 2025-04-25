package main.java;
import redis.clients.jedis.Jedis;
import java.io.OutputStream;
import java.net.Socket;

public class ForwardWorker {
    private Jedis jedis;

    public ForwardWorker(String redisHost) {
        jedis = new Jedis(redisHost);
    }

    public void start() {
        while (true) {
            String message = jedis.lpop("decrypted_queue");
            if (message != null) {
                forwardMessage("next-gateway-ip", 9090, message); // Gateway đích
            }
        }
    }

    private void forwardMessage(String host, int port, String message) {
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream()) {
            out.write(message.getBytes());
            out.flush();
            System.out.println("Forwarded Message: " + message);
        } catch (Exception e) {
            throw new RuntimeException("Forwarding failed", e);
        }
    }
}
