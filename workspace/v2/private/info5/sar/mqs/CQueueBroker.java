package info5.sar.mqs;

import info5.sar.channels.CBroker;
import info5.sar.channels.CChannel;

public class CQueueBroker extends QueueBroker {

    public CQueueBroker(CBroker broker) {
        super(broker);
    }

    @Override
    public MessageQueue accept(int port) {
        CChannel channel = (CChannel) broker.accept(port);
        return new CMessageQueue(channel);
    }

    @Override
    public MessageQueue connect(String name, int port) {
        CChannel channel = (CChannel) broker.connect(name, port);
        return new CMessageQueue(channel);
    }
    
}
