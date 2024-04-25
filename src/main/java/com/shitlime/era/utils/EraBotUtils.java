package com.shitlime.era.utils;

import com.shitlime.era.config.EraConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
}
