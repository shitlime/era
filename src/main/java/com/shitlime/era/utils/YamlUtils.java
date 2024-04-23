package com.shitlime.era.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class YamlUtils {
    /**
     * 将类保存成 yaml
     * @param object
     * @param path
     * @throws IOException
     */
    public static void dumpYaml(Object object, File path) throws IOException {
        Yaml yaml = new Yaml();
        String dumped = yaml.dumpAsMap(object);
        FileWriter writer = new FileWriter(path);
        writer.write(dumped);
        writer.close();
    }
}
