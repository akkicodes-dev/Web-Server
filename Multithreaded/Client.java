import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Concurrent HTTP client for manual testing of the Multithreaded server.
 * Spawns 100 threads to simulate concurrent connections.
 * For proper load testing, use Apache JMeter instead (see jmeter/ directory).
 */
public class Client {

    /**
     * Returns a Runnable that connects to the server and sends an HTTP request.
     * Original method signature preserved from initial codebase.
     */
    public Runnable getRunnable() throws UnknownHostException, IOException {
        return new Runnable() {
            @Override
            public void run() {
                int port = 8010;
                try {
                    InetAddress address = InetAddress.getByName("localhost");
                    // try-with-resources: Socket + streams auto-closed
                    try (Socket socket = new Socket(address, port);
                         PrintWriter toSocket = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader fromSocket = new BufferedReader(
                                 new InputStreamReader(socket.getInputStream()))) {

                        // Send a valid HTTP GET request
                        toSocket.println("GET / HTTP/1.1");
                        toSocket.println("Host: localhost");
                        toSocket.println("");  // Blank line ends HTTP headers

                        // Read HTTP response
                        String line;
                        StringBuilder response = new StringBuilder();
                        while ((line = fromSocket.readLine()) != null) {
                            response.append(line).append("\n");
                        }
                        System.out.println("Response from Server:\n" + response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public static void main(String[] args) {
        Client client = new Client();
        for (int i = 0; i < 100; i++) {
            try {
                Thread thread = new Thread(client.getRunnable());
                thread.start();
            } catch (Exception ex) {
                return;
            }
        }
    }
}
