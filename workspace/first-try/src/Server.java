public class Server implements Runnable {
    Broker broker;
    private final int port;

    public Server() {
        this(1234);
    }

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        broker = Task.getBroker();
        if (broker == null) {
            System.err.println("[Server] No broker available in current Task.");
            return;
        }

        Channel ch = null;
        try {
            System.out.println("[Server] Waiting for connection on port " + port + "...");
            ch = broker.accept(port);
            if (ch == null) {
                System.out.println("[Server] Accept returned null (broker shutdown?). Exiting.");
                return;
            }
            System.out.println("[Server] Client connected. Echoing bytes.");

            byte[] buf = new byte[1024];
            while (!ch.disconnected()) {
                int n = ch.read(buf, 0, buf.length);
                // Echo back what we received
                int off = 0;
                while (off < n) {
                    int wrote = ch.write(buf, off, n - off);
                    off += wrote;
                }
            }
        } catch (Exception e) {
            System.err.println("[Server] Unexpected error: " + e);
            return;
        }
    }
}
