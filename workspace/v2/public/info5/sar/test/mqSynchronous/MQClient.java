package info5.sar.test.mqSynchronous;

import info5.sar.mqs.MessageQueue;
import info5.sar.mqs.QueueBroker;

public class MQClient implements Runnable {
    private final QueueBroker qbroker;
    private final String serverName;
    private final int port;
    private final String clientName;

    public MQClient(QueueBroker qbroker, String serverName, int port, String clientName) {
        this.qbroker = qbroker;
        this.serverName = serverName;
        this.port = port;
        this.clientName = clientName;
    }

    @Override
    public void run() {
        byte[] message = ("Hello from " + clientName).getBytes();
        if (qbroker == null) {
            System.err.println("[" + clientName + "] No queue broker provided.");
            return;
        }
        MessageQueue mq = null;
        try {
            System.out.println("[" + clientName + "] Connecting to MQ '" + serverName + "' on port " + port);
            mq = qbroker.connect(serverName, port);
            if (mq == null) {
                System.err.println("[" + clientName + "] No such MQ broker: " + serverName);
                return;
            }
            System.out.println("[" + clientName + "] Connected to MQ. Sending " + message.length + " bytes.");
            boolean ok = mq.send(message, 0, message.length);
            if (!ok) {
                System.err.println("[" + clientName + "] Failed to send message.");
                return;
            }
            byte[] echo = mq.receive();
            if (echo == null) {
                System.err.println("[" + clientName + "] No echo received.");
                return;
            }
            System.out.println("[" + clientName + "] Echo received: " + new String(echo));
        } catch (Throwable t) {
            System.err.println("[" + clientName + "] Unexpected error: " + t);
            return;
        }
        mq.close();
        System.out.println("[" + clientName + "] Disconnected.");
    }
}
