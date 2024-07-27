package com.shitlime.era.utils;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.springframework.lang.NonNull;

public class JsoupUtils {
    /**
     * 将 Jsoup 文档节点转换为保留换行的纯文本
     *
     * @param doc Jsoup 文档节点
     * @return 解析后的纯文本
     */
    public static String getPlainText(@NonNull Element doc) {
        PlainTextAccumulator accum = new PlainTextAccumulator();
        NodeTraversor.traverse(accum, doc);
        return accum.toString();
    }

    private static class PlainTextAccumulator implements NodeVisitor {
        private final StringBuilder accum = StringUtil.borrowBuilder();

        @Override
        public void head(@NonNull Node node, int i) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                accum.append(((TextNode) node).text());
            } else if (name.equals("li")) {
                accum.append("\n * ");
            } else if (name.equals("dt")) {
                accum.append("  ");
            } else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr")) {
                accum.append("\n");
            }
        }

        @Override
        public void tail(@NonNull Node node, int depth) {
            String name = node.nodeName();
            if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")) {
                accum.append("\n");
            } else if (name.equals("a")) {
                accum.append(String.format(" <%s>", node.absUrl("href")));
            }
        }

        @Override
        public String toString() {
            return StringUtil.releaseBuilder(accum);
        }
    }
}
