/**
 * Coordinates endpoints to create {@link Channel} connections between two peers.
 * <p>
 * A Broker implementation is responsible for resolving peer names
 * and pairing a waiting {@code accept(port)} with a matching {@code connect(name, port)}.
 */
abstract class Broker {
    /** Human-readable name used for lookups (implementation-defined). */
    String name;

    /**
     * Construct a broker with the given name.
     *
     * @param name identifier for this broker (used by {@link #connect(String, int)})
     */
    Broker(String name){
        this.name = name;
    }

    /**
     * Block until a peer connects on the given port, returning a connected {@link Channel}.
     * Only one concurrent accept per port is allowed on a given broker.
     *
     * @param port logical port number to accept on
     * @return a connected {@link Channel} when the connection is established
     */
    abstract Channel accept(int port);

    /**
     * Attempt to connect to a remote broker by name on the specified port.
     * Blocks until the corresponding {@link #accept(int)} is present on the remote side.
     *
     * @param name the remote broker's name to connect to
     * @param port the logical port number
     * @return a connected {@link Channel}, or {@code null} if no broker exists with this name
     */
    abstract Channel connect(String name, int port);
}
