package com.shitlime.era.service;

import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.model.ArrayMsg;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

@Service
public class StateService {
    public List<ArrayMsg> getState() {
        StringJoiner joiner = new StringJoiner("\n");
        // 时间
        joiner.add(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时mm分ss")));

        // 获取操作系统相关信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        joiner.add("操作系统: " + osBean.getName());
        joiner.add("cpu架构: " + osBean.getArch());
        joiner.add("cpu核心数: " + osBean.getAvailableProcessors());
        joiner.add(String.format("系统负载平均值: %.2f", osBean.getSystemLoadAverage()));

        // 获取JVM运行时间相关信息
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime();
        // 将uptime转换为Duration
        Duration duration = Duration.ofMillis(uptime);
        // 获取小时、分钟和秒
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        joiner.add("运行时长: " + hours + "小时" + minutes + "分钟" + seconds + "秒");

        // 获取内存使用情况
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long used2 = memoryBean.getNonHeapMemoryUsage().getUsed();
        joiner.add(String.format("内存使用: %.2fMB", (used+used2)/1024.0/1024.0));

        return ArrayMsgUtils.builder().text(joiner.toString()).buildList();
    }
}
