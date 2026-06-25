package com.atguigu.llmcallback.Util;

import java.util.List;


public class TextUtil {
    // 最小英文 tokenizer：去掉标点，按空白切
    public static List<String> tokenize(String text) {
        if (text == null) return java.util.Collections.emptyList();
        String t = text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ");
        return java.util.Arrays.stream(t.split("\\s+"))
                .filter(w -> w.length() >= 2)
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
    }
}
