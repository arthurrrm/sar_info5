package info5.sar.test.mqSynchronous;

import info5.sar.channels.CBroker;
import info5.sar.mqs.QBTask;
import info5.sar.mqs.QueueBroker;

// Use the concrete private implementation for QueueBroker
import info5.sar.mqs.CQueueBroker;

public class TestMQSimpleEcho {
    public static void main(String[] args) throws InterruptedException {
        // Create underlying channel brokers
        CBroker broker1 = new CBroker("broker1");
        CBroker broker2 = new CBroker("broker2");

        // Wrap them into QueueBrokers
        QueueBroker qb1 = new CQueueBroker(broker1);
        QueueBroker qb2 = new CQueueBroker(broker2);

        // Start two MQ echo servers on different ports on broker2
        QBTask sTask1 = new QBTask(qb2, "mq-server-1");
        QBTask sTask2 = new QBTask(qb2, "mq-server-2");
        sTask1.start(new MQServer(qb2, 2234, "MQ Server 1"));
        sTask2.start(new MQServer(qb2, 2235, "MQ Server 2"));

        // Start a few clients hitting server 1 from broker1
        for (int i = 0; i < 3; i++) {
            QBTask ct = new QBTask(qb1, "mq-client-A" + i);
            ct.start(new MQClient(qb1, "broker2", 2234, "mq-client-A" + i));
            ct.join();
        }
        // And a few clients hitting server 2 from broker2 itself
        for (int i = 0; i < 3; i++) {
            QBTask ct = new QBTask(qb2, "mq-client-B" + i);
            ct.start(new MQClient(qb2, "broker2", 2235, "mq-client-B" + i));
            ct.join();
        }
    }
}
