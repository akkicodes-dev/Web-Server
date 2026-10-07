import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * SINGLE-THREADED WEB SERVER
 * 
 * Architecture: One thread handles ALL requests sequentially.
 * 
 * INTERVIEW INSIGHT:
 * - accept() blocks until a client connects
 * - handleClient() blocks until the response is fully sent
 * - While handling Client 1, ALL other clients wait in the OS TCP queue
 * - A single slow request causes a Denial-of-Service for all other clients
 * - JMeter will show response times stacking linearly under concurrent load
 * 
 * This is the simplest possible server architecture and demonstrates WHY
 * multithreading is necessary for concurrent request handling.
 */
public class Server {

    // ============================================================
    // CONFIGURATION — Modify these values for testing
    // ============================================================

    /** Port the server listens on (same across all server variants for fair benchmarking) */
    private static final int PORT = 8010;

    /**
     * ARTIFICIAL DELAY (milliseconds) — For concurrency/load testing ONLY.
     * 
     * PURPOSE: Makes the difference between architectures visible in JMeter.
     * - SingleThreaded: Delay blocks the ENTIRE server (requests queue up)
     * - Multithreaded:  Delay only blocks the individual thread (parallel)
     * - ThreadPool:     Delay only blocks one worker thread (parallel, bounded)
     * 
     * Set to 0 for no delay. Recommended: 100–500ms for demonstration.
     */
    private static final int REQUEST_DELAY_MS = 100;

    // ============================================================
    // SERVER LOGIC
    // ============================================================

    public void run() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=========================================");
            System.out.println(" SINGLE-THREADED SERVER");
            System.out.println(" Port:  " + PORT);
            System.out.println(" Delay: " + REQUEST_DELAY_MS + "ms per request");
            System.out.println("=========================================");
            System.out.println("[SingleThreaded] Waiting for connections...");

            // ============================================================
            // MAIN LOOP — Sequential request processing
            //
            // Flow:
            //   accept() → handleClient() → accept() → handleClient() → ...
            //
            // PROBLEM: Only ONE client is served at a time.
            // If Client 1 takes 100ms, Client 2 waits 100ms before even starting.
            // With 100 concurrent JMeter threads and 100ms delay:
            //   Last client waits ~10,000ms (100 × 100ms)!
            // ============================================================
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // BLOCKING — no other client can be accepted until this returns
                handleClient(clientSocket);
            }
        }
    }

    /**
     * Handles a single HTTP request/response cycle.
     * Called on the MAIN THREAD — blocks all other connections while executing.
     */
    private void handleClient(Socket clientSocket) {
        // try-with-resources: Socket is auto-closed when this block exits.
        // When a Socket is closed, ALL its derived streams (InputStream,
        // OutputStream) are automatically closed too — no separate close needed.
        try (clientSocket) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            // --- Step 1: Read the HTTP request ---
            // HTTP requests consist of: Request-Line, Headers, blank line, [Body]
            // Format: "GET /path HTTP/1.1\r\nHost: ...\r\n\r\n"
            // We MUST read until the blank line — otherwise the client may hang.
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            // Consume remaining headers (we only need the request line for routing)
            String header;
            while ((header = reader.readLine()) != null && !header.isEmpty()) {
                // Read and discard headers
            }

            // --- Step 2: Parse the request path ---
            // Request line format: "GET /path HTTP/1.1"
            String[] parts = requestLine.split(" ");
            String path = (parts.length > 1) ? parts[1] : "/";

            // --- Step 3: Artificial delay (for benchmarking only) ---
            // In a single-threaded server, this sleep blocks the ENTIRE server.
            // No other client can be accepted or processed during this time.
            if (REQUEST_DELAY_MS > 0) {
                Thread.sleep(REQUEST_DELAY_MS);
            }

            // --- Step 4: Route request and build response ---
            String statusLine;
            String body;
            switch (path) {
                case "/":
                    statusLine = "HTTP/1.1 200 OK";
                    body = "Hello from SingleThreaded Web Server";
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

            // --- Step 5: Send valid HTTP response ---
            // Format required by JMeter / any HTTP client:
            //   Status-Line\r\n
            //   Header: Value\r\n
            //   \r\n           ← blank line separates headers from body
            //   Body
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
            System.err.println("[SingleThreaded] Request handling interrupted: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[SingleThreaded] Error handling client: " + e.getMessage());
        }
        // Socket is auto-closed by try-with-resources — no leak possible
    }

    // ============================================================
    // ENTRY POINT
    // ============================================================

    public static void main(String[] args) {
        Server server = new Server();
        try {
            server.run();
        } catch (IOException ex) {
            System.err.println("[SingleThreaded] Server failed to start: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
