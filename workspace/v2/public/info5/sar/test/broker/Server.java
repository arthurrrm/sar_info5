package info5.sar.test.broker;

import info5.sar.channels.CBroker;
import info5.sar.channels.Channel;

public class Server implements Runnable {
    CBroker broker;
    private final int port;
    private final String name;


    public Server(CBroker b, int port, String name) {
        this.broker = b;
        this.port = port;
        this.name = name;
    }

    @Override
    public void run() {
        if (broker == null) {
            System.err.println("[" + name + "] No broker available in current Task.");
            return;
        }

        Channel ch = null;
        while (true) {
            try {
                System.out.println("[" + name + "] Waiting for connection on port " + port + "...");
                ch = broker.accept(port);
                if (ch == null) {
                    System.out.println("[" + name + "] Accept returned null (broker shutdown?). Exiting.");
                    return;
                }
                System.out.println("[" + name + "] Client connected. Echoing bytes.");

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
                System.err.println("[" + name + "] Unexpected error: " + e);
                return;
            }
        }
    }
}
