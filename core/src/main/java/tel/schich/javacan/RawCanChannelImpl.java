/*
 * The MIT License
 * Copyright © 2018 Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package tel.schich.javacan;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetBoundException;

import tel.schich.javacan.platform.linux.LinuxNetworkDevice;

/**
 * Naming has been adopted from the JDK here (Interface + InterfaceImpl)
 */
public class RawCanChannelImpl extends RawCanChannel {

    private volatile NetworkDevice device;

    RawCanChannelImpl(int sock) {
        super(sock);
    }

    @Override
    public RawCanChannel bind(NetworkDevice device) throws IOException {
        if (!(device instanceof LinuxNetworkDevice)) {
            throw new IllegalArgumentException("Unsupported network device given!");
        }
        SocketCAN.bindSocket(getSocket(), ((LinuxNetworkDevice) device).getIndex(), 0, 0);
        this.device = device;
        return this;
    }

    @Override
    public NetworkDevice getDevice() {
        if (!isBound()) {
            throw new NotYetBoundException();
        }
        return this.device;
    }

    @Override
    public boolean isBound() {
        return this.device != null;
    }

    @Override
    public CanFrame read() throws IOException {
        int length = getOption(CanSocketOptions.FD_FRAMES) ? FD_MTU : MTU;
        ByteBuffer frameBuf = JavaCAN.allocateOrdered(length);
        return read(frameBuf);
    }

    @Override
    public CanFrame read(ByteBuffer buffer) throws IOException {
        readUnsafe(buffer);
        return CanFrame.create(buffer);
    }

    @Override
    public long readUnsafe(ByteBuffer buffer) throws IOException {
        long bytesRead = readSocket(buffer);
        buffer.flip();
        return bytesRead;
    }

    @Override
    public RawCanChannel write(CanFrame frame) throws IOException {
        long written = writeUnsafe(frame.getBuffer());
        if (written != frame.getSize()) {
            throw new IOException("Frame written incompletely!");
        }

        return this;
    }

    @Override
    public long writeUnsafe(ByteBuffer buffer) throws IOException {
        return writeSocket(buffer);
    }
}
