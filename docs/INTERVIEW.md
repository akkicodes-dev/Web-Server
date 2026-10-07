# FAANG-Level System Design & Coding Interview Questions

## Question 1: What happens at the OS level when a thread calls `ServerSocket.accept()`?
- **Concept**: TCP 3-Way Handshake, SYN queue, Accept queue, OS System Call (`accept()`).
- **Answer**: When `accept()` is called, the thread makes a POSIX `accept()` system call. The OS checks the **accept queue** (completed TCP connections that finished the 3-way handshake). If the queue is empty, the thread is **parked** in the kernel — it uses **zero CPU** while waiting. When a client connects (SYN → SYN-ACK → ACK), the OS moves the connection from the SYN queue to the accept queue, and **wakes up** the blocked thread. `accept()` returns a new `Socket` representing that specific connection.
- **Trade-off**: Blocking is simple but wastes the thread. Non-blocking I/O (Java NIO `Selector`) avoids this but adds complexity.

## Question 2: Why can't we spawn infinite Threads for each incoming HTTP request in Production?
- **Concept**: Thread stack memory overhead (1MB default per thread), Context Switching overhead, OS PID limits.
- **Answer**: Each OS thread consumes **~1MB of stack memory** (configurable with `-Xss`). With 1000 concurrent clients, that's ~1GB just for thread stacks — on top of heap, metaspace, and OS overhead. The OS scheduler must **context-switch** between all threads, and with more threads than CPU cores, this becomes pure waste. Linux typically limits threads to 10K-30K per process. Beyond that, `pthread_create` fails → Java throws `OutOfMemoryError: unable to create native thread`. This is why **bounded thread pools** (e.g., `Executors.newFixedThreadPool(N)`) are the production standard.
- **Trade-off**: Thread-per-request gives maximum parallelism but unbounded resource consumption. ThreadPool trades some latency (queuing) for **system stability**.

## Question 3: How does a Thread Pool provide backpressure?
- **Concept**: Bounded concurrency, work queue, ExecutorService, LinkedBlockingQueue.
- **Answer**: `Executors.newFixedThreadPool(N)` creates exactly N threads backed by an unbounded `LinkedBlockingQueue`. When all N threads are busy, new tasks are enqueued — not dropped, not given new threads. The queue acts as a **buffer**: clients experience higher latency (waiting in queue) but the server stays stable. This is **backpressure** — the system naturally slows down under load instead of crashing. For even more control, use `ThreadPoolExecutor` directly with a **bounded queue** + `RejectedExecutionHandler`.
- **Trade-off**: Unbounded queue can still cause OOM if tasks accumulate faster than they're processed. Production systems often use bounded queues with rejection policies (CallerRunsPolicy, AbortPolicy).

## Question 4: What is the difference between `shutdown()` and `shutdownNow()`?
- **Concept**: Graceful shutdown, thread interruption, in-flight requests.
- **Answer**: `shutdown()` stops accepting **new** tasks but lets **in-flight** tasks complete. `shutdownNow()` **interrupts** all running threads and returns a list of unexecuted tasks. Best practice: `shutdown()` → `awaitTermination(timeout)` → `shutdownNow()` as last resort. Our ThreadPool server uses a JVM **shutdown hook** (`Runtime.addShutdownHook()`) to trigger this sequence when Ctrl+C is pressed.
- **Trade-off**: Aggressive shutdown (shutdownNow) risks data loss; patient shutdown (long timeout) risks hanging on Ctrl+C.

## Question 5: What is a resource leak and why is it dangerous in a server?
- **Concept**: File descriptors, Socket leaks, try-with-resources, OS limits.
- **Answer**: Every open Socket/Stream consumes an OS **file descriptor**. Linux defaults to ~1024 FDs per process. If sockets aren't closed after use, FDs are exhausted → new connections fail with `Too many open files`. In our original code, SingleThreaded Server **never closed** the accepted socket or its streams — a classic leak. Fix: `try-with-resources` guarantees cleanup even when exceptions occur. Closing a Socket automatically closes all its derived streams.
- **Trade-off**: None — always close resources. The only question is HOW (manual close vs. try-with-resources).

## Question 6: Why use `Connection: close` in the HTTP response?
- **Concept**: HTTP persistent connections, keep-alive, resource management.
- **Answer**: `Connection: close` tells the client "I'm done; close this TCP connection." Without it, HTTP/1.1 defaults to **keep-alive** — the client holds the socket open for reuse. For our educational server, keep-alive would require tracking connection state, handling timeouts, and multiplexing — unnecessary complexity. Production servers like Tomcat support keep-alive because opening new TCP connections is expensive (3-way handshake).
- **Trade-off**: `close` is simple but has overhead (new TCP connection per request). `keep-alive` is efficient but complex.

## Question 7: What is the difference between throughput and latency?
- **Concept**: Performance metrics, load testing, SLOs.
- **Answer**: **Throughput** = requests/second (server capacity). **Latency** = time per request (user experience). A server with high throughput but high latency means "we process lots of requests, but each one is slow." Ideal: **high throughput + low latency**. In our project: SingleThreaded has low throughput + high latency under load. ThreadPool has moderate throughput + bounded latency. Thread-per-request has highest throughput but risks instability.
- **Trade-off**: Optimizing for throughput (more threads) can increase tail latency (P99) due to context switching.

## Question 8: How would you design this server for 1 million concurrent connections?
- **Concept**: Java NIO, Selector, event-driven architecture, Netty, epoll.
- **Answer**: Thread pools work up to ~10K connections. Beyond that, you need **non-blocking I/O** (Java NIO). Instead of one thread per connection, a single thread uses a `Selector` to monitor thousands of sockets. When a socket has data ready, the Selector wakes up and dispatches it. This is the **reactor pattern** used by Netty, Node.js, Nginx. Linux's `epoll` system call makes this efficient at OS level. Our server could evolve: SingleThreaded → ThreadPool → NIO Selector → Netty framework.
- **Trade-off**: NIO is powerful but complex (callback-based, hard to debug). Thread-per-request is simpler but doesn't scale.

---

## 📝 Topic Quick Reference

| # | Topic | One-Liner |
|---|-------|-----------|
| 1 | ServerSocket vs Socket | Listener vs individual connection |
| 2 | accept() blocking | OS parks thread in kernel wait queue |
| 3 | TCP connection queue | SYN queue + accept queue buffer connections |
| 4 | OS-level blocking | Thread uses zero CPU while blocked |
| 5 | Thread creation cost | ~1ms + ~1MB stack per thread |
| 6 | Context switching | ~1-10μs per switch, waste beyond core count |
| 7 | Thread stack memory | 1MB default, configurable with -Xss |
| 8 | Thread-per-request | Simple concurrency, unbounded risk |
| 9 | ExecutorService | Java's thread pool abstraction |
| 10 | Fixed thread pool | Bounded threads + work queue |
| 11 | Work queue | LinkedBlockingQueue buffers excess tasks |
| 12 | Backpressure | System slows down instead of crashing |
| 13 | Bounded concurrency | Fixed upper limit on parallel work |
| 14 | Graceful shutdown | shutdown() → awaitTermination() → shutdownNow() |
| 15 | Socket/resource leaks | Unclosed FDs → "Too many open files" |
| 16 | try-with-resources | Auto-close guarantee on any exit path |
| 17 | HTTP basics | Request-Line + Headers + blank line + Body |
| 18 | Load testing | JMeter simulates concurrent users |
| 19 | Throughput vs latency | Capacity vs speed — optimize both |
| 20 | JMeter methodology | CLI mode, same params, fair comparison |
