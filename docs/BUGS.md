# Code Smells & Bugs Table

| Bug ID | Location | Issue Description | Impact | Fix Strategy | Status |
|---|---|---|---|---|---|
| BUG-001 | `SingleThreaded/Server.java:17` | Single thread processes requests sequentially; blocked client hangs all future clients | Denial of Service under concurrent load | Introduce Multi-threading / ThreadPool | ✅ Fixed — ThreadPool & Multithreaded variants created |
| BUG-002 | `SingleThreaded/Server.java:19-20` | Socket streams (`PrintWriter`, `BufferedReader`) and `Socket` are never closed explicitly | Resource leak (file descriptor leak) | Try-with-resources / explicit `.close()` | ✅ Fixed — `try (clientSocket)` auto-closes socket + all streams |
| BUG-003 | `SingleThreaded/Client.java:13` | Client connects to port **8090** but server listens on **8010** | Connection refused error | Change client port to 8010 | ✅ Fixed — Port corrected to 8010 |
| BUG-004 | `Multithreaded/Server.java:27` | `clientSocket` is never closed after `getConsumer()` finishes | Resource leak (file descriptor leak) | Close socket in consumer via try-with-resources | ✅ Fixed — `try (clientSocket)` in getConsumer() |
| BUG-005 | `ThreadPool/Server.java:34` | `clientSocket` is never closed after `handleClient()` finishes | Resource leak (file descriptor leak) | Close socket in handleClient via try-with-resources | ✅ Fixed — `try (clientSocket)` in handleClient() |
| BUG-006 | `All Servers` | No HTTP request reading — server sends response without reading request | JMeter/browsers may hang or error | Read HTTP request headers before responding | ✅ Fixed — Servers now read full HTTP request |
| BUG-007 | `All Servers` | Response is raw text (`println`), not valid HTTP | JMeter reports "Non HTTP response" errors | Send proper HTTP/1.1 response with headers | ✅ Fixed — Valid HTTP response with status, headers, body |
| BUG-008 | `SingleThreaded/Server.java:14` | `setSoTimeout(20000)` causes `SocketTimeoutException` after 20s idle | Server crashes if no client connects within timeout | Remove SoTimeout for production/testing stability | ✅ Fixed — SoTimeout removed |
| BUG-009 | `Multithreaded/Server.java:24` | `setSoTimeout(70000)` causes crash after 70s idle | Server crashes on timeout | Remove SoTimeout | ✅ Fixed — SoTimeout removed |
| BUG-010 | `ThreadPool/Server.java:30` | `setSoTimeout(70000)` causes crash after 70s idle | Server crashes on timeout | Remove SoTimeout | ✅ Fixed — SoTimeout removed |
| BUG-011 | `ThreadPool/Server.java:43` | `threadPool.shutdown()` in finally block is unreachable (infinite while loop) | Thread pool never shuts down cleanly | Use Runtime.addShutdownHook() for Ctrl+C handling | ✅ Fixed — Shutdown hook added |
