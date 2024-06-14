package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.service.BaiduAIService;
import com.shitlime.era.utils.EraBotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Shiro
@Component
public class AIChatPlugin extends SessionPlugin {
    private static final String AI_CHAT_TAG = "ai_chat";

    @Autowired
    private EraConfig eraConfig;
    @Autowired
    private BaiduAIService baiduAIService;

    @Override
    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
        List<Long> atList = ShiroUtils.getAtList(event.getArrayMsg());
        if (atList.size() == 1
                && eraConfig.getBot().getId().equals(atList.getFirst())
                && event.getArrayMsg().stream().anyMatch(arrayMsg ->
                (MsgTypeEnum.text.equals(arrayMsg.getType())
                        && arrayMsg.getData().get("text") != null
                        && !arrayMsg.getData().get("text").isBlank()
                        && String.format("%s清除", eraConfig.getBot().getCmd())
                        .equals(arrayMsg.getData().get("text").strip())))
        ) {
            // 清除聊天历史
            log.info("{}清除了AI聊天历史记录", event.getSender().getNickname());
            baiduAIService.clear(event.getUserId());
            bot.sendMsg(event, ArrayMsgUtils.builder().text("已清除聊天历史").build(), true);
            return MESSAGE_BLOCK;
        } else if (atList.size() == 1
                && eraConfig.getBot().getId().equals(atList.getFirst())
                && !hasExclusiveSession(event, AI_CHAT_TAG)  // 判断是否正在进行一回合对话（不允许连续发出对话）
        ) {
            // 聊天对话
            openExclusiveSession(event, AI_CHAT_TAG, 200);  // 设置超时自动删除对话
            String msgPlain = EraBotUtils.getMsgPlain(event.getArrayMsg());
            log.info("AI聊天：{}", msgPlain);
            if (msgPlain == null || msgPlain.isBlank()) {
                msgPlain = "你是谁？";
            }
            String reply = baiduAIService.sendMessage(event.getUserId(), msgPlain);
            bot.sendMsg(event,
                    ArrayMsgUtils.builder()
                            .at(event.getUserId())
                            .text("\n")
                            .text(reply == null ? "发生未知错误，将清空历史对话。" : reply)
                            .build(),
                    true);
            closeExclusiveSession(event, AI_CHAT_TAG);  // 完成一回合对话
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }
}
