package dev.darealturtywurty.superturtybot.core;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class MultiOutputStream extends OutputStream {
    private final List<OutputStream> streams;

    public MultiOutputStream(OutputStream... outputStreams) {
        this.streams = List.of(outputStreams);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        for (OutputStream os : streams)
            os.write(b);
    }

    @Override
    public synchronized void write(byte @NotNull [] bytes, int off, int len) throws IOException {
        for (OutputStream os : streams)
            os.write(bytes, off, len);
    }

    @Override
    public synchronized void flush() throws IOException {
        for (OutputStream os : streams)
            os.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        IOException first = null;
        for (OutputStream os : streams) {
            try {
                os.close();
            } catch (IOException exception) {
                if (first == null)
                    first = exception;
            }
        }

        if (first != null)
            throw first;
    }
}
