package dev.darealturtywurty.superturtybot.commands.core;

import com.sun.management.OperatingSystemMXBean;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class SystemStatsCommand extends CoreCommand {
    public SystemStatsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Shows the system stats of the bot.";
    }

    @Override
    public String getName() {
        return "systemstats";
    }

    @Override
    public String getRichName() {
        return "System Stats";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().setContent("Loading...").queue();
        var sysInfo = new SystemInfo();

        // get cpu
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double cpu = osBean.getCpuLoad() * 100;
        int processes = sysInfo.getOperatingSystem().getProcessCount();
        int threads = sysInfo.getOperatingSystem().getThreadCount();
        long uptime = sysInfo.getOperatingSystem().getSystemUptime() * 1000;

        // get ram
        long availableRam = sysInfo.getHardware().getMemory().getAvailable();
        long totalRam = sysInfo.getHardware().getMemory().getTotal();
        long usedRam = totalRam - availableRam;
        double usedRamPercent = (double) usedRam / totalRam * 100;
        long virtualUsed = sysInfo.getHardware().getMemory().getVirtualMemory().getVirtualInUse();
        long virtualTotal = sysInfo.getHardware().getMemory().getVirtualMemory().getVirtualMax();
        double virtualUsage = (double) virtualUsed / (double) virtualTotal * 100;
        long pageSize = sysInfo.getHardware().getMemory().getPageSize();

        // get disk

        /*HWDiskStore disk = sysInfo.getHardware().getDiskStores().get(0);
        disk.updateAttributes();

        long readBytes = disk.getReadBytes();
        long writeBytes = disk.getWriteBytes();
        long diskTimestamp = disk.getTimeStamp();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        disk.updateAttributes();
        long newReadBytes = disk.getReadBytes();
        long newWriteBytes = disk.getWriteBytes();
        long newDiskTimestamp = disk.getTimeStamp();

        long readBytesPerSecond = (newReadBytes - readBytes) / (newDiskTimestamp - diskTimestamp);
        long writeBytesPerSecond = (newWriteBytes - writeBytes) / (newDiskTimestamp - diskTimestamp);*/

        // get network
        NetworkIF net = sysInfo.getHardware().getNetworkIFs().get(0);
        net.updateAttributes();

        /*long bytesSent = sysInfo.getHardware().getNetworkIFs().get(0).getBytesSent();
        long bytesRecv = sysInfo.getHardware().getNetworkIFs().get(0).getBytesRecv();
        long timestamp = net.getTimeStamp();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        net.updateAttributes();
        long newBytesSent = sysInfo.getHardware().getNetworkIFs().get(0).getBytesSent();
        long newBytesRecv = sysInfo.getHardware().getNetworkIFs().get(0).getBytesRecv();
        long newTimestamp = net.getTimeStamp();
        long sentSpeed = (newBytesSent - bytesSent) / ((newTimestamp - timestamp) / 1000);
        long recvSpeed = (newBytesRecv - bytesRecv) / ((newTimestamp - timestamp) / 1000);*/

        long packetsSent = sysInfo.getHardware().getNetworkIFs().get(0).getPacketsSent();
        long packetsRecv = sysInfo.getHardware().getNetworkIFs().get(0).getPacketsRecv();

        // get operating system
        String osName = sysInfo.getOperatingSystem().getFamily();
        String osVersion = sysInfo.getOperatingSystem().getVersionInfo().getVersion();
        String osArch = sysInfo.getOperatingSystem().getBitness() + " bit";
        String osManufacturer = sysInfo.getOperatingSystem().getManufacturer();
        long osBootTime = sysInfo.getOperatingSystem().getSystemBootTime() * 1000;

        // get java
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");

        // get dependencies
        String jdaVersion = JDAInfo.VERSION;

        var stats = new SystemStats(cpu, processes, threads, uptime, availableRam, totalRam, usedRamPercent,
                virtualUsed, virtualTotal, virtualUsage, pageSize, packetsSent, packetsRecv, osName, osVersion, osArch,
                osManufacturer, osBootTime, javaVersion, javaVendor, jdaVersion);

        BufferedImage image = createStatsImage(stats);
        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpeg", baos);
        } catch (IOException exception) {
            exception.printStackTrace();
            reply(event, "An error occurred while creating the stats image!", false, true);
            return;
        }

        var upload = FileUpload.fromData(baos.toByteArray(), "system_stats.jpeg");
        event.getHook().editOriginal("Here are the system stats!").setFiles(upload).queue();
    }

    private static BufferedImage createStatsImage(SystemStats stats) {
        var image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.decode("#222737"));
        graphics.fillRect(0, 0, 1000, 1000);

        var font = new Font("Arial", Font.BOLD, 30);
        var font2 = new Font("Arial", Font.PLAIN, 20);
        graphics.setFont(font);
        int gap = graphics.getFontMetrics(font).getHeight() + 10;

        graphics.setColor(Color.decode("#EEEEEE"));

        graphics.drawString("CPU", 10, gap);
        graphics.setFont(font2);
        graphics.drawString("Usage: " + String.format("%.2f", stats.cpu) + "%", 10, gap * 2);
        graphics.drawString("Processes: " + stats.processes, 10, gap * 3);
        graphics.drawString("Threads: " + stats.threads, 10, gap * 4);
        graphics.drawString("Uptime: " + UptimeCommand.millisecondsFormatted(stats.uptime), 10, gap * 5);

        graphics.setFont(font);
        graphics.drawString("RAM", 10, gap * 7);
        graphics.setFont(font2);
        graphics.drawString("Available: " + bytesFormatted(stats.availableRam), 10, gap * 8);
        graphics.drawString("Total: " + bytesFormatted(stats.totalRam), 10, gap * 9);
        graphics.drawString("Used: " + String.format("%.2f", stats.ramUsage) + "%", 10, gap * 10);
        graphics.drawString("Virtual Used: " + bytesFormatted(stats.virtualUsed), 10, gap * 11);
        graphics.drawString("Virtual Total: " + bytesFormatted(stats.virtualTotal), 10, gap * 12);
        graphics.drawString("Virtual Usage: " + String.format("%.2f", stats.virtualUsage) + "%", 10, gap * 13);
        graphics.drawString("Page Size: " + bytesFormatted(stats.pageSize), 10, gap * 14);

        //graphics.setFont(font);
        //graphics.drawString("Disk", 10, gap * 16);
        //graphics.setFont(font2);
        //graphics.drawString("Read Speed: " + bytesFormatted(stats.readSpeed) + "/s", 10, gap * 17);
        //graphics.drawString("Write Speed: " + bytesFormatted(stats.writeSpeed) + "/s", 10, gap * 18);

        graphics.setFont(font);
        graphics.drawString("Network", 500, gap);
        graphics.setFont(font2);
        //graphics.drawString("Sent: " + bytesFormatted(stats.bytesSent), 500, gap * 2);
        //graphics.drawString("Received: " + bytesFormatted(stats.bytesReceived), 500, gap * 3);
        graphics.drawString("Packets Sent: " + stats.packetsSent, 500, gap * 2);
        graphics.drawString("Packets Received: " + stats.packetsReceived, 500, gap * 3);
        //graphics.drawString("Upload Speed: " + bytesFormatted(stats.sentSpeed) + "/s", 500, gap * 6);
        //graphics.drawString("Download Speed: " + bytesFormatted(stats.receiveSpeed) + "/s", 500, gap * 7);

        graphics.setFont(font);
        graphics.drawString("Operating System", 500, gap * 5);
        graphics.setFont(font2);
        graphics.drawString("Name: " + stats.osName, 500, gap * 6);
        graphics.drawString("Version: " + stats.osVersion, 500, gap * 7);
        graphics.drawString("Architecture: " + stats.osArch, 500, gap * 8);
        graphics.drawString("Manufacturer: " + stats.osManufacturer, 500, gap * 9);
        graphics.drawString("Boot Time: " + UptimeCommand.millisecondsFormatted(stats.osBootTime), 500, gap * 10);

        graphics.setFont(font);
        graphics.drawString("Java", 500, gap * 12);
        graphics.setFont(font2);
        graphics.drawString("Version: " + stats.javaVersion, 500, gap * 13);
        graphics.drawString("Vendor: " + stats.javaVendor, 500, gap * 14);

        graphics.setFont(font);
        graphics.drawString("Dependencies", 500, gap * 16);
        graphics.setFont(font2);
        graphics.drawString("JDA: " + stats.jdaVersion, 500, gap * 17);

        graphics.dispose();
        return image;
    }

    private static String bytesFormatted(long bytes) {
        String unit = "B";
        double value = bytes;
        if (value > 1024) {
            value /= 1024.0D;
            unit = "KB";
        }

        if (value > 1024) {
            value /= 1024.0D;
            unit = "MB";
        }

        if (value > 1024) {
            value /= 1024.0D;
            unit = "GB";
        }

        if (value > 1024) {
            value /= 1024.0D;
            unit = "TB";
        }

        if (value > 1024) {
            value /= 1024.0D;
            unit = "PB";
        }

        return String.format("%.2f %s", value, unit);
    }

    private record SystemStats(double cpu, int processes, int threads, long uptime, long availableRam, long totalRam,
                               double ramUsage, long virtualUsed, long virtualTotal, double virtualUsage, long pageSize,
                               long packetsSent, long packetsReceived, String osName, String osVersion, String osArch,
                               String osManufacturer, long osBootTime, String javaVersion, String javaVendor,
                               String jdaVersion) {
    }
}
