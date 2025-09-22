public class Client implements Runnable {
    Broker broker;
    private final String serverName;
    private final int port;
    private final byte[] message;

    public Client() {
        this("server", 1234, "hello".getBytes());
    }

    public Client(String serverName, int port, byte[] message) {
        this.serverName = serverName;
        this.port = port;
        this.message = message != null ? message : new byte[0];
    }

    @Override
    public void run(){
        broker = Task.getBroker();
        if (broker == null) {
            System.err.println("[Client] No broker available in current Task.");
            return;
        }

        Channel ch = null;
        try {
            System.out.println("[Client] Connecting to '" + serverName + "' on port " + port );
            ch = broker.connect(serverName, port);
            if (ch == null) {
                System.err.println("[Client] No such broker: " + serverName);
                return;
            }
            System.out.println("[Client] Connected. Writing " + message.length + " bytes.");

            // Write the message
            int off = 0;
            while (off < message.length) {
                int wrote = ch.write(message, off, message.length - off);
                if (wrote <= 0) break;
                off += wrote;
            }

            // Read echo of the same size
            byte[] buf = new byte[message.length];
            int read = 0;
            while (read < message.length && !ch.disconnected()) {
                int n = ch.read(buf, read, buf.length - read);
                if (n <= 0) continue;
                read += n;
            }

            System.out.println("[Client] Echo received: " + new String(buf));
        } catch (Exception e) {
            System.err.println("[Client] Unexpected error: " + e);
            return;
        }
    }
}
