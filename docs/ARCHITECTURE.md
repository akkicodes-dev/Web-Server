# System Architecture & Flow Diagrams

## Single-Threaded Server Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client1 as Client 1
    actor Client2 as Client 2
    participant Server as SingleThreaded Server Loop
    participant Socket as ServerSocket (Port 8010)

    Server->>Socket: accept() [BLOCKING]
    Client1->>Socket: Connect request
    Socket-->>Server: acceptedConnection Socket
    Server->>Client1: Send "Hello World..."
    Note over Server,Client2: Client 2 is BLOCKED in OS TCP queue until Client 1 finishes!
    Server->>Socket: accept() [BLOCKING next loop]
```

## Architecture Evolution Plan
1. **Single-Threaded**: Sequential handling, 1 connection at a time.
2. **Thread-per-Request**: Spawn dynamic OS thread for every incoming socket.
3. **ThreadPool**: Fixed worker threads backed by `ArrayBlockingQueue` / `LinkedBlockingQueue`.
