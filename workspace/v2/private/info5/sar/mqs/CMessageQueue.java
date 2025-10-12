package info5.sar.mqs;

import info5.sar.channels.CChannel;

public class CMessageQueue extends MessageQueue {

    public CMessageQueue(CChannel channel) {
        super(channel);
    }
    @Override
    public boolean send(byte[] bytes, int offset, int length) {
        if (channel.disconnected()) 
            throw new IllegalStateException("channel disconnected on send");
        byte[] l = new byte[4];
        l[0] = (byte) (length >> 24);
        l[1] = (byte) (length >> 16);
        l[2] = (byte) (length >> 8);
        l[3] = (byte) (length);

        int r;
        try {
            r = channel.write(l, 0, 4);
            if (r < 4) return false;
            r = channel.write(bytes, offset, length);
            if (r < length) return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public byte[] receive() {
        if (channel.disconnected()) 
            throw new IllegalStateException("channel disconnected on receive");
        byte[] l = new byte[4];
        int r;
        try {
            r = channel.read(l, 0, 4);
            if (r < 4) return null;
            int length = ((l[0] & 0xFF) << 24) | ((l[1] & 0xFF) << 16) | ((l[2] & 0xFF) << 8) | (l[3] & 0xFF);
            byte[] bytes = new byte[length];
            r = channel.read(bytes, 0, length);
            if (r < length) return null;
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() {
        channel.disconnect();
    }

    @Override
    public boolean closed() {
        return channel.disconnected();
    }
    
}
