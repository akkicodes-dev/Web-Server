import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Simple HTTP client for manual testing of the SingleThreaded server.
 * For load testing, use Apache JMeter instead (see jmeter/ directory).
 */
public class Client {

    public void run() throws UnknownHostException, IOException {
        int port = 8010;  // FIXED: was 8090 — must match server port
        InetAddress address = InetAddress.getByName("localhost");

        // try-with-resources: Socket + streams auto-closed when done
        try (Socket socket = new Socket(address, port);
             PrintWriter toSocket = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader fromSocket = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            // Send a valid HTTP GET request (required by the updated server)
            toSocket.println("GET / HTTP/1.1");
            toSocket.println("Host: localhost");
            toSocket.println("");  // Blank line ends HTTP headers

            // Read and print the full HTTP response
            String line;
            while ((line = fromSocket.readLine()) != null) {
                System.out.println(line);
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.run();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
