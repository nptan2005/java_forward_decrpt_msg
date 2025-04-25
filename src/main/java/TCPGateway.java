package main.java;
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
