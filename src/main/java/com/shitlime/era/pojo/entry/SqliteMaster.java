package com.shitlime.era.pojo.entry;

import lombok.Data;

@Data
public class SqliteMaster {
    private String type;
    private String name;
    private String tblName;
    private String rootpage;
    private String sql;
}
