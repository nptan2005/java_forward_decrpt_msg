package main.java;
import redis.clients.jedis.Jedis;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class DecryptionWorker {
    private Jedis jedis;

    public DecryptionWorker(String redisHost) {
        jedis = new Jedis(redisHost);
    }

    public void start() {
        while (true) {
            String encryptedMessage = jedis.lpop("encrypted_queue");
            if (encryptedMessage != null) {
                String decryptedMessage = decryptAES(encryptedMessage, "your-aes-key");
                jedis.rpush("decrypted_queue", decryptedMessage); // Đẩy vào hàng đợi tiếp theo
                System.out.println("Decrypted Message: " + decryptedMessage);
            }
        }
    }

    private String decryptAES(String data, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(data));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
