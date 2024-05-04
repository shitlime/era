package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.action.response.GetStatusResp;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.service.StateService;
import com.shitlime.era.utils.EraBotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 运行状态插件
 */
@Slf4j
@Shiro
@Component
public class StatePlugin extends BotPlugin {
    @Autowired
    private EraConfig eraConfig;
    @Autowired
    private EraBotUtils eraBotUtils;
    @Autowired
    private StateService stateService;

    /**
     * 查看运行状态
     *
     * @param bot   {@link Bot}
     * @param event {@link AnyMessageEvent}
     * @return
     */
    @Override
    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
        String cmd = String.format("%sstate", eraConfig.getBot().getCmd());
        if (cmd.equals(event.getMessage()) &&
                eraBotUtils.isAdmin(event.getSender().getUserId())
        ) {
            log.info("获取运行状态");
            String eraState = stateService.getEraState();
            GetStatusResp status = bot.getStatus();
            String onebotState = stateService.getOnebotStatus(status);

            StringJoiner joiner = new StringJoiner("\n");
            joiner.add(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时mm分ss")));
            joiner.add(eraState);
            joiner.add(onebotState);

            bot.sendMsg(event, ArrayMsgUtils.builder()
                    .text(joiner.toString()).build(), true);
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }
}
