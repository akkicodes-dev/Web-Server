# Code Smells & Bugs Table

| Bug ID | Location | Issue Description | Impact | Fix Strategy | Status |
|---|---|---|---|---|---|
| BUG-001 | `SingleThreaded/Server.java:17` | Single thread processes requests sequentially; blocked client hangs all future clients | Denial of Service under concurrent load | Introduce Multi-threading / ThreadPool | Open |
| BUG-002 | `SingleThreaded/Server.java:19-20` | Socket streams (`PrintWriter`, `BufferedReader`) and `Socket` are never closed explicitly | Resource leak (file descriptor leak) | Try-with-resources / explicit `.close()` | Open |
