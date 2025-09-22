/**
 * A simple FIFO, bidirectional byte stream between two endpoints.
 * One side can read while the other writes. Call {@link #disconnect()} when done.
 */
abstract class Channel {
    /** Read up to {@code length} bytes into {@code bytes} at {@code offset`}. Blocks until some data is available. */
    abstract int read(byte[] bytes, int offset, int length);

    /** Write {@code length} bytes from {@code bytes[offset]}. May block. Returns number written. */
    abstract int write(byte[] bytes, int offset, int length);

    /** Close the local endpoint; further local reads/writes are not allowed. */
    abstract void disconnect();

    /** @return {@code true} if this endpoint is already disconnected. */
    abstract boolean disconnected();
}
