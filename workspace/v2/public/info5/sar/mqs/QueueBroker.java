package info5.sar.mqs;

import info5.sar.channels.Broker;

public abstract class QueueBroker {
    Broker broker;

    QueueBroker(Broker broker) {
        this.broker = broker;
    }

    String name() {
        return broker.getName();
    }

    Broker getBroker() {
        return broker;
    }
    
    public abstract MessageQueue accept(int port);

    public abstract MessageQueue connect(String name, int port);
    
}