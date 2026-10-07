import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * THREAD POOL WEB SERVER (Production-Oriented)
 * 
 * Architecture: Fixed pool of reusable worker threads handles all connections.
 * 
 * INTERVIEW INSIGHT:
 * - BOUNDED CONCURRENCY: Only MAX_THREADS threads exist at any time
 * - WORK QUEUE: Extra connections wait in the executor's internal queue
 * - BACKPRESSURE: Server won't crash under load — excess work is queued
 * - THREAD REUSE: Threads are recycled, avoiding creation/destruction overhead
 * - GRACEFUL SHUTDOWN: Shutdown hook allows in-flight requests to complete
 */
public class Server {

    // ============================================================
    // CONFIGURATION
    // ============================================================
    private static final int PORT = 8010;
    private static final int MAX_THREADS = 10;
    private static final int REQUEST_DELAY_MS = 100;

    // ============================================================
    // THREAD POOL
    // ============================================================
    private final ExecutorService threadPool;

    public Server(int poolSize) {
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    // ============================================================
    // REQUEST HANDLER
    // ============================================================

    public void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            // --- Read HTTP request ---
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String header;
            while ((header = reader.readLine()) != null && !header.isEmpty()) {
                // consume headers
            }

            // --- Parse request path ---
            String[] parts = requestLine.split(" ");
            String path = (parts.length > 1) ? parts[1] : "/";

            // --- Artificial delay ---
            if (REQUEST_DELAY_MS > 0) {
                Thread.sleep(REQUEST_DELAY_MS);
            }

            // --- Route and build response ---
            String statusLine;
            String body;
            switch (path) {
                case "/":
                    statusLine = "HTTP/1.1 200 OK";
                    body = "Hello from ThreadPool Web Server";
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
            System.err.println("[ThreadPool] Worker interrupted: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[ThreadPool] Error handling client: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException e) { /* ignore */ }
        }
    }

    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) {
        Server server = new Server(MAX_THREADS);

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[ThreadPool] Shutting down...");
            server.threadPool.shutdown();
            try {
                if (!server.threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.threadPool.shutdownNow();
            }
            System.out.println("[ThreadPool] Server shut down.");
        }));

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("=========================================");
            System.out.println(" THREAD POOL SERVER");
            System.out.println(" Port:     " + PORT);
            System.out.println(" Workers:  " + MAX_THREADS + " threads");
            System.out.println(" Delay:    " + REQUEST_DELAY_MS + "ms per request");
            System.out.println("=========================================");
            System.out.println("[ThreadPool] Waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                server.threadPool.execute(() -> server.handleClient(clientSocket));
            }
        } catch (IOException ex) {
            System.err.println("[ThreadPool] Server failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
