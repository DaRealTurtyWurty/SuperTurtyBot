package io.github.darealturtywurty.superturtybot.core.console;

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class FileMonitor {
    private FileAlterationMonitor monitor;
    
    public FileMonitor(long interval) {
        this.monitor = new FileAlterationMonitor(interval);
    }
    
    public void monitor(String path, String fileName, FileAlterationListener listener) {
        final var observer = new FileAlterationObserver(new File(path),
            pathname -> pathname.getName().equals(fileName));
        this.monitor.addObserver(observer);
        observer.addListener(listener);
    }
    
    public void start() {
        try {
            this.monitor.start();
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }
    
    public void stop() {
        try {
            this.monitor.stop();
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }
}
