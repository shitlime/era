package com.shitlime.era.plugins;

import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SessionPlugin extends BotPlugin {
    private List<Map<String, Map<Long, Long>>> sessionList = new ArrayList<>();

    /**
     * 判断当前插件是否有会话
     * @param event
     * @param sessionTag tag 用于区分不同的会话
     * @return
     */
    protected boolean hasSession(AnyMessageEvent event, String sessionTag) {
        if (event == null || sessionList.isEmpty() || event.getSender() == null) {
            return false;
        }
        Map<String, Map<Long, Long>> map = toSessionListDataFormat(event, sessionTag);
        for (Map<String, Map<Long, Long>> sessionMap : sessionList) {
            if (map.equals(sessionMap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打开一个会话
     * @param event
     * @param sessionTag tag 用于区分不同的会话
     * @return 不需要使用返回值，只是不返回会导致 shiro 反射报错
     */
    protected boolean openSession(AnyMessageEvent event, String sessionTag) {
        Map<String, Map<Long, Long>> map = toSessionListDataFormat(event, sessionTag);
        sessionList.add(map);
        return true;
    }

    /**
     * 关闭一个会话
     * @param event
     * @param sessionTag tag 用于区分不同的会话
     * @return 不需要使用返回值，只是不返回会导致 shiro 反射报错
     */
    protected boolean closeSession(AnyMessageEvent event, String sessionTag) {
        Map<String, Map<Long, Long>> map = toSessionListDataFormat(event, sessionTag);
        for (Map<String, Map<Long, Long>> sessionMap : sessionList) {
            if (map.equals(sessionMap)) {
                sessionList.remove(sessionMap);
                return true;
            }
        }
        return false;
    }

    /**
     * 组装成 sessionList 中的数据格式
     * @param event
     * @param sessionTag
     * @return
     */
    private static Map<String, Map<Long, Long>> toSessionListDataFormat
    (AnyMessageEvent event, String sessionTag) {
        Map<Long,Long> idMap = new HashMap<>();
        idMap.put(event.getGroupId(), event.getSender().getUserId());
        Map<String, Map<Long, Long>> map = new HashMap<>();
        map.put(sessionTag, idMap);
        return map;
    }
}
