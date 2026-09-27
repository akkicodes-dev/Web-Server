# Multithreaded Web Server — Progress Tracker

## Phase 0: Setup & Baseline Analysis
- [x] Repository Clone & Initial Branch Setup (`phase-0-setup`)
- [x] Documentation Artifacts Initialization (`docs/`)
- [ ] SingleThreaded Server Analysis & Crash Execution

## Phase 1: Single-Threaded Limitations & Socket Basics
- [ ] Understand `ServerSocket.accept()` blocking model
- [ ] Simulate blocking/hanging requests with multiple concurrent clients
- [ ] Identify resource leaks & unclosed IO streams

## Phase 2: Multi-Threaded Server (Thread-per-Request)
- [ ] Implement `Thread` spawn per socket connection
- [ ] Analyze memory overhead & context switching limits under load
- [ ] Crash server with Thread Exhaustion / OutOfMemoryError

## Phase 3: Thread Pool Server (Fixed Worker Threads)
- [ ] Implement `ExecutorService` / `Executors.newFixedThreadPool`
- [ ] Bound task queue & explore backpressure / rejection execution policies
- [ ] Benchmarking throughput under concurrency

## Phase 4: Production Hardening & Non-Blocking I/O (NIO)
- [ ] HTTP protocol parsing (Request line, Headers, Body)
- [ ] Graceful shutdown hooks & thread pool termination safety
- [ ] Introduction to Selector & Non-blocking Sockets (Java NIO)
