package com.vibe.utils;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片处理工具类
 *
 * <p>基于 Thumbnailator 提供图片压缩、缩略图、水印能力。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
public class ImageUtils {

    /** 默认压缩质量（0-1） */
    private static final float DEFAULT_QUALITY = 0.75f;
    /** 默认缩略图宽度 */
    private static final int DEFAULT_THUMB_WIDTH = 200;
    /** 默认缩略图高度 */
    private static final int DEFAULT_THUMB_HEIGHT = 200;

    /**
     * 压缩图片（保持原尺寸，降低质量）
     *
     * @param bytes     原图字节
     * @param format    格式：jpg/png/gif
     * @param quality   质量 0-1
     */
    public byte[] compress(byte[] bytes, String format, float quality) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .scale(1.0)
                    .outputQuality(quality)
                    .outputFormat(format)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("[ImageUtils] 图片压缩失败", e);
            throw new BusinessException(ResultCode.FILE_IO_ERROR, "图片压缩失败: " + e.getMessage());
        }
    }

    /**
     * 压缩图片（默认质量）
     */
    public byte[] compress(byte[] bytes, String format) {
        return compress(bytes, format, DEFAULT_QUALITY);
    }

    /**
     * 生成缩略图（按指定宽高等比缩放并裁剪）
     */
    public byte[] thumbnail(byte[] bytes, String format, int width, int height) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .size(width, height)
                    .keepAspectRatio(true)
                    .outputFormat(format)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("[ImageUtils] 缩略图生成失败", e);
            throw new BusinessException(ResultCode.FILE_IO_ERROR, "缩略图生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成默认缩略图（200x200）
     */
    public byte[] thumbnail(byte[] bytes, String format) {
        return thumbnail(bytes, format, DEFAULT_THUMB_WIDTH, DEFAULT_THUMB_HEIGHT);
    }

    /**
     * 添加文字水印（右下角）
     *
     * @param bytes   原图字节
     * @param format  格式
     * @param text    水印文字
     */
    public byte[] addTextWatermark(byte[] bytes, String format, String text) {
        return addTextWatermark(bytes, format, text, Positions.BOTTOM_RIGHT, Color.WHITE, 0.7f, 18);
    }

    /**
     * 添加文字水印（自定义位置/颜色/透明度/字号）
     */
    public byte[] addTextWatermark(byte[] bytes, String format, String text,
                                   Positions position, Color color, float alpha, int fontSize) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage watermarkImage = createTextWatermarkImage(text, color, alpha, fontSize);
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .scale(1.0)
                    .watermark(position, watermarkImage, alpha)
                    .outputFormat(format)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("[ImageUtils] 水印添加失败", e);
            throw new BusinessException(ResultCode.FILE_IO_ERROR, "水印添加失败: " + e.getMessage());
        }
    }

    /**
     * 添加时间+GPS 水印（用于施工现场照片防作弊）
     *
     * @param bytes   原图字节
     * @param format  格式
     * @param gpsText GPS 文本，如 "经度:116.40, 纬度:39.90"
     * @param time    时间文本，如 "2025-07-02 14:30:00"
     */
    public byte[] addGpsWatermark(byte[] bytes, String format, String gpsText, String time) {
        String watermarkText = time + "\n" + gpsText;
        return addTextWatermark(bytes, format, watermarkText, Positions.BOTTOM_RIGHT,
                new Color(255, 255, 255, 220), 0.85f, 16);
    }

    /**
     * 获取图片宽高
     */
    public int[] getWidthAndHeight(byte[] bytes) {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                return new int[]{0, 0};
            }
            return new int[]{image.getWidth(), image.getHeight()};
        } catch (IOException e) {
            log.warn("[ImageUtils] 读取图片尺寸失败: {}", e.getMessage());
            return new int[]{0, 0};
        }
    }

    /* ============ 内部方法 ============ */

    private BufferedImage createTextWatermarkImage(String text, Color color, float alpha, int fontSize) {
        String[] lines = text.split("\n");
        int lineCount = lines.length;
        int width = fontSize * (text.lines().mapToInt(String::length).max().orElse(10));
        int height = (fontSize + 4) * lineCount + 10;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
            g2d.setFont(new Font("Microsoft YaHei", Font.PLAIN, fontSize));
            FontMetrics fm = g2d.getFontMetrics();
            int y = fm.getAscent() + 2;
            for (String line : lines) {
                g2d.drawString(line, 4, y);
                y += fm.getHeight();
            }
        } finally {
            g2d.dispose();
        }
        return image;
    }
}
