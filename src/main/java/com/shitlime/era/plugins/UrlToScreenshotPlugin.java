package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GetMsgResp;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.service.UrlToScreenshotService;
import com.shitlime.era.utils.EraBotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Shiro
@Component
public class UrlToScreenshotPlugin extends BotPlugin {
    private static final List<String> keywords = List.of("浏览", "打开网页", "打开网址", "浏览网页");
    private static final Pattern urlPattern = Pattern.compile(
    "(?:https?://)(?:[^\\s/.?=]+)(?:\\.(?:[^a-zA-Z0-9/]+|[a-zA-Z0-9]+))*(?::\\d+)?(?:/[a-zA-Z0-9\\-._~%/?#=&]*)?");

    @Autowired
    EraConfig eraConfig;
    @Autowired
    EraBotUtils eraBotUtils;
    @Autowired
    UrlToScreenshotService urlToScreenshotService;

    @Override
    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
        if (eraBotUtils.isAdmin(event.getSender().getUserId())
                && EraBotUtils.getMsgPlain(event.getArrayMsg()).matches(getCmd())) {
        // 命令式网页截图 （需要bot admin权）
            String replyMsg = null;
            for (ArrayMsg arrayMsg : event.getArrayMsg()) {
                if (MsgTypeEnum.reply.equals(arrayMsg.getType())){
                    int id = Integer.parseInt(arrayMsg.getData().get("id"));
                    ActionData<GetMsgResp> data = bot.getMsg(id);
                    replyMsg = data.getData().getRawMessage();
                    break;
                }
            }
            if (replyMsg == null || replyMsg.isEmpty()) {
                return MESSAGE_IGNORE;
            }

            // 提取url
            Matcher matcher = urlPattern.matcher(replyMsg);
            if (!matcher.find()) {
                return MESSAGE_IGNORE;
            }
            String url = matcher.group(0);

            log.info("浏览Url并返回截图。");
            byte[] screenshot = urlToScreenshotService.getScreenshot(url);
            if (screenshot != null) {
                log.info("截图完成。{}Bytes", screenshot.length);
                bot.sendMsg(event,
                        ArrayMsgUtils.builder().reply(event.getMessageId()).img(screenshot).build(), true);
            } else {
                log.info("截图失败。");
                bot.sendMsg(event,
                        ArrayMsgUtils.builder().reply(event.getMessageId()).text("失败了喵").build(), true);
            }
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 获取指令
     * @return
     */
    private String getCmd() {
        String perfix = eraConfig.getBot().getCmd();
        // 如果前缀是一个特殊字符 "|" 或 "\\"（即 | 或 \），就对它进行转义（前面加一个 \）
        perfix = perfix.matches("(?:\\||\\\\)")? "\\"+perfix : perfix;
        StringJoiner joiner = new StringJoiner("|", "(?:", ")");
        keywords.forEach(joiner::add);
        return String.format("%s%s", perfix, joiner);
    }
}
