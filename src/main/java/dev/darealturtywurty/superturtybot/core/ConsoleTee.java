package dev.darealturtywurty.superturtybot.core;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConsoleTee implements AutoCloseable {
    private final PrintStream prevOut;
    private final PrintStream prevErr;
    private final List<Closeable> toClose = new ArrayList<>();

    private ConsoleTee(PrintStream prevOut, PrintStream prevErr) {
        this.prevOut = prevOut;
        this.prevErr = prevErr;
    }

    /**
     * Start teeing both stdout and stderr into the same file.
     *
     * @param path   log file path
     * @param append true to append, false to overwrite
     */
    public static ConsoleTee toFile(Path path, boolean append) throws IOException {
        var fos = new FileOutputStream(path.toFile(), append);
        var bos = new BufferedOutputStream(fos);

        var teeOut = new PrintStream(
                new MultiOutputStream(new NonClosingOutputStream(bos), System.out),
                true, StandardCharsets.UTF_8);

        var teeErr = new PrintStream(
                new MultiOutputStream(new NonClosingOutputStream(bos), System.err),
                true, StandardCharsets.UTF_8);

        var ctl = new ConsoleTee(System.out, System.err);
        ctl.toClose.add(teeOut);
        ctl.toClose.add(teeErr);
        ctl.toClose.add(bos); // closes underlying fos too

        System.setOut(teeOut);
        System.setErr(teeErr);
        return ctl;
    }

    /**
     * Restore originals and close the resources we created.
     */
    @Override
    public void close() {
        System.setOut(prevOut);
        System.setErr(prevErr);

        for (int index = toClose.size() - 1; index >= 0; index--) {
            try {
                toClose.get(index).close();
            } catch (IOException ignored) {}
        }
    }

    private static final class NonClosingOutputStream extends OutputStream {
        private final OutputStream delegate;

        NonClosingOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte @NotNull [] bytes, int off, int len) throws IOException {
            delegate.write(bytes, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() { /* no-op: underlying stream closed later by ConsoleTee */ }
    }
}
