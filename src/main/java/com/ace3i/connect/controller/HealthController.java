package com.ace3i.connect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/health")
public class HealthController {

    @GetMapping
    public String healthPage() {
        return "health";
    }

    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHealthStats() {
        Map<String, Object> stats = new HashMap<>();

        // RAM Info
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = allocatedMemory - freeMemory;

        stats.put("ramUsed", usedMemory / (1024 * 1024)); // MB
        stats.put("ramTotal", maxMemory / (1024 * 1024)); // MB
        stats.put("ramFree", (maxMemory - usedMemory) / (1024 * 1024)); // MB
        stats.put("ramPercent", (double) usedMemory / maxMemory * 100);

        // CPU Info
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double cpuLoad = osBean.getCpuLoad();
        if (cpuLoad < 0) cpuLoad = 0;
        stats.put("cpuLoad", String.format("%.2f", cpuLoad * 100));

        // Disk Info
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;

        stats.put("diskUsed", usedSpace / (1024 * 1024 * 1024)); // GB
        stats.put("diskTotal", totalSpace / (1024 * 1024 * 1024)); // GB
        stats.put("diskFree", freeSpace / (1024 * 1024 * 1024)); // GB
        stats.put("diskPercent", (double) usedSpace / totalSpace * 100);

        // Uptime
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptime = rb.getUptime();
        stats.put("uptime", formatUptime(uptime));

        return ResponseEntity.ok(stats);
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
    }
}
