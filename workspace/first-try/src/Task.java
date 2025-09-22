abstract class Task extends Thread {
    static Broker broker;
    Runnable runnable;

    Task(Broker b, Runnable r) {
        broker = b;
        runnable = r;
    }

    public static Broker getBroker() {
        return broker;
    }
}
