package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.common.GroupMemberRoleConstants;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.pojo.dto.RssDTO;
import com.shitlime.era.service.RssManageService;
import com.shitlime.era.utils.EraBotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Shiro
@Component
public class RssManagePlugin extends SessionPlugin {
    private static final String REMOVE_SESSION_TAG = "remove";
    private static final String ENABLE_SESSION_TAG = "enable";
    private static final String DISABLE_SESSION_TAG = "disable";

    @Autowired
    private EraConfig eraConfig;
    @Autowired
    private EraBotUtils eraBotUtils;
    @Autowired
    private RssManageService rssManageService;

    @Override
    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
        // 群组中只有群主/管理/Bot管理才有权限访问Rss管理插件
        if (event.getGroupId() != null &&
            !(GroupMemberRoleConstants.ADMIN.equals(event.getSender().getRole())
                || GroupMemberRoleConstants.OWNER.equals(event.getSender().getRole()))
            && !eraBotUtils.isAdmin(event.getSender().getUserId())
        ) {
            return MESSAGE_IGNORE;
        }

        if (hasSession(event, REMOVE_SESSION_TAG)) {
            // 会话：删除rss
            if (event.getMessage().matches("\\d+")) {
                List<ArrayMsg> msg = rssManageService.removeRss(
                        event.getGroupId(), event.getSender().getUserId(),
                        Integer.parseInt(event.getMessage()));
                bot.sendMsg(event, msg, true);
            } else if ("|ok".equals(event.getMessage())) {
                closeSession(event, REMOVE_SESSION_TAG);
            }
            return MESSAGE_BLOCK;
        } else if (hasSession(event, ENABLE_SESSION_TAG)) {
            // 会话：启用rss
            if (event.getMessage().matches("\\d+")) {
                List<ArrayMsg> msg = rssManageService.enableRss(
                        event.getGroupId(), event.getSender().getUserId(),
                        Integer.parseInt(event.getMessage()));
                bot.sendMsg(event, msg, true);
            } else if ("|ok".equals(event.getMessage())) {
                closeSession(event, ENABLE_SESSION_TAG);
            }
        } else if (hasSession(event, DISABLE_SESSION_TAG)) {
            // 会话：禁用rss
            if (event.getMessage().matches("\\d+")) {
                List<ArrayMsg> msg = rssManageService.disableRss(
                        event.getGroupId(), event.getSender().getUserId(),
                        Integer.parseInt(event.getMessage()));
                bot.sendMsg(event, msg, true);
            } else if ("|ok".equals(event.getMessage())) {
                closeSession(event, DISABLE_SESSION_TAG);
            }
        } else if (getHelpCmd().equals(event.getMessage())) {
            // 帮助
            log.info("打开rss帮助");

            StringJoiner joiner = new StringJoiner("\n");
            joiner.add("　　【RSS插件】");
            joiner.add("by: Shitlime 2024-04-24");
            joiner.add("------ 帮助 ------");
            joiner.add("1.订阅新的RSS：");
            joiner.add(getAddCmd());
            joiner.add("2.查看已订阅RSS：");
            joiner.add(getShowCmd());
            joiner.add("3.删除订阅的RSS：");
            joiner.add(getRemoveCmd());
            joiner.add("4.启用RSS：");
            joiner.add(getEnableCmd());
            joiner.add("5.禁用RSS：");
            joiner.add(getDisableCmd());
            joiner.add("6.查看本帮助：");
            joiner.add(getHelpCmd());
            joiner.add("※群组内仅群主/管理有权限");

            bot.sendMsg(event, ArrayMsgUtils.builder()
                    .text(joiner.toString()).buildList(), true);
            return MESSAGE_BLOCK;
        } else if (event.getMessage().matches(getAddCmd())) {
            // 订阅
            Pattern pattern = Pattern.compile(getAddCmd());
            Matcher matcher = pattern.matcher(event.getMessage());
            matcher.find();
            String url = matcher.group(1);
            RssDTO rss = new RssDTO();
            rss.setUrl(url);
            rss.setGroupId(event.getGroupId());
            rss.setUserId(event.getSender().getUserId());

            log.info("{}添加新的rss订阅:{}", event.getSender().getNickname(), url);

            List<ArrayMsg> msg = rssManageService.addRss(rss);

            bot.sendMsg(event, msg, true);
            return MESSAGE_BLOCK;
        } else if (getShowCmd().equals(event.getMessage())) {
            // 展示订阅
            log.info("展示rss订阅");

            List<ArrayMsg> msg = rssManageService
                    .showRss(event.getGroupId(), event.getSender().getUserId());

            bot.sendMsg(event, msg, true);
            return MESSAGE_BLOCK;
        } else if (getRemoveCmd().equals(event.getMessage())) {
            // 删除订阅
            List<ArrayMsg> msg = rssManageService
                    .showRss(event.getGroupId(), event.getSender().getUserId());
            ArrayMsg arrayMsg = new ArrayMsg();
            arrayMsg.setType(MsgTypeEnum.text);
            HashMap<String, String> map = new HashMap<>();
            map.put("text", "\n> 输入要删除的编号");
            arrayMsg.setData(map);
            msg.add(arrayMsg);
            bot.sendMsg(event, msg, true);
            openSession(event, REMOVE_SESSION_TAG);
            return MESSAGE_BLOCK;
        } else if (getEnableCmd().equals(event.getMessage())) {
            // 启用
            List<ArrayMsg> msg = rssManageService
                    .showRss(event.getGroupId(), event.getSender().getUserId());
            ArrayMsg arrayMsg = new ArrayMsg();
            arrayMsg.setType(MsgTypeEnum.text);
            HashMap<String, String> map = new HashMap<>();
            map.put("text", "\n> 输入要启用的编号");
            arrayMsg.setData(map);
            msg.add(arrayMsg);
            bot.sendMsg(event, msg, true);
            openSession(event, ENABLE_SESSION_TAG);
        } else if (getDisableCmd().equals(event.getMessage())) {
            // 禁用
            List<ArrayMsg> msg = rssManageService
                    .showRss(event.getGroupId(), event.getSender().getUserId());
            ArrayMsg arrayMsg = new ArrayMsg();
            arrayMsg.setType(MsgTypeEnum.text);
            HashMap<String, String> map = new HashMap<>();
            map.put("text", "\n> 输入要禁用的编号");
            arrayMsg.setData(map);
            msg.add(arrayMsg);
            bot.sendMsg(event, msg, true);
            openSession(event, DISABLE_SESSION_TAG);
        }
        return MESSAGE_IGNORE;
    }

    private String getHelpCmd() {
        return String.format("%srss help", eraConfig.getBot().getCmd());
    }

    private String getAddCmd() {
        String perfix = eraConfig.getBot().getCmd();
        perfix = perfix.matches("(?:\\||\\\\)")? "\\"+perfix : perfix;
        return String.format("%srss sub (\\S+)", perfix);
    }

    private String getShowCmd() {
        return String.format("%srss show", eraConfig.getBot().getCmd());
    }

    private String getRemoveCmd() {
        return String.format("%srss rm", eraConfig.getBot().getCmd());
    }

    private String getEnableCmd() {
        return String.format("%srss enable", eraConfig.getBot().getCmd());
    }

    private String getDisableCmd() {
        return String.format("%srss disable", eraConfig.getBot().getCmd());
    }
}
