public class basicTask extends Task {
    public basicTask(Broker b, Runnable r) {
        super(b, r);
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
