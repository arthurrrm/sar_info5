package info5.sar.test;

import info5.sar.channels.CBroker;
import info5.sar.channels.Task;


public class TestSimpleEcho {
    public static void main(String[] args) throws InterruptedException {
        // create a broker
        CBroker broker = new CBroker("broker1");
        // create a client
        CBroker broker2 = new CBroker("broker2");
        // create a server
        Task t3 = new Task("t3", broker2);
        Server s1 = new Server(broker2, 1234, "Server 1");
        Thread th3 = new Thread(() -> t3.start(s1));
        Task t4 = new Task("t4", broker2);
        Server s2 = new Server(broker2, 1235, "Server 2");
        Thread th4 = new Thread(() -> t4.start(s2));

        th3.start();
        th4.start();
        for (int i = 0; i < 5; i++) {
            Task t = new Task("t" + i, broker);
            Client c = new Client(broker, "broker2", 1234, "client A" + i);
            Thread th = new Thread(() -> t.start(c));
            th.start();th.join();
        }
        for (int i = 0; i < 5; i++) {
            Task t = new Task("t" + i, broker2);
            Client c = new Client(broker2, "broker2", 1235, "client B" + i);
            Thread th = new Thread(() -> t.start(c));
            th.start();th.join();
        }

    }
}
