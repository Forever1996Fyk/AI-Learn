package com.forever1996Fyk.ai.springai.rag.splitter;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/2 00:24
 **/
public class ModalTextSplitter extends TextSplitter {
    private final int maxSize;
    private final int overlap;

    private static final Pattern IMAGE_PATTERN =
            Pattern.compile("<image\\b[^>]*>.*?</image>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final String SEPARATOR = "\n\n";

    public ModalTextSplitter(int maxSize, int overlap) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        if (overlap < 0) throw new IllegalArgumentException("overlap must be >= 0");
        if (overlap >= maxSize) throw new IllegalArgumentException("overlap must be < maxSize");

        this.maxSize = maxSize;
        this.overlap = overlap;
    }
    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();

        // 按图片切分为初步块
        List<String> rawBlocks = splitByImage(text);
        List<String> result = new ArrayList<>();

        for (int i = 0; i < rawBlocks.size(); i++) {
            String currentBlock = rawBlocks.get(i).trim();
            if (currentBlock.isEmpty()) continue;

            boolean containsImage = IMAGE_PATTERN.matcher(currentBlock).find();

            if (containsImage) {
                // 图片块：应用前后 overlap
                result.add(buildImageChunk(rawBlocks, i));

            } else if (currentBlock.length() > maxSize) {
                // 超长纯文本块：按 maxSize 切分，不重叠
                result.addAll(chunkPureTextNoOverlap(currentBlock));

            } else {
                // 短纯文本块：直接加入，不应用 overlap
                result.add(currentBlock);
            }
        }

        return result;
    }

    /**
     * 超长纯文本块按 maxSize 切分，不重叠
     */
    private List<String> chunkPureTextNoOverlap(String text) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + maxSize, length);
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
            start = end;
        }

        return chunks;
    }

    /**
     * 构建图片块 chunk，应用前后 overlap
     */
    private String buildImageChunk(List<String> blocks, int index) {
        StringBuilder chunkBuilder = new StringBuilder();
        String currentBlock = blocks.get(index).trim();

        // 前序 overlap
        if (index > 0) {
            String prevBlock = blocks.get(index - 1).trim();
            int start = Math.max(0, prevBlock.length() - overlap);
            String prevOverlap = prevBlock.substring(start);
            if (!prevOverlap.isEmpty())
                chunkBuilder.append(prevOverlap).append(SEPARATOR);
        }

        // 当前图片块
        chunkBuilder.append(currentBlock);

        // 后序 overlap
        if (index < blocks.size() - 1) {
            String nextBlock = blocks.get(index + 1).trim();
            int end = Math.min(overlap, nextBlock.length());
            String nextOverlap = nextBlock.substring(0, end);
            if (!nextOverlap.isEmpty())
                chunkBuilder.append(SEPARATOR).append(nextOverlap);
        }

        return chunkBuilder.toString().trim();
    }
    private List<String> splitByImage(String text) {
        List<String> blocks = new ArrayList<>();
        Matcher matcher = IMAGE_PATTERN.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            int imgStart = matcher.start();
            int imgEnd = matcher.end();

            if (imgStart > lastEnd) {
                blocks.add(text.substring(lastEnd, imgStart));
            }

            int afterImgEnd = findNextBreak(text, imgEnd);
            blocks.add(text.substring(imgStart, afterImgEnd));

            lastEnd = afterImgEnd;
        }

        if (lastEnd < text.length()) {
            blocks.add(text.substring(lastEnd));
        }

        return blocks;
    }

    /**
     * 查找下一个断点（优先 \n\n、\n，其次句末标点）
     */
    private int findNextBreak(String text, int start) {
        int p = indexOf(text, "\n\n", start);
        if (p != -1) return p + 2;

        p = indexOf(text, "\n", start);
        if (p != -1) return p + 1;

        int lookAheadLimit = Math.min(text.length(), start + 300);
        for (int i = start; i < lookAheadLimit; i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '.' || c == '！' || c == '!' || c == '？' || c == '?' || c == ';' || c == '；') {
                return i + 1;
            }
        }

        return text.length();
    }

    private int indexOf(String text, String sub, int from) {
        if (from < 0) from = 0;
        return text.indexOf(sub, from);
    }
}
