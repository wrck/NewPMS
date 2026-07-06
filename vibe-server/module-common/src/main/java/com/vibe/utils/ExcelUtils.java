package com.vibe.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * EasyExcel 工具类
 *
 * <p>封装 EasyExcel 常用读写操作。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
public class ExcelUtils {

    /**
     * 从输入流读取 Excel
     *
     * @param is        输入流
     * @param clazz     数据模型类
     * @param listener  行读取监听器
     */
    public <T> void read(InputStream is, Class<T> clazz, ReadListener<T> listener) {
        try {
            EasyExcel.read(is, clazz, listener).sheet().doRead();
        } catch (Exception e) {
            log.error("[ExcelUtils] Excel 读取失败", e);
            throw new BusinessException(ResultCode.FILE_IO_ERROR, "Excel 解析失败: " + e.getMessage());
        }
    }

    /**
     * 写 Excel 到输出流
     *
     * @param os        输出流
     * @param clazz     数据模型类
     * @param sheetName Sheet 名称
     * @param data      数据列表
     */
    public <T> void write(OutputStream os, Class<T> clazz, String sheetName, List<T> data) {
        ExcelWriter writer = null;
        try {
            writer = EasyExcel.write(os, clazz).build();
            WriteSheet sheet = EasyExcel.writerSheet(sheetName).build();
            writer.write(data, sheet);
        } catch (Exception e) {
            log.error("[ExcelUtils] Excel 写出失败", e);
            throw new BusinessException(ResultCode.FILE_IO_ERROR, "Excel 写出失败: " + e.getMessage());
        } finally {
            if (writer != null) {
                writer.finish();
            }
        }
    }

    /**
     * 浏览器下载 Excel
     *
     * @param response  HttpServletResponse
     * @param fileName 下载文件名（不含扩展名）
     * @param clazz     数据模型类
     * @param sheetName Sheet 名称
     * @param data      数据列表
     */
    public <T> void download(HttpServletResponse response, String fileName, Class<T> clazz,
                             String sheetName, List<T> data) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encodedName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedName);
            EasyExcel.write(response.getOutputStream(), clazz).sheet(sheetName).doWrite(data);
        } catch (IOException e) {
            log.error("[ExcelUtils] Excel 下载失败", e);
            throw new BusinessException(ResultCode.FILE_IO_ERROR, "Excel 下载失败: " + e.getMessage());
        }
    }

    /**
     * 通用 Excel 导出（静态方法，自动适配列宽）。
     *
     * <p>设置响应头为 xlsx 类型，文件名采用 RFC 5987 编码（filename*=utf-8''）
     * 以兼容中文文件名；同时注册 {@link LongestMatchColumnWidthStyleStrategy}
     * 根据表头与内容长度自动调整列宽。</p>
     *
     * @param response   HttpServletResponse
     * @param fileName   下载文件名（不含扩展名，自动追加 .xlsx）
     * @param sheetName  Sheet 名称
     * @param headClazz  数据模型类（标注 @ExcelProperty 的 DTO）
     * @param data       数据列表
     * @param <T>        数据模型类型
     * @throws IOException 写出失败时抛出
     */
    public static <T> void export(HttpServletResponse response, String fileName, String sheetName,
                                  Class<T> headClazz, List<T> data) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), headClazz)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(sheetName)
                .doWrite(data);
    }
}
