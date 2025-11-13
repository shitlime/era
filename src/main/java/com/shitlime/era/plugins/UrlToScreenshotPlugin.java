package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.mikuac.shiro.dto.action.response.MsgResp;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.common.GroupMemberRoleConstants;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.pojo.dto.UrlToScreenshotDTO;
import com.shitlime.era.pojo.entry.UrlToScreenshot;
import com.shitlime.era.service.UrlToScreenshotService;
import com.shitlime.era.utils.EraBotUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Shiro
@Component
public class UrlToScreenshotPlugin extends SessionPlugin {
    private static final List<String> keywords = List.of("浏览", "打开网页", "打开网址", "浏览网页");
    private static final Pattern urlPattern = Pattern.compile(
    "(?:https?://)(?:[^\\s/.?=]+)(?:\\.(?:[^a-zA-Z0-9/]+|[a-zA-Z0-9]+))*(?::\\d+)?(?:/[a-zA-Z0-9\\-._~%/?#=&]*)?");
    private static final String mainCmdName = "网页截图";

    private static final String REMOVE_SESSION_TAG = "remove";
    private static final String ACTIVE_SESSION_TAG = "active";
    private static final List<MsgId> recallList = new ArrayList<>();
    private List<UrlToScreenshot> tempRuleList;

    @Autowired
    EraConfig eraConfig;
    @Autowired
    EraBotUtils eraBotUtils;
    @Autowired
    UrlToScreenshotService urlToScreenshotService;

    @SneakyThrows
    @Override
    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
        String msgPlain = EraBotUtils.getMsgPlain(event.getArrayMsg());
        Matcher urlMatcher = urlPattern.matcher(msgPlain);
        if (urlMatcher.find()) {
            // 识别url后截图
            String url = urlMatcher.group(0);
            return autoScreenshot(bot, event, url);
        } else if (eraBotUtils.isAdmin(event.getSender().getUserId())
                && EraBotUtils.getMsgPlain(event.getArrayMsg()).matches(getCmd())) {
            // 命令式网页截图 （需要bot admin权）
            return cmdUrlToScreenshot(bot, event);
        } else if (GroupMemberRoleConstants.ADMIN.equals(event.getSender().getRole())
                || GroupMemberRoleConstants.OWNER.equals(event.getSender().getRole())
                || eraBotUtils.isAdmin(event.getSender().getUserId())) {
            // 管理员|bot admin 可以使用的一系列规则CRUD
            if (hasSharedSession(event, REMOVE_SESSION_TAG)) {
                // 删除
                return removeRule(bot, event);
            } else if (hasSharedSession(event, ACTIVE_SESSION_TAG)) {
                // 启/停
                return toggleRule(bot, event);
            } else if (getHelpCmd().equals(event.getMessage())) {
                // 帮助
                return openHelp(bot, event);
            } else if (event.getMessage().matches(getAddCmd())) {
                // 添加规则
                return addRule(bot, event);
            } else if (getShowCmd().equals(event.getMessage())) {
                // 展示规则
                return showRule(bot, event);
            } else if (getRemoveCmd().equals(event.getMessage())) {
                // 打开删除会话
                return openOperateSession(bot, event, REMOVE_SESSION_TAG);
            } else if (getActiveCmd().equals(event.getMessage())) {
                // 打开启/停会话
                return openOperateSession(bot, event, ACTIVE_SESSION_TAG);
            }
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 识别url并截图
     * @param bot
     * @param event
     * @param url
     * @return
     */
    @SneakyThrows(java.net.MalformedURLException.class)
    private int autoScreenshot(Bot bot, AnyMessageEvent event, String url) {
        String host = new URL(url).getHost();
        UrlToScreenshotDTO urlToScreenshotDTO = new UrlToScreenshotDTO();
        urlToScreenshotDTO.setDomainName(host);
        urlToScreenshotDTO.setGroupId(event.getGroupId());
        urlToScreenshotDTO.setUserId(event.getUserId());
        if (urlToScreenshotService.checkRule(urlToScreenshotDTO)) {
            byte[] screenshot = urlToScreenshotService.getScreenshot(url);
            if (screenshot != null) {
                log.info("截图完成。{}Bytes", screenshot.length);
                bot.sendMsg(event, ArrayMsgUtils.builder()
                        .reply(event.getMessageId()).img(screenshot).build(), true);
            } else {
                log.info("截图失败。");
            }
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 展示规则
     * @param bot
     * @param event
     * @return
     */
    private int showRule(Bot bot, AnyMessageEvent event) {
        List<UrlToScreenshot> list =
                urlToScreenshotService.getRule(event.getGroupId(), event.getSender().getUserId());
        if (list == null || list.isEmpty()) {
            bot.sendMsg(event, ArrayMsgUtils.builder().text("- 空 -").build(), true);
            return MESSAGE_BLOCK;
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (int n = 0; n < list.size(); n++) {
            joiner.add(String.format("%s. %s[%s]",
                    n+1, list.get(n).getDomainName(), list.get(n).getEnable()? "启用":"停用"));
        }
        bot.sendMsg(event, ArrayMsgUtils.builder().text(joiner.toString()).build(), true);
        return MESSAGE_BLOCK;
    }

    /**
     * 添加规则
     * @param bot
     * @param event
     * @return
     */
    private int addRule(Bot bot, AnyMessageEvent event) {
        Pattern pattern = Pattern.compile(getAddCmd());
        Matcher matcher = pattern.matcher(event.getMessage());
        if (!matcher.find()) {
            return MESSAGE_IGNORE;
        }
        String host = matcher.group(1);
        UrlToScreenshotDTO urlToScreenshotDTO = new UrlToScreenshotDTO();
        urlToScreenshotDTO.setGroupId(event.getGroupId());
        urlToScreenshotDTO.setUserId(event.getSender().getUserId());
        urlToScreenshotDTO.setDomainName(host);
        log.info("{}添加新的规则：{}", event.getSender().getNickname(), host);
        String msg;
        if (urlToScreenshotService.addRule(urlToScreenshotDTO)) {
            msg = String.format("已添加%s", host);
        } else {
            msg = "添加失败";
        }
        bot.sendMsg(event, ArrayMsgUtils.builder().text(msg).build(), true);
        return MESSAGE_BLOCK;
    }

    /**
     * 帮助
     * @param bot
     * @param event
     * @return
     */
    private int openHelp(Bot bot, AnyMessageEvent event) {
        log.info("打开Url自动截图帮助。");

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("　　【Url自动截图插件】");
        joiner.add("1. 添加规则（域名）");
        joiner.add(getAddCmd());
        joiner.add("2. 查看已有规则");
        joiner.add(getShowCmd());
        joiner.add("3. 删除已有规则");
        joiner.add(getRemoveCmd());
        joiner.add("4. 启用/停用规则");
        joiner.add(getActiveCmd());
        joiner.add("5. 查看帮助");
        joiner.add(getHelpCmd());
        joiner.add("※以上群组内仅群主/管理有权限");

        bot.sendMsg(event, ArrayMsgUtils.builder().text(joiner.toString()).build(), true);
        return MESSAGE_BLOCK;
    }

    /**
     * 启用/停用规则
     * @param bot
     * @param event
     * @return
     */
    private int toggleRule(Bot bot, AnyMessageEvent event) {
        if (event.getMessage().matches("\\d+")) {
            int index = Integer.parseInt(event.getMessage()) - 1;
            if (index < 0 || index >= tempRuleList.size()) {
                return MESSAGE_BLOCK;
            }
            log.info("启用/停用Url自动截图规则。index={}", index);
            String msg;
            if (tempRuleList.get(index) != null
                    && urlToScreenshotService.toggleRuleEnable(
                            tempRuleList.get(index).getId(),
                            tempRuleList.get(index).getEnable())
            ) {
                msg = String.format("成功%s%s",
                        tempRuleList.get(index).getEnable()? "停用":"启用",
                        tempRuleList.get(index).getDomainName());
            } else {
                msg = String.format("%s失败", tempRuleList.get(index).getEnable()? "停用":"启用");
            }
            ActionData<MsgId> actionData = bot.sendMsg(event,
                    ArrayMsgUtils.builder().text(msg).text(" 输入|ok退出对话模式").build(), true);
            recallList.add(actionData.getData());
        } else if ("|ok".equals(event.getMessage())) {
            log.info("关闭{}会话", ACTIVE_SESSION_TAG);
            recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId()));
            closeSharedSession(event, ACTIVE_SESSION_TAG);
            tempRuleList = null;
        }
        return MESSAGE_BLOCK;
    }

    /**
     * 删除规则
     * @param bot
     * @param event
     * @return
     */
    private int removeRule(Bot bot, AnyMessageEvent event) {
        if (event.getMessage().matches("\\d+")) {
            int index = Integer.parseInt(event.getMessage()) - 1;
            if (index < 0 || index >= tempRuleList.size()) {
                return MESSAGE_BLOCK;
            }
            log.info("删除Url自动截图规则。index={}", index);
            String msg;
            if (tempRuleList.get(index) != null
                    && urlToScreenshotService.deleteRule(tempRuleList.get(index).getId())) {
                msg = String.format("成功删除%s", tempRuleList.get(index).getDomainName());
            } else {
                msg = "删除失败";
            }
            ActionData<MsgId> actionData = bot.sendMsg(event,
                    ArrayMsgUtils.builder().text(msg).text(" 输入|ok退出对话模式").build(), true);
            recallList.add(actionData.getData());
        } else if ("|ok".equals(event.getMessage())) {
            log.info("关闭{}会话", REMOVE_SESSION_TAG);
            recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId()));
            closeSharedSession(event, REMOVE_SESSION_TAG);
            tempRuleList = null;
        }
        return MESSAGE_BLOCK;
    }

    /**
     * 打开规则操作会话
     * @param bot
     * @param event
     * @param removeSessionTag
     * @return
     */
    private int openOperateSession(Bot bot, AnyMessageEvent event, String removeSessionTag) {
        log.info("打开{}会话。", removeSessionTag);
        tempRuleList = urlToScreenshotService.getRule(event.getGroupId(), event.getSender().getUserId());
        if (tempRuleList == null || tempRuleList.isEmpty()) {
            bot.sendMsg(event, ArrayMsgUtils.builder().text("- 空 -").build(), true);
            return MESSAGE_BLOCK;
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (int n = 0; n < tempRuleList.size(); n++) {
            joiner.add(String.format("%s. %s[%s]",
                    n+1, tempRuleList.get(n).getDomainName(), tempRuleList.get(n).getEnable()? "启用":"停用"));
        }
        joiner.add("> 输入要操作的编号");
        ActionData<MsgId> actionData = bot.sendMsg(event, ArrayMsgUtils.builder()
                .reply(event.getMessageId()).text(joiner.toString()).build(), true);
        recallList.add(actionData.getData());
        openSharedSession(event, removeSessionTag, 110,
                () -> recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId())));
        return MESSAGE_BLOCK;
    }

    /**
     * 命令式网页截图
     *
     * @param bot
     * @param event
     * @return
     */
    private int cmdUrlToScreenshot(Bot bot, AnyMessageEvent event) {
        List<ArrayMsg> replyMsg = null;
        for (ArrayMsg arrayMsg : event.getArrayMsg()) {
            if (MsgTypeEnum.reply.equals(arrayMsg.getType())) {
                int id = arrayMsg.getData().path("id").asInt();
                ActionData<MsgResp> data = bot.getMsg(id);
                replyMsg = data.getData().getArrayMsg();
                break;
            }
        }
        if (replyMsg == null || replyMsg.isEmpty()) {
            return MESSAGE_IGNORE;
        }

        // 提取url
        Matcher matcher = urlPattern.matcher(EraBotUtils.getMsgPlain(replyMsg));
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

    /**
     * 获取指令
     *
     * @return
     */
    private String getCmd() {
        String perfix = eraConfig.getBot().getCmd();
        // 如果前缀是一个特殊字符 "|" 或 "\\"（即 | 或 \），就对它进行转义（前面加一个 \）
        perfix = perfix.matches("(?:\\||\\\\)") ? "\\" + perfix : perfix;
        StringJoiner joiner = new StringJoiner("|", "(?:", ")");
        keywords.forEach(joiner::add);
        return String.format("%s%s", perfix, joiner);
    }

    private String getRemoveCmd() {
        return String.format("%s%s 删除", eraConfig.getBot().getCmd(), mainCmdName);
    }
    private String getActiveCmd() {
        return String.format("%s%s 启停", eraConfig.getBot().getCmd(), mainCmdName);
    }
    private String getHelpCmd() {
        return String.format("%s%s 帮助", eraConfig.getBot().getCmd(), mainCmdName);
    }
    private String getShowCmd() {
        return String.format("%s%s 规则", eraConfig.getBot().getCmd(), mainCmdName);
    }
    private String getAddCmd() {
        String perfix = eraConfig.getBot().getCmd();
        perfix = perfix.matches("(?:\\||\\\\)")? "\\"+perfix : perfix;
        return String.format("%s%s 添加 (\\S+)", perfix, mainCmdName);
    }
}
