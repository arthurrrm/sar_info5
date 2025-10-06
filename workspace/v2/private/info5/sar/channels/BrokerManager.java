package info5.sar.channels;

import java.util.concurrent.ConcurrentHashMap;

public class BrokerManager {
    private static ConcurrentHashMap<String, Broker> brokers = new ConcurrentHashMap<>();

    private BrokerManager() {
    }

    public static Broker getBroker(String name) {
        return brokers.get(name);
    }

    public static Broker registerBroker(Broker broker) {
        return brokers.put(broker.getName(), broker);
    }

    public static void unregisterBroker(Broker broker) {
        brokers.remove(broker.getName());
    }
}