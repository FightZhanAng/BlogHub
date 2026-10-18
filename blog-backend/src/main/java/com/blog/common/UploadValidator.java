package com.blog.common;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 上传文件校验工具。
 *
 * <p>之前只信 {@code MultipartFile#getContentType()}（客户端可任意伪造），头像上传甚至
 * 直接用原始文件名的扩展名落盘，可以把 .html / .svg 写到 /uploads 下同源执行。</p>
 *
 * <p>这里做三件事：① 扩展名白名单；② 拒绝 SVG（同源可执行脚本）；③ 用文件头魔数
 * 校验真实类型，与声明的 Content-Type 交叉验证。</p>
 */
public final class UploadValidator {

    private UploadValidator() {}

    /** 允许的图片扩展名（不含 svg：SVG 能被浏览器当作文档执行脚本） */
    public static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp");

    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;

    /** 从原始文件名提取并规范化扩展名，非法则返回 null */
    public static String safeExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        if (idx <= 0 || idx == filename.length() - 1) return null;
        String ext = filename.substring(idx + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : null;
    }

    /**
     * 校验图片内容：文件头魔数必须匹配 jpg/png/gif/webp/bmp 之一。
     *
     * @return 识别出的扩展名；无法识别返回 null
     */
    public static String detectImageExtension(byte[] header) {
        if (header == null || header.length < 4) return null;
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47
        if ((header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
            return "png";
        }
        // GIF: 47 49 46 38
        if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
            return "gif";
        }
        // BMP: 42 4D
        if (header[0] == 'B' && header[1] == 'M') {
            return "bmp";
        }
        // WEBP: RIFF....WEBP
        if (header.length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
