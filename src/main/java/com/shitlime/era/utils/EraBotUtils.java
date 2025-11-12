package com.shitlime.era.utils;

import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.config.EraConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class EraBotUtils {
    @Autowired
    private EraConfig eraConfig;

    /**
     * 管理员权限验证
     * @param uid
     * @return
     */
    public boolean isAdmin(Long uid) {
        for (Long adminId : eraConfig.getBot().getAdmin()) {
            if (Objects.equals(uid, adminId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从消息链取得消息纯文本内容
     * @param listArrayMsg
     * @return
     */
    public static String getMsgPlain(List<ArrayMsg> listArrayMsg) {
        StringBuilder builder = new StringBuilder();
        listArrayMsg.forEach(arrayMsg -> {
            if (MsgTypeEnum.text.equals(arrayMsg.getType())) {
                builder.append(arrayMsg.getStringData("text"));
            }
        });
        return builder.toString();
    }
}
