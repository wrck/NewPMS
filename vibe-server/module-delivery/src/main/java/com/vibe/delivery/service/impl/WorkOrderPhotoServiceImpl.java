package com.vibe.delivery.service.impl;

import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.delivery.bo.GpsLocation;
import com.vibe.delivery.constant.DeliveryConstant;
import com.vibe.delivery.dto.WorkOrderPhotoUploadDTO;
import com.vibe.delivery.entity.WorkOrderEntity;
import com.vibe.delivery.entity.WorkOrderPhotoEntity;
import com.vibe.delivery.mapper.WorkOrderMapper;
import com.vibe.delivery.mapper.WorkOrderPhotoMapper;
import com.vibe.delivery.service.WorkOrderPhotoService;
import com.vibe.delivery.vo.WorkOrderPhotoVO;
import com.vibe.utils.ImageUtils;
import com.vibe.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 工单施工照片服务实现
 *
 * <p>照片处理流程（防作弊）：</p>
 * <ol>
 *   <li>读取原图字节，按格式压缩（质量 0.85，长边 ≤ 2048px）</li>
 *   <li>生成缩略图（320×320）</li>
 *   <li>添加水印（时间 + GPS 坐标 + 上传人），写入右下角</li>
 *   <li>原图 + 缩略图上传到 MinIO（目录 work-order/{workOrderId}/photos）</li>
 *   <li>保存照片记录（含 GPS JSON、拍摄时间、上传人）到 work_order_photo</li>
 *   <li>工单 photo_count + 1</li>
 * </ol>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderPhotoServiceImpl implements WorkOrderPhotoService {

    private static final DateTimeFormatter WATERMARK_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WorkOrderPhotoMapper workOrderPhotoMapper;
    private final WorkOrderMapper workOrderMapper;
    private final ImageUtils imageUtils;
    private final MinioUtils minioUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderPhotoVO uploadPhoto(Long workOrderId, MultipartFile file, WorkOrderPhotoUploadDTO meta) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "照片文件不能为空");
        }
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw BusinessException.of(ResultCode.WORK_ORDER_NOT_FOUND);
        }

        byte[] originalBytes = readBytes(file);
        String format = resolveImageFormat(file);

        // 1. 压缩（质量 0.85，长边 ≤ 2048）
        byte[] compressed = compressWithLongEdgeLimit(originalBytes, format);

        // 2. 生成缩略图（320×320）
        byte[] thumbnail = imageUtils.thumbnail(compressed, format,
                DeliveryConstant.THUMBNAIL_WIDTH, DeliveryConstant.THUMBNAIL_HEIGHT);

        // 3. 添加水印（时间 + GPS + 上传人）
        GpsLocation gps = meta != null ? meta.getGps() : null;
        LocalDateTime takenTime = meta != null && meta.getTakenTime() != null
                ? meta.getTakenTime() : LocalDateTime.now();
        Long stepId = meta != null ? meta.getStepId() : null;
        byte[] watermarked = addWatermark(compressed, format, gps, takenTime);

        // 4. 上传到 MinIO
        String dir = String.format(DeliveryConstant.PHOTO_DIR_FORMAT, workOrderId);
        String originalName = file.getOriginalFilename();
        String suffix = "." + format;
        String photoObjectName = dir + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        String thumbObjectName = dir + "/thumb_" + UUID.randomUUID().toString().replace("-", "") + suffix;
        minioUtils.upload(watermarked, photoObjectName, contentTypeOf(format));
        minioUtils.upload(thumbnail, thumbObjectName, contentTypeOf(format));

        // 5. 保存照片记录
        WorkOrderPhotoEntity entity = new WorkOrderPhotoEntity();
        entity.setWorkOrderId(workOrderId);
        entity.setStepId(stepId);
        entity.setPhotoUrl(photoObjectName);
        entity.setThumbnailUrl(thumbObjectName);
        if (gps != null) {
            // 水印时间文本回填到 GPS BO，便于后续展示
            gps.setTimeText(takenTime.format(WATERMARK_TIME_FORMAT));
        }
        entity.setGps(gps);
        entity.setTakenTime(takenTime);
        entity.setUploadedBy(UserContextHolder.getUserId());
        workOrderPhotoMapper.insert(entity);

        // 6. 工单 photo_count + 1
        WorkOrderEntity update = new WorkOrderEntity();
        update.setId(workOrder.getId());
        update.setVersion(workOrder.getVersion());
        update.setPhotoCount((workOrder.getPhotoCount() == null ? 0 : workOrder.getPhotoCount()) + 1);
        workOrderMapper.updateById(update);

        log.info("[WorkOrderPhoto] 照片上传成功: workOrderId={}, photoId={}, stepId={}, size={}bytes",
                workOrderId, entity.getId(), stepId, watermarked.length);
        return toVo(entity, originalName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WorkOrderPhotoVO> uploadPhotos(Long workOrderId, MultipartFile[] files, List<WorkOrderPhotoUploadDTO> metas) {
        if (files == null || files.length == 0) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "照片文件列表不能为空");
        }
        List<WorkOrderPhotoVO> result = new ArrayList<>(files.length);
        for (int i = 0; i < files.length; i++) {
            WorkOrderPhotoUploadDTO meta = (metas != null && i < metas.size()) ? metas.get(i) : null;
            result.add(uploadPhoto(workOrderId, files[i], meta));
        }
        return result;
    }

    @Override
    public List<WorkOrderPhotoVO> listByWorkOrder(Long workOrderId) {
        List<WorkOrderPhotoVO> list = workOrderPhotoMapper.selectByWorkOrderId(workOrderId);
        fillPresignedUrls(list);
        return list;
    }

    @Override
    public List<WorkOrderPhotoVO> listByStep(Long stepId) {
        List<WorkOrderPhotoVO> list = workOrderPhotoMapper.selectByStepId(stepId);
        fillPresignedUrls(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePhoto(Long photoId) {
        WorkOrderPhotoEntity photo = workOrderPhotoMapper.selectById(photoId);
        if (photo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "照片不存在");
        }
        // 删除 MinIO 对象
        if (StringUtils.hasText(photo.getPhotoUrl())) {
            minioUtils.delete(photo.getPhotoUrl());
        }
        if (StringUtils.hasText(photo.getThumbnailUrl())) {
            minioUtils.delete(photo.getThumbnailUrl());
        }
        workOrderPhotoMapper.deleteById(photoId);
        // 工单 photo_count - 1
        WorkOrderEntity workOrder = workOrderMapper.selectById(photo.getWorkOrderId());
        if (workOrder != null) {
            WorkOrderEntity update = new WorkOrderEntity();
            update.setId(workOrder.getId());
            update.setVersion(workOrder.getVersion());
            int newCount = (workOrder.getPhotoCount() == null ? 1 : workOrder.getPhotoCount()) - 1;
            update.setPhotoCount(Math.max(0, newCount));
            workOrderMapper.updateById(update);
        }
    }

    /* ============ 私有方法 ============ */

    /**
     * 压缩图片并限制长边 ≤ 2048px。
     *
     * <p>先按 Thumbnailator 的 size 方法等比缩放长边到 2048，再以质量 0.85 输出。</p>
     */
    private byte[] compressWithLongEdgeLimit(byte[] bytes, String format) {
        int[] wh = imageUtils.getWidthAndHeight(bytes);
        int width = wh[0];
        int height = wh[1];
        byte[] scaled = bytes;
        // 长边超过上限时先等比缩放
        if (width > 0 && height > 0) {
            int longEdge = Math.max(width, height);
            if (longEdge > DeliveryConstant.PHOTO_MAX_LONG_EDGE) {
                // 利用 thumbnail 工具按长边缩放（size 取长边，短边按比例自动）
                // Thumbnailator.size 是限制最大宽高，等比缩放
                scaled = imageUtils.thumbnail(bytes, format,
                        DeliveryConstant.PHOTO_MAX_LONG_EDGE, DeliveryConstant.PHOTO_MAX_LONG_EDGE);
            }
        }
        return imageUtils.compress(scaled, format, DeliveryConstant.PHOTO_COMPRESS_QUALITY);
    }

    /**
     * 添加水印：时间 + GPS 坐标 + 上传人姓名
     *
     * <p>水印内容（右下角，多行）：</p>
     * <pre>
     * 2025-07-02 14:30:00
     * 经度:116.407400, 纬度:39.904200
     * 北京市朝阳区XX大厦
     * 上传人:张三
     * </pre>
     */
    private byte[] addWatermark(byte[] bytes, String format, GpsLocation gps, LocalDateTime takenTime) {
        String timeText = takenTime.format(WATERMARK_TIME_FORMAT);
        StringBuilder gpsText = new StringBuilder();
        if (gps != null && gps.getLatitude() != null && gps.getLongitude() != null) {
            gpsText.append("经度:").append(formatCoord(gps.getLongitude()))
                    .append(", 纬度:").append(formatCoord(gps.getLatitude()));
            if (StringUtils.hasText(gps.getAddress())) {
                gpsText.append("\n").append(gps.getAddress());
            }
        } else {
            gpsText.append("GPS 未获取");
        }
        // 上传人
        UserContext ctx = UserContextHolder.get();
        String uploader = (ctx != null && StringUtils.hasText(ctx.getRealName())) ? ctx.getRealName() : "未知";
        gpsText.append("\n上传人:").append(uploader);
        try {
            return imageUtils.addGpsWatermark(bytes, format, gpsText.toString(), timeText);
        } catch (Exception e) {
            log.warn("[WorkOrderPhoto] 水印添加失败，使用原图: {}", e.getMessage());
            return bytes;
        }
    }

    private String formatCoord(double coord) {
        return String.format(Locale.ROOT, "%.6f", coord);
    }

    /**
     * 解析图片格式（jpg/png/gif），默认 jpg
     */
    private String resolveImageFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("png")) {
                return "png";
            }
            if (ct.contains("gif")) {
                return "gif";
            }
            if (ct.contains("jpeg") || ct.contains("jpg")) {
                return "jpg";
            }
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                if ("png".equals(ext) || "gif".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)) {
                    return "jpg".equals(ext) || "jpeg".equals(ext) ? "jpg" : ext;
                }
            }
        }
        return "jpg";
    }

    private String contentTypeOf(String format) {
        return switch (format) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "image/jpeg";
        };
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_IO_ERROR, "读取照片文件失败: " + e.getMessage());
        }
    }

    /**
     * 为 VO 填充预签名 URL
     */
    private void fillPresignedUrls(List<WorkOrderPhotoVO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (WorkOrderPhotoVO vo : list) {
            if (StringUtils.hasText(vo.getPhotoUrl())) {
                try {
                    vo.setPhotoUrl(minioUtils.getPresignedDownloadUrl(vo.getPhotoUrl()));
                } catch (Exception e) {
                    log.warn("生成照片预签名 URL 失败: {}", e.getMessage());
                }
            }
            if (StringUtils.hasText(vo.getThumbnailUrl())) {
                try {
                    vo.setThumbnailUrl(minioUtils.getPresignedDownloadUrl(vo.getThumbnailUrl()));
                } catch (Exception e) {
                    log.warn("生成缩略图预签名 URL 失败: {}", e.getMessage());
                }
            }
        }
    }

    private WorkOrderPhotoVO toVo(WorkOrderPhotoEntity entity, String originalName) {
        WorkOrderPhotoVO vo = new WorkOrderPhotoVO();
        vo.setId(entity.getId());
        vo.setWorkOrderId(entity.getWorkOrderId());
        vo.setStepId(entity.getStepId());
        vo.setPhotoUrl(entity.getPhotoUrl());
        vo.setThumbnailUrl(entity.getThumbnailUrl());
        vo.setGps(entity.getGps());
        vo.setTakenTime(entity.getTakenTime());
        vo.setUploadedBy(entity.getUploadedBy());
        vo.setCreateTime(entity.getCreateTime());
        // 预签名 URL
        if (StringUtils.hasText(vo.getPhotoUrl())) {
            try {
                vo.setPhotoUrl(minioUtils.getPresignedDownloadUrl(vo.getPhotoUrl()));
            } catch (Exception ignored) {
            }
        }
        if (StringUtils.hasText(vo.getThumbnailUrl())) {
            try {
                vo.setThumbnailUrl(minioUtils.getPresignedDownloadUrl(vo.getThumbnailUrl()));
            } catch (Exception ignored) {
            }
        }
        return vo;
    }
}
