package com.shitlime.era.service;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.handle.impl.PlaywrightHandle;
import com.shitlime.era.pojo.dto.PageSettingDTO;
import lombok.SneakyThrows;
import org.jsoup.nodes.Entities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Service
public class TextToImageService {
    private Page page;

    @Autowired
    PlaywrightHandle playwrightHandle;
    @Autowired
    EraConfig eraConfig;

    /**
     * 长文本渲染。带边框和边距。
     * @param text
     * @return
     */
    public byte[] longToImage(String text) {
        PageSettingDTO pageSettingDTO = getPageContentDTO();
        pageSettingDTO.setContent(text);
        String html = buildPage(pageSettingDTO);
        return screenshot(html);
    }

    /**
     * 短文本渲染。无边框和边距。
     * @param text
     * @return
     */
    public byte[] shortToImage(String text) {
        PageSettingDTO pageSettingDTO = getPageContentDTO();
        pageSettingDTO.setFontSize(80);
        pageSettingDTO.setBorder(0);
        pageSettingDTO.setPaddingTop(0);
        pageSettingDTO.setPaddingBottom(0);
        pageSettingDTO.setPaddingLeft(0);
        pageSettingDTO.setPaddingRight(0);
        pageSettingDTO.setContent(text);
        String html = buildPage(pageSettingDTO);
        return screenshot(html);
    }

    /**
     * 截图
     * @param html 页面的 HTML 代码
     * @return
     */
    private synchronized byte[] screenshot(String html) {
        openPage(html);
        ElementHandle show = this.page.locator("#show").elementHandle();
        return show.screenshot();
    }

    /**
     * 根据 HTML 打开页面
     * @param html 页面的 HTML 代码
     */
    @SneakyThrows(IOException.class)
    private void openPage(String html) {
        if (this.page == null) {
            File pageFile = new File(eraConfig.getResources().getPath().getTemp(),
                    UUID.randomUUID() + ".html");
            Files.createDirectories(pageFile.getParentFile().toPath());
            Files.createFile(pageFile.toPath());
            FileWriter writer = new FileWriter(pageFile);
            writer.write(html);
            writer.close();
            pageFile.deleteOnExit();
            this.page = playwrightHandle.newPage();
            playwrightHandle.navigate(this.page, pageFile);
        } else {
            this.page.evaluate(String.format(
                    "document.open();document.write(\"%s\");document.close();",
                    html.replace("\"", "\\\"")));
        }
    }

    /**
     * 页面内容参数
     * @return
     */
    private PageSettingDTO getPageContentDTO() {
        PageSettingDTO pageSettingDTO = new PageSettingDTO();
        String fontsPath = eraConfig.getResources().getPath().getFonts();
        pageSettingDTO.setFontList(
                Arrays.stream(Objects.requireNonNull(new File(fontsPath).listFiles()))
                        .filter(file -> file.isFile()
                                && file.getName().matches(
                                        "^.+\\.(?:ttf|TTF|ttc|TTC|otf|OTF)$"))
                        .sorted()
                        .map(File::getAbsolutePath)
                        .toList()
                        .reversed());
        pageSettingDTO.setFontSize(60);
        pageSettingDTO.setMaxWidth(1080);
        pageSettingDTO.setPaddingTop(40);
        pageSettingDTO.setPaddingBottom(40);
        pageSettingDTO.setPaddingLeft(30);
        pageSettingDTO.setPaddingRight(25);
        pageSettingDTO.setBorder(3);
        pageSettingDTO.setBorderColor("#ffb8c6");
        int hour = LocalDateTime.now().getHour();
        if (hour >= 6 && hour <= 18) {
            pageSettingDTO.setBackgroundColor("#ffffff");
            pageSettingDTO.setColor("#000000");
        } else {
            pageSettingDTO.setBackgroundColor("#000000");
            pageSettingDTO.setColor("#ffffff");
        }
        return pageSettingDTO;
    }

    /**
     * 页面模板
     * @param page
     * @return
     */
    private String buildPage(PageSettingDTO page) {
        StringBuilder builder = new StringBuilder();
        builder.append("<head>");
        builder.append("<title>文字渲染-txt2png</title>");
        builder.append("<style>");
        // 字体
        for (String font : page.getFontList()) {
            builder.append(String.format("@font-face{font-family:'MyFont';src:url('%s');}", font));
        }
        builder.append("body{font-family:Arial,'MyFont';}");
        builder.append("#show{");
        // 宽度
        builder.append(String.format("max-width:%spx;", page.getMaxWidth()));
        // 边框
        builder.append(String.format("border:%spx solid %s;",
                page.getBorder(), page.getBorderColor()));
        // 边距
        builder.append(String.format("padding-top:%spx;", page.getPaddingTop()));
        builder.append(String.format("padding-right:%spx;", page.getPaddingRight()));
        builder.append(String.format("padding-bottom:%spx;", page.getPaddingBottom()));
        builder.append(String.format("padding-left:%spx;", page.getPaddingLeft()));
        // 字号
        builder.append(String.format("font-size:%s;", page.getFontSize()));
        // 颜色
        builder.append(String.format("color:%s;", page.getColor()));
        builder.append(String.format("background:%s;", page.getBackgroundColor()));
        builder.append("display:inline-block;");
        builder.append("}");
        builder.append("#content{");
        builder.append("display:flex;");
        builder.append("align-items:center;");
        builder.append("justify-content:center;");
        builder.append("white-space:pre-wrap;");
        builder.append("word-break:break-word;");
        builder.append("}");
        builder.append("</style>");
        builder.append("</head>");

        builder.append("<body>");
        builder.append("<div id=\"show\">");
        builder.append("<span id=\"content\">");

        String content = Entities.escape(page.getContent())
                .replace("\n", "<br>");
        builder.append(content);

        builder.append("</span>");
        builder.append("</div>");
        builder.append("</body>");
        return builder.toString();
    }
}
