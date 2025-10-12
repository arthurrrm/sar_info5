package info5.sar.test.mqSynchronous;

import info5.sar.mqs.MessageQueue;
import info5.sar.mqs.QueueBroker;

public class MQServer implements Runnable {
    private final QueueBroker qbroker;
    private final int port;
    private final String name;

    public MQServer(QueueBroker qbroker, int port, String name) {
        this.qbroker = qbroker;
        this.port = port;
        this.name = name;
    }

    @Override
    public void run() {
        if (qbroker == null) {
            System.err.println("[" + name + "] No queue broker provided.");
            return;
        }

        while (true) {
            try {
                System.out.println("[" + name + "] Waiting for MQ connection on port " + port + "...");
                MessageQueue mq = qbroker.accept(port);
                if (mq == null) {
                    System.out.println("[" + name + "] accept returned null. Exiting.");
                    return;
                }
                System.out.println("[" + name + "] Client connected to MQ. Echoing messages.");

                while (!mq.closed()) {
                    byte[] msg = mq.receive();
                    if (msg == null) {
                        // Connection likely closed or error
                        break;
                    }
                    // Echo back
                    boolean ok = mq.send(msg, 0, msg.length);
                    if (!ok) break;
                }
                mq.close();
            } catch (Throwable t) {
                System.err.println("[" + name + "] Unexpected error: " + t);
                return;
            }
        }
    }
}
