package com.shitlime.era.service;

import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GetStatusResp;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.*;
import java.util.StringJoiner;

@Service
public class StateService {
    /**
     * 获取本程序的状态
     * @return
     */
    public String getEraState() {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("----- 大脑 -----");

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

        joiner.add("---------------");
        return joiner.toString();
    }

    /**
     * 获取 onebot 的状态
     * @param status
     * @return
     */
    public String getOnebotStatus(ActionData<GetStatusResp> status) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("----- 身体 -----");

        // 发送/接收消息次数
        joiner.add(String.format("发送：%s 接收：%s",
                status.getData().getStat().getMessageSent(), status.getData().getStat().getMessageReceived()));
        // 发/收包
        joiner.add(String.format("发包：%s 收包：%s",
                status.getData().getStat().getPacketSent(), status.getData().getStat().getPacketReceived()));
        // 连接断开次数
        joiner.add(String.format("断开：%s次", status.getData().getStat().getDisconnectTimes()));
        // 丢包数
        joiner.add(String.format("丢包：%s", status.getData().getStat().getPacketLost()));

        joiner.add("---------------");
        return joiner.toString();
    }
}
