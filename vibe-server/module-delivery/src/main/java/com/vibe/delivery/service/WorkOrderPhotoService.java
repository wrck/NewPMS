package com.vibe.delivery.service;

import com.vibe.delivery.dto.WorkOrderPhotoUploadDTO;
import com.vibe.delivery.vo.WorkOrderPhotoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 工单施工照片服务
 *
 * <p>核心能力：照片上传（压缩+缩略图+水印+MinIO）/ 列表查询 / 按步骤归类。</p>
 *
 * @author vibe
 */
public interface WorkOrderPhotoService {

    /**
     * 上传单张施工照片：
     * <ol>
     *   <li>调用 ImageUtils 压缩（质量 0.85，长边 ≤ 2048px）</li>
     *   <li>生成缩略图（320×320）</li>
     *   <li>添加水印（时间 + GPS 坐标 + 上传人）</li>
     *   <li>上传原图 + 缩略图到 MinIO</li>
     *   <li>保存照片记录到 work_order_photo（含 GPS JSON + 拍摄时间）</li>
     *   <li>工单 photo_count + 1</li>
     * </ol>
     *
     * @param workOrderId 工单ID
     * @param file        照片文件
     * @param meta        元数据（GPS / 拍摄时间 / 步骤ID）
     * @return 照片视图
     */
    WorkOrderPhotoVO uploadPhoto(Long workOrderId, MultipartFile file, WorkOrderPhotoUploadDTO meta);

    /**
     * 批量上传施工照片
     *
     * @param workOrderId 工单ID
     * @param files       照片文件列表
     * @param metas       元数据列表（与 files 一一对应，长度可小于 files，缺失项用默认）
     * @return 照片视图列表
     */
    List<WorkOrderPhotoVO> uploadPhotos(Long workOrderId, MultipartFile[] files, List<WorkOrderPhotoUploadDTO> metas);

    /**
     * 按工单查询照片列表
     */
    List<WorkOrderPhotoVO> listByWorkOrder(Long workOrderId);

    /**
     * 按步骤查询照片列表
     */
    List<WorkOrderPhotoVO> listByStep(Long stepId);

    /**
     * 删除照片（同步删除 MinIO 对象）
     */
    void deletePhoto(Long photoId);
}
