package com.shitlime.era.plugins;

import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public abstract class SessionPlugin extends BotPlugin {
    private final List<Map<String, Map<Long, Long>>> sessionList = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    /**
     * 判断当前插件是否有独享会话
     *
     * @param event      onebot event
     * @param sessionTag tag 用于区分不同的会话
     * @return
     */
    protected boolean hasExclusiveSession(AnyMessageEvent event, String sessionTag) {
        if (event == null || sessionList.isEmpty() || event.getSender() == null) {
            return false;
        }
        Map<String, Map<Long, Long>> map = toExclusiveSessionData(event, sessionTag);
        return hasSession(map);
    }

    /**
     * 判断当前插件是否有共享会话
     *
     * @param event      onebot event
     * @param sessionTag tag 用于区分不同的会话
     * @return
     */
    protected boolean hasSharedSession(AnyMessageEvent event, String sessionTag) {
        if (event == null || sessionList.isEmpty() || event.getSender() == null) {
            return false;
        }
        Map<String, Map<Long, Long>> map = toSharedSessionData(event, sessionTag);
        return hasSession(map);
    }

    /**
     * 判断是否有会话
     */
    private boolean hasSession(Map<String, Map<Long, Long>> requestData) {
        for (Map<String, Map<Long, Long>> sessionMap : sessionList) {
            if (requestData.equals(sessionMap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打开一个独享会话
     *
     * @param event      onebot event
     * @param sessionTag tag 用于区分不同的会话
     * @param timeToLive    会话超时时间
     * @param timeoutAction   超时执行代码块
     * @return 不需要使用返回值，只是不返回会导致 shiro 反射报错
     */
    protected boolean openExclusiveSession
    (AnyMessageEvent event, String sessionTag, int timeToLive, Runnable timeoutAction) {
        Map<String, Map<Long, Long>> map = toExclusiveSessionData(event, sessionTag);
        return openSession(map, timeToLive, timeoutAction);
    }

    /**
     * 打开一个独享会话
     *
     * @param event      onebot event
     * @param sessionTag tag 用于区分不同的会话
     * @return 不需要使用返回值，只是不返回会导致 shiro 反射报错
     */
    protected boolean openExclusiveSession(AnyMessageEvent event, String sessionTag) {
        return openExclusiveSession(event, sessionTag, 100, () -> {});
    }

    /**
     * 打开一个共享会话
     *
     * @param event      onebot event
     * @param sessionTag tag 用于区分不同的会话
     * @param timeToLive    会话超时时间
     * @param timeoutAction 超时执行代码块
     * @return 不需要使用返回值，只是不返回会导致 shiro 反射报错
     */
    protected boolean openSharedSession
    (AnyMessageEvent event, String sessionTag, int timeToLive, Runnable timeoutAction) {
        Map<String, Map<Long, Long>> map = toSharedSessionData(event, sessionTag);
        return openSession(map, timeToLive, timeoutAction);
    }

    /**
     * 打开一个共享会话
     *
     * @param event      onebot event
     * @param sessionTag tag 用于区分不同的会话
     * @return 不需要使用返回值，只是不返回会导致 shiro 反射报错
     */
    protected boolean openSharedSession(AnyMessageEvent event, String sessionTag) {
        return openSharedSession(event, sessionTag, 100, () -> {});
    }

    /**
     * 打开一个会话
     */
    private boolean openSession
    (Map<String, Map<Long, Long>> sessionData, int timeToLive, Runnable timeoutAction) {
        sessionList.add(sessionData);
        executor.schedule(() -> {
            sessionList.remove(sessionData);
            timeoutAction.run();
        }, timeToLive, TimeUnit.SECONDS);
        return true;
    }

    /**
     * 关闭一个独享会话
     *
     * @param event      onebot event
     * @param sessionTag tag 用于区分不同的会话
     * @return 不需要使用返回值，只是不返回会导致 shiro 反射报错
     */
    protected boolean closeExclusiveSession(AnyMessageEvent event, String sessionTag) {
        Map<String, Map<Long, Long>> map = toExclusiveSessionData(event, sessionTag);
        return closeSession(map);
    }

    /**
     * 关闭一个共享会话
     *
     * @param event      onebot event
     * @param sessionTag tag 用于区分不同的会话
     * @return 不需要使用返回值，只是不返回会导致 shiro 反射报错
     */
    protected boolean closeSharedSession(AnyMessageEvent event, String sessionTag) {
        Map<String, Map<Long, Long>> map = toSharedSessionData(event, sessionTag);
        return closeSession(map);
    }

    /**
     * 关闭一个会话
     */
    private boolean closeSession(Map<String, Map<Long, Long>> sessionData) {
        for (Map<String, Map<Long, Long>> sessionMap : sessionList) {
            if (sessionData.equals(sessionMap)) {
                sessionList.remove(sessionMap);
                return true;
            }
        }
        return false;
    }

    /**
     * 组装成群内单人独立（独享）的会话数据格式
     */
    private static Map<String, Map<Long, Long>> toExclusiveSessionData
    (AnyMessageEvent event, String sessionTag) {
        Map<Long, Long> idMap = new HashMap<>();
        idMap.put(event.getGroupId(), event.getSender().getUserId());
        Map<String, Map<Long, Long>> map = new HashMap<>();
        map.put(sessionTag, idMap);
        return map;
    }

    /**
     * 组装成群组内共享的会话数据格式
     */
    private static Map<String, Map<Long, Long>> toSharedSessionData
    (AnyMessageEvent event, String sessionTag) {
        Map<Long, Long> idMap = new HashMap<>();
        if (event.getGroupId() != null) {
            idMap.put(event.getGroupId(), null);
        } else {
            idMap.put(null, event.getSender().getUserId());
        }
        Map<String, Map<Long, Long>> map = new HashMap<>();
        map.put(sessionTag, idMap);
        return map;
    }
}
