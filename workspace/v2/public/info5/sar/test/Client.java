package info5.sar.test;

import info5.sar.channels.CBroker;
import info5.sar.channels.Channel;

public class Client implements Runnable {
    CBroker broker;
    private final String serverName;
    private final int port;
    private final String clientName;


    public Client(CBroker b, String serverName, int port, String clientName) {
        this.broker = b;
        this.serverName = serverName;
        this.port = port;
        this.clientName = clientName;
    }

    @Override
    public void run() {
        byte[] message = ("Hello from " + clientName).getBytes();

        if (broker == null) {
            System.err.println("[" + clientName +"] No broker available in current Task.");
            return;
        }

        Channel ch = null;
        try {
            System.out.println("[" + clientName +"] Connecting to '" + serverName + "' on port " + port );
            ch = broker.connect(serverName, port);
            if (ch == null) {
                System.err.println("[" + clientName +"] No such broker: " + serverName);
                return;
            }
            System.out.println("[" + clientName +"] Connected. Writing " + message.length + " bytes.");

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

            System.out.println("[" + clientName +"] Echo received: " + new String(buf));
        } catch (Exception e) {
            System.err.println("[" + clientName +"] Unexpected error: " + e);
            return;
        }
        ch.disconnect();
        System.out.println("[" + clientName +"] Disconnected.");
    }
}
