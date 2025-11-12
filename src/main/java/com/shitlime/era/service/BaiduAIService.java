package com.shitlime.era.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shitlime.era.config.EraConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class BaiduAIService {
    private static final String ROLE = "role";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String CONTENT = "content";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";
    private static final String TOKEN_FILE_NAME = "baidu-ai-access-token.properties";
    private static final String TOKEN_FILE_COMMENTS = "baidu ai access token";

    @Autowired
    private EraConfig eraConfig;

    private List<Map<Long, Map<String, List<Map<String, String>>>>> chatSessionList =
            new CopyOnWriteArrayList<>();

    private final ObjectMapper objectmapper;

    public BaiduAIService(ObjectMapper objectmapper) {
        this.objectmapper = objectmapper;
    }

    /**
     * 向AI发送信息
     *
     * @param userId  用户id
     * @param message 发送的信息
     * @return AI的回复
     */
    public String sendMessage(Long userId, String message) {
        Map<Long, Map<String, List<Map<String, String>>>> userChatSession = null;
        for (Map<Long, Map<String, List<Map<String, String>>>> chatSessionMap : chatSessionList) {
            if (chatSessionMap.containsKey(userId)) {
                userChatSession = chatSessionMap;
            }
        }
        if (userChatSession == null) {
            // 初始化一个会话
            userChatSession = new HashMap<>();
            userChatSession.put(userId, getInitialSession());
            chatSessionList.add(userChatSession);
        }

        Map<String, List<Map<String, String>>> msgData = userChatSession.get(userId);
        List<Map<String, String>> messages = msgData.get("messages");
        messages.add(Map.of(ROLE, ROLE_USER, CONTENT, message));
        // todo 检查是否超过8k本文，超过则清空/删除旧记录 （目前采用出错即清空策略）
        log.debug("当前的messages={}", messages);
        // 添加调整参数
        Map<String, Object> data = new HashMap<>();
        data.putAll(msgData);
        data.put("temperature", 0.56);
        data.put("top_k", 80);
        data.put("top_p", 0.6);
        data.put("penalty_score", 1.35);
        data.put("stop", List.of("<im_end>"));
        data.put("user_id", "6655def5-2f37-e59d-bda4-2fe8df51591c");

        String result = sendMsgData(data);
        if (result != null) {
            messages.add(Map.of(ROLE, ROLE_ASSISTANT, CONTENT, result));
            userChatSession.put(userId, msgData);
        } else {
            chatSessionList.remove(userChatSession);
        }
        return result;
    }

    /**
     * 清除用户聊天历史
     *
     * @param userId
     */
    public void clear(Long userId) {
        chatSessionList.removeIf(chatSessionMap -> chatSessionMap.containsKey(userId));
    }

    /**
     * 组装初始会话数据
     *
     * @return
     */
    private Map<String, List<Map<String, String>>> getInitialSession() {
        List<String> initialChat = eraConfig.getPlugin().getAiChat().getBaidu().getInitialChat();
        if (initialChat == null || initialChat.size() % 2 != 0) {
            throw new IllegalArgumentException("initial-chat 未正确配置");
        }
        List<Map<String, String>> msgList = new ArrayList<>();
        for (int i = 0; i < initialChat.size(); i++) {
            if (i % 2 == 0) {
                msgList.add(Map.of(ROLE, ROLE_USER, CONTENT, initialChat.get(i)));
            } else {
                msgList.add(Map.of(ROLE, ROLE_ASSISTANT, CONTENT, initialChat.get(i)));
            }
        }
        Map<String, List<Map<String, String>>> map = new HashMap<>();
        map.put("messages", msgList);
        return map;
    }

    /**
     * 发送对话请求
     *
     * @param msgData
     * @return
     */
    private synchronized String sendMsgData(Map<String, Object> msgData) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            String jsonMsg = objectmapper.writeValueAsString(msgData);
            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(new URI(String.format("https://aip.baidubce.com/rpc/2.0/ai_custom/v1" +
                                    "/wenxinworkshop/chat/%s?access_token=%s",
                            eraConfig.getPlugin().getAiChat().getBaidu().getModelId(),
                            getAccessToken())))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMsg))
                    .build();
            HttpResponse<String> response = client.send(request2, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map map = objectmapper.readValue(response.body(), Map.class);
                log.debug("response={}", response);
                Object result = map.get("result");
                if (result != null) {
                    return result.toString();
                }
            }
            return null;
        } catch (URISyntaxException | InterruptedException | IOException e) {
            log.debug(e.toString());
            return null;
        }
    }

    /**
     * 获取 access token
     *
     * @return
     */
    @SneakyThrows(IOException.class)
    private String getAccessToken() {
        File tokenFile = new File(eraConfig.getResources().getPath().getTemp(), TOKEN_FILE_NAME);
        if (tokenFile.exists() && tokenFile.isFile()) {
            FileReader reader = new FileReader(tokenFile);
            Properties properties = new Properties();
            properties.load(reader);
            reader.close();
            String expiresTime = properties.getProperty(EXPIRES_IN);
            if (expiresTime == null
                    || expiresTime.isBlank()
                    || Long.parseLong(expiresTime) < Instant.now().getEpochSecond()
            ) {
                Map<String, String> responseData = getAccessTokenData();
                properties.putAll(responseData);
                FileWriter writer = new FileWriter(tokenFile);
                properties.store(writer, TOKEN_FILE_COMMENTS);
                writer.close();
                return properties.getProperty(ACCESS_TOKEN);
            } else {
                return properties.getProperty(ACCESS_TOKEN);
            }
        } else {
            Files.createDirectories(tokenFile.getParentFile().toPath());
            Files.createFile(tokenFile.toPath());
            Map<String, String> responseData = getAccessTokenData();
            Properties properties = new Properties();
            properties.putAll(responseData);
            FileWriter writer = new FileWriter(tokenFile);
            properties.store(writer, TOKEN_FILE_COMMENTS);
            writer.close();
            return properties.getProperty(ACCESS_TOKEN);
        }
    }

    /**
     * 获取新的 access token
     *
     * @return
     */
    private Map<String, String> getAccessTokenData() {
        String url = String.format("https://aip.baidubce.com/oauth/2.0/token" +
                        "?grant_type=client_credentials&client_id=%s&client_secret=%s",
                eraConfig.getPlugin().getAiChat().getBaidu().getApiKey(),
                eraConfig.getPlugin().getAiChat().getBaidu().getSecretKey());
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> result =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            Map responseData = objectmapper.readValue(result.body().toString(), Map.class);
            Long expires_in = Instant.now().getEpochSecond()
                    + Long.parseLong(responseData.get(EXPIRES_IN).toString()) - 10;
            responseData.put(EXPIRES_IN, expires_in.toString());
            return responseData;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
