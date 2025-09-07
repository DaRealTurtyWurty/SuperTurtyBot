package dev.darealturtywurty.superturtybot.server;

import com.sun.management.UnixOperatingSystemMXBean;

import java.lang.management.*;
import java.util.*;

public class JvmStatsService {
    private final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
    private final List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
    private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    private final ClassLoadingMXBean classes = ManagementFactory.getClassLoadingMXBean();
    private final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

    public Map<String,Object> uptime() {
        return Map.of(
                "start_time_ms", runtime.getStartTime(),
                "uptime_ms", runtime.getUptime(),
                "name", runtime.getName(),           // pid@host
                "vm", Map.of("name", runtime.getVmName(), "vendor", runtime.getVmVendor(), "version", runtime.getVmVersion())
        );
    }

    public Map<String,Object> memory() {
        var heap = memory.getHeapMemoryUsage();
        var non  = memory.getNonHeapMemoryUsage();

        // Pool breakdown (works with G1/ZGC/Shenandoah; names vary so just report all)
        var poolList = pools.stream().map(p -> Map.of(
                "name", p.getName(),
                "type", p.getType().toString(),
                "usage", Map.of(
                        "init", p.getUsage() == null ? -1 : p.getUsage().getInit(),
                        "used", p.getUsage() == null ? -1 : p.getUsage().getUsed(),
                        "committed", p.getUsage() == null ? -1 : p.getUsage().getCommitted(),
                        "max", p.getUsage() == null ? -1 : p.getUsage().getMax()
                )
        )).toList();

        return Map.of(
                "heap", Map.of(
                        "used", heap.getUsed(),
                        "committed", heap.getCommitted(),
                        "max", heap.getMax()
                ),
                "non_heap", Map.of(
                        "used", non.getUsed(),
                        "committed", non.getCommitted(),
                        "max", non.getMax()
                ),
                "pools", poolList
        );
    }

    public Map<String,Object> gc() {
        long count = 0, timeMs = 0;
        var perCollector = new ArrayList<Map<String,Object>>();
        for (var gc : gcs) {
            count += Math.max(0, gc.getCollectionCount());
            timeMs += Math.max(0, gc.getCollectionTime());
            perCollector.add(Map.of(
                    "name", gc.getName(),
                    "count", gc.getCollectionCount(),
                    "time_ms", gc.getCollectionTime(),
                    "pools", Arrays.asList(gc.getMemoryPoolNames())
            ));
        }
        return Map.of(
                "total_count", count,
                "total_time_ms", timeMs,
                "collectors", perCollector
        );
    }

    public Map<String,Object> threads() {
        long[] deadlocks = threads.findDeadlockedThreads(); // returns null if none
        return Map.of(
                "live", threads.getThreadCount(),
                "daemon", threads.getDaemonThreadCount(),
                "peak", threads.getPeakThreadCount(),
                "deadlocked", deadlocks == null ? 0 : deadlocks.length
        );
    }

    public Map<String,Object> classes() {
        return Map.of(
                "loaded", classes.getLoadedClassCount(),
                "total_loaded", classes.getTotalLoadedClassCount(),
                "unloaded", classes.getUnloadedClassCount()
        );
    }

    public Map<String,Object> process() {
        // CPU load is 0..1 (average since last call), may be -1 initially
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double procCpu = osBean.getProcessCpuLoad();
        Map<String,Object> base = new LinkedHashMap<>();
        base.put("process_cpu_load", procCpu);
        base.put("available_processors", os.getAvailableProcessors());
        base.put("system_load_average_1m", os.getSystemLoadAverage());

        if (os instanceof UnixOperatingSystemMXBean uos) {
            base.put("open_fds", uos.getOpenFileDescriptorCount());
            base.put("max_fds", uos.getMaxFileDescriptorCount());
        }
        return base;
    }

    // One-shot snapshot if you want a single endpoint
    public Map<String,Object> snapshot() {
        return Map.of(
                "uptime", uptime(),
                "memory", memory(),
                "gc", gc(),
                "threads", threads(),
                "classes", classes(),
                "process", process()
        );
    }
}