package info5.sar.mqs;

import info5.sar.channels.Channel;

public abstract class MessageQueue {

    Channel channel;

    public MessageQueue(Channel channel) {
        this.channel = channel;
    }

    public abstract boolean send(byte[] bytes, int offset, int length);

    public abstract byte[] receive();

    public abstract void close();

    public abstract boolean closed();
}
