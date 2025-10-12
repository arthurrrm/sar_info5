package info5.sar.test.broker;

import info5.sar.channels.BrokerManager;
import info5.sar.channels.CBroker;
import info5.sar.channels.Channel;
import info5.sar.channels.Task;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal console test  for the channel/broker.
 * Each test has a name and a description; results are printed with a final summary.
 */
public class TestBroker {

    interface TestFn {
        void run() throws Exception;
    }

    static class TestCase {
        final String name;
        final String description;
        final TestFn fn;
        TestCase(String name, String description, TestFn fn) {
            this.name = name;
            this.description = description;
            this.fn = fn;
        }
    }

    private static final AtomicInteger UNIQUE = new AtomicInteger();

    private static String uid(String base) {
        return base + "-" + UNIQUE.incrementAndGet();
    }

    // --- tiny assertions ---
    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
    private static void assertEquals(Object exp, Object got, String msg) {
        if (exp == null && got == null) return;
        if (exp != null && exp.equals(got)) return;
        throw new AssertionError(msg + " (expected=" + exp + ", got=" + got + ")");
    }

    public static void main(String[] args) {
        List<TestCase> tests = new ArrayList<>();

        tests.add(new TestCase(
                "echo_basic",
                "Client connects to server, sends 'Hello', server echoes back same bytes.",
                TestBroker::testEchoBasic));

        tests.add(new TestCase(
                "disconnected_after_drain",
                "After remote disconnects, local disconnected() becomes true only after draining remaining bytes; subsequent read throws.",
                TestBroker::testDisconnectedAfterDrain));

        tests.add(new TestCase(
                "unknown_broker_connect",
                "Connecting to an unknown broker name returns null immediately.",
                TestBroker::testUnknownBrokerConnect));

        tests.add(new TestCase(
                "duplicate_broker_name",
                "Registering two brokers with the same name throws IllegalArgumentException.",
                TestBroker::testDuplicateBrokerName));

        tests.add(new TestCase(
                "invalid_read_write_args",
                "Channel.read/write with invalid offset/length throws IllegalArgumentException.",
                TestBroker::testInvalidReadWriteArgs));

        tests.add(new TestCase(
                "sequential_clients_same_port",
                "A server can accept two clients sequentially on the same port, echoing their messages.",
                TestBroker::testSequentialClients));

        int passed = 0, failed = 0;
        long t0 = System.currentTimeMillis();
        for (TestCase tc : tests) {
            String header = String.format("[RUN ] %s - %s", tc.name, tc.description);
            System.out.println(header);
            try {
                tc.fn.run();
                passed++;
                System.out.println(String.format("[PASS] %s", tc.name));
            } catch (Throwable th) {
                failed++;
                System.out.println(String.format("[FAIL] %s - %s", tc.name, th.toString()));
                th.printStackTrace(System.out);
            }
            System.out.println();
        }
        long dt = System.currentTimeMillis() - t0;
        System.out.println("========================");
        System.out.println(String.format("Tests finished: %d passed, %d failed (%.3fs)",
                passed, failed, dt / 1000.0));
        System.out.println("=======================");

        if (failed > 0) System.exit(1);
    }

    // ----------------- tests implementation -----------------

    private static void testEchoBasic() throws Exception {
        String bAName = uid("A");
        String bBName = uid("B");
        CBroker a = new CBroker(bAName);
        CBroker b = new CBroker(bBName);
        int port = 10000 + UNIQUE.get() % 1000;

        Task server = new Task("server-echo-basic", b);
        server.start(() -> {
            Channel ch = b.accept(port);
            byte[] buf = new byte[64];
            int n = ch.read(buf, 0, buf.length);
            // echo back the same number of bytes
            int off = 0;
            while (off < n) off += ch.write(buf, off, n - off);
            ch.disconnect();
        });

        byte[] msg = "Hello".getBytes(StandardCharsets.UTF_8);
        Channel ch = a.connect(bBName, port);
        assertTrue(ch != null, "connect returned null");
        int wrote = ch.write(msg, 0, msg.length);
        assertEquals(msg.length, wrote, "wrote length mismatch");
        byte[] back = new byte[msg.length];
        int read = 0; while (read < back.length) read += ch.read(back, read, back.length - read);
        assertEquals("Hello", new String(back, StandardCharsets.UTF_8), "echo mismatch");
        ch.disconnect();
        server.join(2000);
        BrokerManager.unregisterBroker(a);
        BrokerManager.unregisterBroker(b);
    }

    private static void testDisconnectedAfterDrain() throws Exception {
        String bAName = uid("A");
        String bBName = uid("B");
        CBroker a = new CBroker(bAName);
        CBroker b = new CBroker(bBName);
        int port = 11000 + UNIQUE.get() % 1000;

        Task server = new Task("server-disconnect", b);
        server.start(() -> {
            Channel ch = b.accept(port);
            byte[] data = "XYZ".getBytes(StandardCharsets.UTF_8);
            ch.write(data, 0, data.length);
            // remote half-disconnect
            ch.disconnect();
        });

        Channel ch = a.connect(bBName, port);
        assertTrue(ch != null, "connect returned null");
        // Before reading, should not be considered disconnected
        assertTrue(!ch.disconnected(), "should not be disconnected before draining");
        byte[] buf = new byte[3];
        int r = 0; while (r < 3) r += ch.read(buf, r, 3 - r);
        assertEquals("XYZ", new String(buf, StandardCharsets.UTF_8), "payload mismatch");
        // After reading the rest, now disconnected() should be true
        assertTrue(ch.disconnected(), "should be disconnected after draining");
        boolean threw = false;
        try {
            ch.read(new byte[1], 0, 1);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue(threw, "read on disconnected channel should throw");
        server.join(2000);
        BrokerManager.unregisterBroker(a);
        BrokerManager.unregisterBroker(b);
    }

    private static void testUnknownBrokerConnect() {
        String bAName = uid("A");
        CBroker a = new CBroker(bAName);
        Channel ch = a.connect("does-not-exist-" + uid("X"), 42);
        assertTrue(ch == null, "connect to unknown broker should return null");
        BrokerManager.unregisterBroker(a);
    }

    private static void testDuplicateBrokerName() {
        String name = uid("DUP");
        CBroker a = new CBroker(name);
        boolean threw = false;
        try {
            new CBroker(name); // should throw
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue(threw, "Creating a broker with duplicate name should throw IllegalArgumentException");
        BrokerManager.unregisterBroker(a);
    }

    private static void testInvalidReadWriteArgs() {
        String bAName = uid("A");
        String bBName = uid("B");
        CBroker a = new CBroker(bAName);
        CBroker b = new CBroker(bBName);
        int port = 12000 + UNIQUE.get() % 1000;

        Task server = new Task("server-invalid-args", b);
        server.start(() -> {
            Channel ch = b.accept(port);
            // keep channel open a bit so client can try invalid ops
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            ch.disconnect();
        });

        Channel ch = a.connect(bBName, port);
        assertTrue(ch != null, "connect returned null");
        byte[] arr = new byte[4];
        boolean threw;
        threw = false; try { ch.write(arr, -1, 1); } catch (IllegalArgumentException e) { threw = true; }
        assertTrue(threw, "write with negative offset should throw");
        threw = false; try { ch.write(arr, 0, 10); } catch (IllegalArgumentException e) { threw = true; }
        assertTrue(threw, "write beyond array end should throw");
        threw = false; try { ch.read(arr, 5, 0); } catch (IllegalArgumentException e) { threw = true; }
        assertTrue(threw, "read with offset past end should throw");

        ch.disconnect();
        try { server.join(1000); } catch (InterruptedException ignored) {}
        BrokerManager.unregisterBroker(a);
        BrokerManager.unregisterBroker(b);
    }

    private static void testSequentialClients() throws Exception {
        String bAName = uid("A");
        String bBName = uid("B");
        CBroker a = new CBroker(bAName);
        CBroker b = new CBroker(bBName);
        int port = 13000 + UNIQUE.get() % 1000;

        CountDownLatch served = new CountDownLatch(2);
        Task server = new Task("server-seq", b);
        server.start(() -> {
            for (int i = 0; i < 2; i++) {
                Channel ch = b.accept(port);
                byte[] buf = new byte[64];
                int n = ch.read(buf, 0, buf.length);
                int off = 0; while (off < n) off += ch.write(buf, off, n - off);
                ch.disconnect();
                served.countDown();
            }
        });

        Runnable clientTask = ( ) -> {
            String txt = "hello-" + Thread.currentThread().getName();
            Channel ch = a.connect(bBName, port);
            if (ch == null) throw new RuntimeException("connect returned null");
            byte[] msg = txt.getBytes(StandardCharsets.UTF_8);
            ch.write(msg, 0, msg.length);
            byte[] back = new byte[msg.length];
            int r = 0; while (r < back.length) r += ch.read(back, r, back.length - r);
            if (!txt.equals(new String(back, StandardCharsets.UTF_8)))
                throw new RuntimeException("echo mismatch");
            ch.disconnect();
        };

    Task c1 = new Task("c1", a);
    Task c2 = new Task("c2", a);
    c1.start(clientTask); c1.join();
    c2.start(clientTask); c2.join();
        assertTrue(served.await(2, TimeUnit.SECONDS), "server did not serve both clients in time");

        server.join(1000);
        BrokerManager.unregisterBroker(a);
        BrokerManager.unregisterBroker(b);
    }
}
