package main.java;
import redis.clients.jedis.Jedis;

public class LogWorker {
    private Jedis jedis;
    private boolean debugMode;

    public LogWorker(String redisHost, boolean debugMode) {
        jedis = new Jedis(redisHost);
        this.debugMode = debugMode;
    }

    public void start() {
        while (true) {
            String message = jedis.lpop("decrypted_queue");
            if (message != null) {
                logMessage(message);
            }
        }
    }

    private void logMessage(String message) {
        if (debugMode) {
            System.out.println("ISO Dump: " + parseToISO(message));
        } else {
            System.out.println("Log Message: " + message);
        }
    }

    private String parseToISO(String message) {
        // Mã giả parse sang dạng ISO
        return "ISO Data [" + message + "]";
    }
}
