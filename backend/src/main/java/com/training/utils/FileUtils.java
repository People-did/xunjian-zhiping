package com.training.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtils {

    // 文档类型
    private static final Set<String> DOC_TYPES = Set.of("doc", "docx", "pdf", "txt", "md", "rst", "tex");
    // 代码类型
    private static final Set<String> CODE_TYPES = Set.of(
            "py", "java", "js", "ts", "jsx", "tsx", "html", "htm", "css", "scss", "less",
            "sql", "c", "cpp", "h", "hpp", "go", "rs", "rb", "php", "swift", "kt", "scala",
            "r", "m", "pl", "sh", "bat", "ps1", "json", "xml", "yaml", "yml", "toml", "ini",
            "cfg", "conf", "properties", "gradle", "makefile", "dockerfile"
    );
    // 图片类型
    private static final Set<String> IMAGE_TYPES = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico"
    );
    // 压缩包类型
    private static final Set<String> ARCHIVE_TYPES = Set.of("zip");

    public static String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public static String generateUniqueFilename(String originalFilename) {
        String ext = getFileExtension(originalFilename);
        return UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }

    /**
     * 检查文件类型是否在允许列表中
     */
    public static boolean isAllowedFileType(String extension) {
        if (extension == null || extension.isEmpty()) return false;
        return DOC_TYPES.contains(extension)
                || CODE_TYPES.contains(extension)
                || IMAGE_TYPES.contains(extension)
                || ARCHIVE_TYPES.contains(extension);
    }

    /**
     * 判断文件是否为文档类型（可提取文本内容）
     */
    public static boolean isDocumentType(String extension) {
        return DOC_TYPES.contains(extension);
    }

    /**
     * 判断文件是否为代码类型
     */
    public static boolean isCodeType(String extension) {
        return CODE_TYPES.contains(extension);
    }

    /**
     * 判断文件是否为图片类型
     */
    public static boolean isImageType(String extension) {
        return IMAGE_TYPES.contains(extension);
    }

    /**
     * 判断文件是否为压缩包类型
     */
    public static boolean isArchiveType(String extension) {
        return ARCHIVE_TYPES.contains(extension);
    }

    /**
     * 获取文件类型分类标签
     */
    public static String getFileCategoryLabel(String extension) {
        if (isDocumentType(extension)) return "文档";
        if (isCodeType(extension)) return "代码";
        if (isImageType(extension)) return "截图";
        if (isArchiveType(extension)) return "压缩包";
        return "其他";
    }

    // ==================== 文本提取 ====================

    /**
     * 从 MultipartFile 提取文本内容（根据扩展名自动分发）
     */
    public static String extractTextFromFile(MultipartFile file) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());

        if (isDocumentType(extension)) {
            return extractDocumentText(file.getInputStream(), extension);
        }
        if (isCodeType(extension)) {
            return extractCodeText(file.getInputStream());
        }
        // 图片和压缩包不提取纯文本，由上层调用 AI 视觉或解压处理
        if (isImageType(extension)) {
            return "[图片文件: " + file.getOriginalFilename() + "，需通过AI视觉分析]";
        }
        if (isArchiveType(extension)) {
            return "[压缩包文件: " + file.getOriginalFilename() + "，需解压后解析]";
        }
        throw new IOException("不支持的文件类型: " + extension);
    }

    /**
     * 从 File 提取文本内容（根据扩展名自动分发）
     */
    public static String extractTextFromFile(File file) throws IOException {
        String extension = getFileExtension(file.getName());

        if (isDocumentType(extension)) {
            return extractDocumentText(new FileInputStream(file), extension);
        }
        if (isCodeType(extension)) {
            return extractCodeText(new FileInputStream(file));
        }
        if (isImageType(extension)) {
            return "[图片文件: " + file.getName() + "，需通过AI视觉分析]";
        }
        if (isArchiveType(extension)) {
            return extractZipFileSummary(file);
        }
        throw new IOException("不支持的文件类型: " + extension);
    }

    /**
     * 文档文本提取统一入口
     */
    private static String extractDocumentText(InputStream inputStream, String extension) throws IOException {
        return switch (extension) {
            case "doc" -> extractTextFromDoc(inputStream);
            case "docx" -> extractTextFromDocx(inputStream);
            case "pdf" -> extractTextFromPdf(inputStream);
            case "txt", "md", "rst", "tex" -> extractTextFromPlainText(inputStream);
            default -> throw new IOException("不支持的文档类型: " + extension);
        };
    }

    /**
     * 纯文本/TXT/MD 文件读取
     */
    public static String extractTextFromPlainText(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 代码文件读取（作为纯文本处理）
     */
    public static String extractCodeText(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null && lineNumber < 2000) {
                sb.append(line).append("\n");
                lineNumber++;
            }
            if (lineNumber >= 2000) {
                sb.append("\n... (文件过长，已截断至前2000行)");
            }
        }
        return sb.toString();
    }

    /**
     * 获取代码文件的统计信息
     */
    public static Map<String, Object> getCodeFileStats(InputStream inputStream, String fileName) throws IOException {
        int totalLines = 0;
        int commentLines = 0;
        int blankLines = 0;
        int codeLines = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean inBlockComment = false;

            while ((line = reader.readLine()) != null) {
                totalLines++;
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    blankLines++;
                    continue;
                }

                // 块注释检测
                if (inBlockComment) {
                    commentLines++;
                    if (trimmed.contains("*/") || trimmed.contains("\"\"\"")) {
                        inBlockComment = false;
                    }
                    continue;
                }

                if (trimmed.startsWith("/*") || trimmed.startsWith("/**") || trimmed.startsWith("\"\"\"")) {
                    commentLines++;
                    if (!trimmed.contains("*/") && !trimmed.endsWith("\"\"\"")) {
                        inBlockComment = true;
                    }
                    continue;
                }

                if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("--")) {
                    commentLines++;
                    continue;
                }

                codeLines++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("fileName", fileName);
        stats.put("totalLines", totalLines);
        stats.put("codeLines", codeLines);
        stats.put("commentLines", commentLines);
        stats.put("blankLines", blankLines);
        stats.put("commentRate", totalLines > 0 ? Math.round(commentLines * 100.0 / totalLines) : 0);
        return stats;
    }

    // ==================== ZIP 压缩包处理 ====================

    /**
     * 解压 ZIP 并返回文件树摘要（纯 Java 实现，前 3 层）
     */
    public static String extractZipFileSummary(File zipFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 ZIP 压缩包内容摘要\n");
        sb.append("文件名: ").append(zipFile.getName()).append("\n");
        sb.append("大小: ").append(formatFileSize(zipFile.length())).append("\n\n");

        Map<String, List<String>> tree = new TreeMap<>();
        Map<String, Integer> typeCount = new LinkedHashMap<>();
        int totalFiles = 0;

        try (ZipFile zf = new ZipFile(zipFile, StandardCharsets.UTF_8)) {
            // 先尝试 UTF-8，失败则尝试 GBK
            processZipEntries(zf, tree, typeCount);
            totalFiles = tree.size();
        } catch (Exception e) {
            // 尝试 GBK 编码
            try (ZipFile zf = new ZipFile(zipFile, java.nio.charset.Charset.forName("GBK"))) {
                processZipEntries(zf, tree, typeCount);
                totalFiles = tree.size();
            }
        }

        sb.append("文件总数: ").append(totalFiles).append("\n");
        sb.append("文件类型分布:\n");
        typeCount.forEach((type, count) -> sb.append("  ").append(type).append(": ").append(count).append(" 个\n"));

        sb.append("\n📂 文件树结构（前3层）:\n");
        Map<String, Object> rootTree = buildFileTree(tree, 3);
        appendTree(sb, rootTree, "");

        return sb.toString();
    }

    private static void processZipEntries(ZipFile zf, Map<String, List<String>> tree,
                                           Map<String, Integer> typeCount) {
        var entries = zf.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            String ext = getFileExtension(name);
            String category = getFileCategoryLabel(ext);
            typeCount.merge(category, 1, Integer::sum);
            tree.put(name, List.of(category, String.valueOf(entry.getSize())));
        }
    }

    private static Map<String, Object> buildFileTree(Map<String, List<String>> flatFiles, int maxDepth) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (var entry : flatFiles.entrySet()) {
            String path = entry.getKey();
            List<String> info = entry.getValue();
            String[] parts = path.replace('\\', '/').split("/");
            if (parts.length > maxDepth) {
                // 深层文件归入 "..." 节点
                root.compute("... (深层文件)", (k, v) -> {
                    @SuppressWarnings("unchecked")
                    List<String> list = (v instanceof List) ? (List<String>) v : new ArrayList<>();
                    list.add(parts[parts.length - 1]);
                    return list;
                });
                continue;
            }
            navigateAndAdd(root, parts, 0, info);
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static void navigateAndAdd(Map<String, Object> node, String[] parts, int idx, List<String> info) {
        if (idx >= parts.length) return;
        String key = parts[idx];
        if (idx == parts.length - 1) {
            // 叶子节点：文件
            node.put(key + " (" + info.get(0) + ", " + formatFileSize(Long.parseLong(info.get(1))) + ")", null);
        } else {
            // 目录节点
            node.computeIfAbsent(key + "/", k -> new LinkedHashMap<>());
            navigateAndAdd((Map<String, Object>) node.get(key + "/"), parts, idx + 1, info);
        }
    }

    private static void appendTree(StringBuilder sb, Map<String, Object> node, String prefix) {
        List<String> keys = new ArrayList<>(node.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            boolean isLast = (i == keys.size() - 1);
            String connector = isLast ? "└── " : "├── ";
            sb.append(prefix).append(connector).append(key).append("\n");

            Object value = node.get(key);
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> child = (Map<String, Object>) value;
                appendTree(sb, child, prefix + (isLast ? "    " : "│   "));
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                sb.append(prefix).append(isLast ? "    " : "│   ")
                        .append("└── ... 共 ").append(list.size()).append(" 个文件\n");
            }
        }
    }

    // ==================== 原有文档提取方法 ====================

    private static String extractTextFromDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                sb.append(paragraph.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    private static String extractTextFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    // ==================== 文件保存与删除 ====================

    public static void saveFile(MultipartFile file, String uploadPath, String filename) throws IOException {
        Path path = Paths.get(uploadPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        file.transferTo(path.resolve(filename));
    }

    public static boolean deleteFile(String filePath) {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
    }
}
