package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GetMsgResp;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.service.TextToImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Shiro
@Component
public class TextToImagePlugin extends BotPlugin {
    @Autowired
    EraConfig eraConfig;
    @Autowired
    TextToImageService textToImageService;

    @Override
    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
        if (event.getMessage().matches(getCmd())) {
            // 命令式渲染
            Pattern pattern = Pattern.compile(getCmd());
            StringBuilder builder = new StringBuilder();
            event.getArrayMsg().forEach(arrayMsg -> {
                if (MsgTypeEnum.text.equals(arrayMsg.getType())) {
                    builder.append(arrayMsg.getData().get("text"));
                }
            });
            Matcher matcher = pattern.matcher(builder.toString());
            matcher.find();
            String tofu = matcher.group(1);
            sendImage(bot, event, tofu);
        } else if (
            event.getArrayMsg().stream().anyMatch(arrayMsg ->
            MsgTypeEnum.reply.equals(arrayMsg.getType()))
            && event.getArrayMsg().stream().anyMatch(arrayMsg ->
            (MsgTypeEnum.text.equals(arrayMsg.getType())
                    && "|豆腐".equals(arrayMsg.getData().get("text"))))
        ) {
            // 回复式渲染
            String replyMsg = null;
            for (ArrayMsg arrayMsg : event.getArrayMsg()) {
                if (MsgTypeEnum.reply.equals(arrayMsg.getType())) {
                    int id = Integer.parseInt(arrayMsg.getData().get("id"));
                    ActionData<GetMsgResp> data = bot.getMsg(id);
                    replyMsg = data.getData().getRawMessage();
                    break;
                }
            }
            if (replyMsg != null && !replyMsg.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                ShiroUtils.rawToArrayMsg(replyMsg).forEach(arrayMsg -> {
                    if (MsgTypeEnum.text.equals(arrayMsg.getType())) {
                        builder.append(arrayMsg.getData().get("text"));
                    }
                });
                sendImage(bot, event, builder.toString());
            }
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 发送豆腐块渲染后的图片
     * @param bot
     * @param event
     * @param tofu
     */
    private void sendImage(Bot bot, AnyMessageEvent event, String tofu) {
        log.info("渲染豆腐块{}", tofu);

        String base64;
        if (tofu.codePoints().count() > 17) {
            base64 = textToImageService.longToImageBase64(tofu);
        } else {
            base64 = textToImageService.shortToImageBase64(tofu);
        }
        bot.sendMsg(event, ArrayMsgUtils.builder().reply(event.getMessageId())
                .img("base64://" + base64).build(), true);
    }

    /**
     * 获取指令
     * @return
     */
    private String getCmd() {
        String perfix = eraConfig.getBot().getCmd();
        perfix = perfix.matches("(?:\\||\\\\)")? "\\"+perfix : perfix;
        return String.format("%s(?:豆腐|tofu|渲染)(.+)", perfix);
    }
}
