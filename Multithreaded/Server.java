import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * THREAD-PER-REQUEST WEB SERVER
 * 
 * Architecture: A new OS thread is created for EVERY incoming connection.
 * 
 * INTERVIEW INSIGHT:
 * - BENEFIT:  Multiple clients are served concurrently (parallel handling)
 * - DANGER:   Each thread consumes ~1MB stack memory (OS default)
 *             1000 concurrent clients = ~1GB just for thread stacks
 * - DANGER:   OS has thread limits (typically 10K-30K on Linux)
 * - DANGER:   Context switching overhead grows with thread count
 * - DANGER:   No backpressure — server accepts unlimited connections
 * - FAILURE:  Under extreme load -> OutOfMemoryError / OS thread exhaustion
 * 
 * This demonstrates WHY thread pools with bounded concurrency are preferred
 * over unlimited thread creation in production systems.
 */
public class Server {

    // ============================================================
    // CONFIGURATION
    // ============================================================
    private static final int PORT = 8010;

    /**
     * ARTIFICIAL DELAY (milliseconds) — For concurrency testing only.
     * In thread-per-request model, each thread sleeps independently.
     * Concurrent clients are NOT blocked by each other's delays.
     * Compare JMeter results against SingleThreaded to see the difference.
     */
    private static final int REQUEST_DELAY_MS = 100;

    // ============================================================
    // REQUEST HANDLER
    // ============================================================

    /**
     * Returns a Consumer that handles a single client connection.
     * Each invocation runs in its OWN dedicated OS thread.
     * 
     * Original method signature preserved from initial codebase.
     */
    public Consumer<Socket> getConsumer() {
        return (clientSocket) -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream();

                // --- Read HTTP request (request line + headers until blank line) ---
                String requestLine = reader.readLine();
                if (requestLine == null || requestLine.isEmpty()) return;

                String header;
                while ((header = reader.readLine()) != null && !header.isEmpty()) {
                    // consume headers
                }

                // --- Parse request path from "GET /path HTTP/1.1" ---
                String[] parts = requestLine.split(" ");
                String path = (parts.length > 1) ? parts[1] : "/";

                // --- Artificial delay — each thread sleeps independently ---
                if (REQUEST_DELAY_MS > 0) {
                    Thread.sleep(REQUEST_DELAY_MS);
                }

                // --- Route and build response ---
                String statusLine;
                String body;
                switch (path) {
                    case "/":
                        statusLine = "HTTP/1.1 200 OK";
                        body = "Hello from Multithreaded Web Server";
                        break;
                    case "/health":
                        statusLine = "HTTP/1.1 200 OK";
                        body = "OK";
                        break;
                    default:
                        statusLine = "HTTP/1.1 404 Not Found";
                        body = "404 Not Found";
                        break;
                }

                // --- Send valid HTTP response ---
                String httpResponse = statusLine + "\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "Content-Length: " + body.length() + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                        + body;

                out.write(httpResponse.getBytes());
                out.flush();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Multithreaded] Thread interrupted: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("[Multithreaded] Error handling client: " + e.getMessage());
            } finally {
                try { clientSocket.close(); } catch (IOException e) { /* ignore */ }
            }
        };
    }

    // ============================================================
    // MAIN — Accept loop with thread-per-request dispatch
    // ============================================================

    public static void main(String[] args) {
        Server server = new Server();

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("=========================================");
            System.out.println(" THREAD-PER-REQUEST SERVER");
            System.out.println(" Port:  " + PORT);
            System.out.println(" Delay: " + REQUEST_DELAY_MS + "ms per request");
            System.out.println("=========================================");
            System.out.println("[Multithreaded] Waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread thread = new Thread(() -> server.getConsumer().accept(clientSocket));
                thread.start();
            }
        } catch (IOException ex) {
            System.err.println("[Multithreaded] Server failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
