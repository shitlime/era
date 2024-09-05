package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.service.BilibiliService;
import com.shitlime.era.utils.EraBotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Shiro
@Component
public class BilibiliPlugin extends BotPlugin {
    @Autowired
    BilibiliService bilibiliService;

    @Override
    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
        if (event.getArrayMsg().stream().anyMatch(m ->
                m.getData().get("text").matches(".*BV[0-9a-zA-Z]{10}.*"))) {
            // 识别到BV号自动发送视屏基本信息
            String msgPlain = EraBotUtils.getMsgPlain(event.getArrayMsg());
            Pattern pattern = Pattern.compile("BV[0-9a-zA-Z]{10}", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(msgPlain);
            if (matcher.find()) {
                String bvid = matcher.group();
                log.info("获取Bilibili视频信息：" + bvid);
                List<ArrayMsg> msgList = bilibiliService.autoGetVideoInfo(bvid);
                bot.sendMsg(event, msgList, true);
                // 如果消息不只有BV号，发送纯BV号
                if (!msgPlain.trim().equals(bvid)) {
                    bot.sendMsg(event, ArrayMsgUtils.builder().text(bvid).build(), true);
                }
                return MESSAGE_BLOCK;
            }
        }
        return MESSAGE_IGNORE;
    }
}
