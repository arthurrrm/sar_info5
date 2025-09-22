# Spec

This document describes a minimal in-memory messaging abstraction consisting of a Broker that pairs peers,
and a Channel that provides a FIFO, bidirectional byte stream between them

## Broker

### Role

Coordinate connections to create channels between two endpoints.

### Methods

* `Channel accept(int port)`
  Blocking call.
  Waits for a `connect()` from another broker on the same port.
  Returns a connected `Channel` when it succeeds.
  Only one accept per port at a time on a given broker.

* `Channel connect(String name, int port)`
  Blocking call.
  Looks up a broker by `name`.

  * If no broker exists: returns `null`.
  * If broker exists but no accept yet: blocks until it appears.
    Returns a connected `Channel` when it succeeds.

Notes

* Implementations must allow only one `accept(port)` per port at a time on a given broker instance.
* A matching `connect(name, port)` unblocks the waiting `accept(port)` (or vice versa) and both sides receive a connected `Channel`.
* Implementations may choose to return `null` from `accept` during shutdown; callers should handle this gracefully.

---

## Channel

### Role

Provide a FIFO, lossless, bidirectional byte stream between two endpoints.

### Concurrency Rules

* Safe: one reader and one writer at the same endpoint.
* Unsafe: two readers at the same endpoint or two writers at the same endpoint.
* Different endpoints can read and write concurrently without problems.

### Disconnection

* the channel is created already connected.
* Becomes disconnected when either side calls `disconnect()`.
* Once disconnected by calling `disconnect()` (then `disconnected()` returns `true`), `read()`/`write()` is instantly forbidden locally.
* The remote can finish reading.
* If the remote writes after you disconnect but before it learns about it, those bytes may be ignored/dropped by your endpoint.

### Methods

* `int write(byte[] bytes, int offset, int length)`
  Writes bytes from `bytes[offset]` for `length`.
  Blocks if it cannot write.
  Returns number of bytes actually written.
  Throws a disconnected exception if the channel is closed.

* `int read(byte[] bytes, int offset, int length)`
  Reads up to `length` bytes into `bytes[offset]`.
  Blocks until at least one byte is available.
  Returns number of bytes actually read.
  Throws a disconnected exception at end of stream.

* `void disconnect()`
  Closes the local endpoint. After calling this, `read` and `write` are no longer legal locally.

* `boolean disconnected()`
  Returns `true` if this endpoint is already disconnected.

### Examples

Simple echo:

1. Server calls `accept(1234)` and blocks.
2. Client calls `connect("serverBroker", 1234)` and obtains a `Channel`.
3. Client writes N bytes, then reads N bytes back (echo).
4. Either side calls `disconnect()` when done.

---

## Task

### Role

Execute a piece of code (`Runnable`) in its own thread. This lets you handle blocking I/O without blocking the main application.

### Usage

* Create a `Task` with a `Broker` and a `Runnable`.
* Start it with `start()`.
* Inside the `Runnable`, use `Task.getBroker()` to access the broker associated with the current task.

### Static Method

`static Broker getBroker()` – returns the broker for the current thread.

### Edge cases and guarantees

* Reads must block until at least one byte is available, unless the channel is disconnected/end-of-stream.
* Writes must block if the channel cannot currently accept more bytes, unless the channel has been disconnected.
* After a local `disconnect()`, `read`/`write` must fail immediately at this endpoint.
* The remote may still be able to read any bytes already buffered prior to noticing the disconnect.


---
