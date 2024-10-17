package com.shitlime.era.service;

import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.pojo.dto.Dataset;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.enums.DatasetTypeEnum;
import com.shitlime.era.handle.impl.PicFileSearchHandle;
import com.shitlime.era.handle.impl.TxtLineSearchHandle;
import com.shitlime.era.handle.impl.UnicodeSearchHandle;
import com.shitlime.era.mapper.DatasetMapper;
import com.shitlime.era.mapper.QuickSearchMapper;
import com.shitlime.era.pojo.config.dataset.DatasetConfig;
import com.shitlime.era.pojo.entry.QuickSearch;
import com.shitlime.era.utils.FileUtils;
import com.shitlime.era.utils.TableUtils;
import com.shitlime.era.utils.UnicodeUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 字符查询业务逻辑
 */
@Slf4j
@Service
public class CharacterSearchService {
    private final ArrayList<Dataset> datasetList = new ArrayList<>();
    private boolean isLoaded = false;

    @Autowired
    EraConfig eraConfig;
    @Autowired
    TxtLineSearchHandle txtLineSearchHandle;
    @Autowired
    PicFileSearchHandle picFileSearchHandle;
    @Autowired
    UnicodeSearchHandle unicodeSearchHandle;
    @Autowired
    private TableUtils tableUtils;
    @Autowired
    QuickSearchMapper quickSearchMapper;
    @Autowired
    private DatasetMapper datasetMapper;

    /**
     * 装载所有数据集
     */
    @SneakyThrows(value = IOException.class)
    public void loadDatasets() {
        // 优先补充unicode数据集
        DatasetConfig unicodeConfig = new DatasetConfig();
        unicodeConfig.setType(DatasetTypeEnum.UNICODE);
        unicodeConfig.setName("Unicode");
        unicodeConfig.setId("u");
        unicodeSearchHandle.load();
        datasetList.add(new Dataset(unicodeConfig, null, null));

        // 装载其他数据集
        String rootPath = System.getProperty("user.dir");
        log.info("当前 user.dir 目录：" + rootPath);
        File datasetPath = new File(rootPath,
                eraConfig.getPlugin().getCharacterSearch().getDataset().getPath());
        log.info("当前 datasetPath = " + datasetPath);
        File[] files = Objects.requireNonNull(datasetPath.listFiles());
        Arrays.sort(files, Comparator.comparing(File::getName));
        log.info("数据集文件夹内容：" + Arrays.toString(files));
        for (File file : files) {
            if (!file.isDirectory()) {
                log.info("【不是文件夹，跳过循环】");
                continue;
            }
            Dataset dataset = loadDatasetConfig(file);
            log.info("读取到数据集【{}】，正在装载...", dataset.getPath());

            switch (dataset.getDatasetConfig().getType()) {
                case DatasetTypeEnum.TXT_LINE -> {
                    txtLineSearchHandle.load(dataset);
                }
                case DatasetTypeEnum.PIC_FILE -> {
                    picFileSearchHandle.load(dataset);
                }
            }
            datasetList.add(dataset);

            log.info("数据集【{}】装载完成。", dataset.getDatasetConfig().getName());
        }
        isLoaded = true;
    }

    /**
     * 根据 id, keyword 查询结果
     *
     * @param datasetId
     * @param keyword
     * @return
     */
    public List<ArrayMsg> searchById(String datasetId, String keyword) {
        for (Dataset dataset : datasetList) {
            if (dataset.getDatasetConfig().getId().equals(datasetId)) {
                switch (dataset.getDatasetConfig().getType()) {
                    case UNICODE -> {
                        return unicodeSearch(dataset, keyword);
                    }
                    case TXT_LINE -> {
                        return txtLineSearch(dataset, keyword);
                    }
                    case PIC_FILE -> {
                        return picFileSearch(dataset, keyword);
                    }
                }
            }
        }
        return ArrayMsgUtils.builder().text("未找到对应的数据集").build();
    }

    /**
     * 根据 keyword 查询所有数据集结果
     *
     * @param keyword
     * @return
     */
    public List<String> searchAllDataset(String keyword) {
        if (datasetList.isEmpty()) {
            return null;
        }

        List<String> msgList = new ArrayList<>();
        for (Dataset dataset : datasetList) {
            switch (dataset.getDatasetConfig().getType()) {
                case UNICODE -> {
                    msgList.add(ShiroUtils.arrayMsgToCode(
                            unicodeSearch(dataset, keyword)));
                }
                case TXT_LINE -> {
                    msgList.add(ShiroUtils.arrayMsgToCode(
                            txtLineSearch(dataset, keyword)));
                }
                case PIC_FILE -> {
                    msgList.add(ShiroUtils.arrayMsgToCode(
                            picFileSearch(dataset, keyword)));
                }
            }
        }
        return msgList;
    }

    /**
     * 设置快捷查询
     *
     * @param quickSearch
     */
    public List<ArrayMsg> setQuickSearch(QuickSearch quickSearch) {
        if (!consistDatasetList(quickSearch.getDatasetId())) {
            return ArrayMsgUtils.builder().text("未找到对应的数据集").build();
        }

        if (tableUtils.isExist(QuickSearchMapper.tableName)) {
            // 清除旧的快捷查询
            QuickSearch qs = new QuickSearch();
            qs.setUserId(quickSearch.getUserId());
            qs.setGroupId(quickSearch.getGroupId());
            List<QuickSearch> quickSearchList = quickSearchMapper.select(qs);
            if (quickSearchList != null && !quickSearchList.isEmpty()) {
                quickSearchList.forEach(q -> {
                    quickSearchMapper.delete(q.getId());
                });
            }
        } else {
            quickSearchMapper.createTable();
        }

        // 插入新的快捷查询
        quickSearch.setCreateTime(LocalDateTime.now());
        quickSearchMapper.insert(quickSearch);
        return ArrayMsgUtils.builder().text(
                        String.format("快键查询%s已设置为『%s』", quickSearch.getPrefix(),
                                getDatasetNameById(quickSearch.getDatasetId())))
                .build();
    }

    /**
     * 快捷查询 （求同存异，缺省时群友共享）
     *
     * @param quickSearch
     * @param keyword
     * @return
     */
    public List<ArrayMsg> quickSearch(QuickSearch quickSearch, String keyword) {
        if (!tableUtils.isExist(QuickSearchMapper.tableName)) {
            quickSearchMapper.createTable();
        } else {
            // 优先匹配群号和用户号，没有时尝试只匹配群号，最后尝试匹配用户号
            QuickSearch uid = new QuickSearch();
            uid.setUserId(quickSearch.getUserId());
            QuickSearch gid = new QuickSearch();
            gid.setGroupId(quickSearch.getGroupId());
            List<QuickSearch> quickSearchList = quickSearchMapper.select(quickSearch);
            quickSearchList = (gid.getGroupId() != null &&
                    (quickSearchList == null || quickSearchList.isEmpty())) ?
                    quickSearchMapper.select(gid) : quickSearchList;
            quickSearchList = (quickSearchList == null || quickSearchList.isEmpty()) ?
                    quickSearchMapper.select(uid) : quickSearchList;
            if (quickSearchList != null && !quickSearchList.isEmpty()) {
                QuickSearch search = quickSearchList.getFirst();
                return searchById(search.getDatasetId(), keyword);
            }
        }
        return ArrayMsgUtils.builder()
                .text(String.format("未设置快捷查询，可通过“%s　set　<数据集id>”设置",
                        quickSearch.getPrefix()))
                .build();
    }

    /**
     * 清空快捷查询设置
     *
     * @param userId
     */
    public void clearQuickSearch(Long userId) {
        quickSearchMapper.deleteByUser(userId);
    }


    /**
     * 列出所有数据集
     *
     * @return
     */
    public List<ArrayMsg> datasetList() {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(String.format("= %s =", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时mm分ss"))));
        joiner.add("已装载数据集：");
        datasetList.forEach(dataset -> {
            String tableName = dataset.getTableName();
            Long size = 0L;
            if (tableName != null) {
                size = datasetMapper.count(tableName);
            }
            DatasetConfig datasetConfig = dataset.getDatasetConfig();
            joiner.add(String.format("%s.『%s』%s条",
                    datasetConfig.getId(),
                    datasetConfig.getName(),
                    size != 0 ? size : "-"));
        });
        if (!isLoaded) {
            joiner.add("= 正在装载中... =");
        } else {
            joiner.add("= End =");
        }
        return ArrayMsgUtils.builder().text(joiner.toString()).build();
    }

    /**
     * 加载数据集配置文件
     *
     * @param file
     * @throws IOException
     */
    private Dataset loadDatasetConfig(File file) throws IOException {
        log.info("加载数据集配置文件1");
        File configPath = new File(file, "config.yml");
        log.info("加载数据集配置文件2");
        log.info("Config path: " + configPath.getAbsolutePath());
        log.info("File exists: " + configPath.exists());
        log.info("File readable: " + configPath.canRead());
        FileReader reader = new FileReader(configPath);
        log.info("加载数据集配置文件3");
        Yaml yaml = new Yaml();
        log.info("加载数据集配置文件4");
        DatasetConfig datasetConfig = new DatasetConfig();
        try {
            datasetConfig = yaml.loadAs(reader, DatasetConfig.class);
        } catch (Exception e) {
            log.error("YAML 解析出错: ", e);
        }
        log.info("加载数据集配置文件5");
        reader.close();
        log.info("加载数据集配置文件6");
        String tableName = eraConfig.getPlugin().getCharacterSearch()
                .getDataset().getTableNamePrefix() + datasetConfig.getId();
        log.info("加载数据集配置文件7");
        return new Dataset(datasetConfig, tableName, file);
    }

    /**
     * unicode数据集搜索逻辑
     *
     * @param dataset
     * @param string
     * @return
     */
    private List<ArrayMsg> unicodeSearch(Dataset dataset, String string) {
        StringJoiner joiner = new StringJoiner("\n");
        for (int codePoint : string.codePoints().toArray()) {
            String character = UnicodeUtils.unicodeToCharacter(codePoint);
            String result = unicodeSearchHandle.search(dataset, character);
            joiner.add(String.format("【%s】%s", character, result));
        }
        return ArrayMsgUtils.builder()
                .text(String.format("[%s]\n%s",
                        dataset.getDatasetConfig().getName(), joiner))
                .build();
    }

    /**
     * 图片文件数据集搜索逻辑
     *
     * @param dataset
     * @param string
     * @return
     */
    private List<ArrayMsg> picFileSearch(Dataset dataset, String string) {
        ArrayMsgUtils msgUtils = ArrayMsgUtils.builder()
                .text(String.format("[%s]\n",
                        dataset.getDatasetConfig().getName()));
        for (int codePoint : string.codePoints().toArray()) {
            String character = UnicodeUtils.unicodeToCharacter(codePoint);
            msgUtils.text(String.format("【%s】", character));
            List<String> result = picFileSearchHandle.search(dataset, character);
            result.forEach(pic ->
                    msgUtils.img("base64://" + FileUtils.fileToBase64(pic)));
        }
        return msgUtils.build();
    }

    /**
     * 文本行数据集搜索逻辑
     *
     * @param dataset
     * @param string
     * @return
     */
    private List<ArrayMsg> txtLineSearch(Dataset dataset, String string) {
        StringJoiner joiner = new StringJoiner("\n");
        for (int codePoint : string.codePoints().toArray()) {
            String character = UnicodeUtils.unicodeToCharacter(codePoint);
            String result = txtLineSearchHandle.search(dataset, character);
            joiner.add(String.format("【%s】%s", character, result));
        }
        return ArrayMsgUtils.builder()
                .text(String.format("[%s]\n%s",
                        dataset.getDatasetConfig().getName(), joiner))
                .build();
    }

    /**
     * 判断是否存在该 id 的数据集
     *
     * @param datasetId
     * @return
     */
    private boolean consistDatasetList(String datasetId) {
        for (Dataset dataset : datasetList) {
            if (dataset.getDatasetConfig().getId().equals(datasetId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据数据集 id 获取数据集 name
     *
     * @param datasetId
     * @return name or null
     */
    private String getDatasetNameById(String datasetId) {
        for (Dataset dataset : datasetList) {
            if (dataset.getDatasetConfig().getId().equals(datasetId)) {
                return dataset.getDatasetConfig().getName();
            }
        }
        return null;
    }
}
