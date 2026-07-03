package com.vibe.delivery.controller;

import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.delivery.dto.WorkOrderPhotoUploadDTO;
import com.vibe.delivery.service.WorkOrderPhotoService;
import com.vibe.delivery.vo.WorkOrderPhotoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 工单施工照片 Controller
 *
 * <p>路径：{@code /api/v1/work-orders/{workOrderId}/photos}</p>
 *
 * <p>上传流程（防作弊）：服务端接收文件 → 压缩（0.85 质量、长边≤2048）→ 生成缩略图 →
 * 添加水印（时间+GPS+上传人）→ 上传 MinIO → 保存记录。</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "施工照片", description = "施工照片上传/查询（带时间+GPS 水印）")
@RestController
@RequestMapping("/api/v1/work-orders/{workOrderId}/photos")
@RequiredArgsConstructor
public class WorkOrderPhotoController {

    private final WorkOrderPhotoService workOrderPhotoService;

    @Operation(summary = "上传单张施工照片")
    @OperationLog(module = "交付管理", type = "INSERT", description = "上传施工照片", saveRequest = false)
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping(consumes = "multipart/form-data")
    public Result<WorkOrderPhotoVO> upload(@PathVariable Long workOrderId,
                                           @RequestPart("file") MultipartFile file,
                                           @RequestParam(value = "stepId", required = false) Long stepId,
                                           @RequestParam(value = "longitude", required = false) Double longitude,
                                           @RequestParam(value = "latitude", required = false) Double latitude,
                                           @RequestParam(value = "address", required = false) String address,
                                           @RequestParam(value = "takenTime", required = false)
                                           java.time.LocalDateTime takenTime) {
        WorkOrderPhotoUploadDTO meta = buildMeta(stepId, longitude, latitude, address, takenTime);
        return Result.success(workOrderPhotoService.uploadPhoto(workOrderId, file, meta));
    }

    @Operation(summary = "批量上传施工照片")
    @OperationLog(module = "交付管理", type = "INSERT", description = "批量上传施工照片", saveRequest = false)
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping(value = "/batch", consumes = "multipart/form-data")
    public Result<List<WorkOrderPhotoVO>> uploadBatch(@PathVariable Long workOrderId,
                                                      @RequestPart("files") MultipartFile[] files,
                                                      @RequestParam(value = "stepId", required = false) Long stepId,
                                                      @RequestParam(value = "longitude", required = false) Double longitude,
                                                      @RequestParam(value = "latitude", required = false) Double latitude,
                                                      @RequestParam(value = "address", required = false) String address,
                                                      @RequestParam(value = "takenTime", required = false)
                                                      java.time.LocalDateTime takenTime) {
        // 批量上传：所有照片共用同一组元数据（同一步骤、同一 GPS 定位）
        List<WorkOrderPhotoUploadDTO> metas = new ArrayList<>(files.length);
        for (int i = 0; i < files.length; i++) {
            metas.add(buildMeta(stepId, longitude, latitude, address, takenTime));
        }
        return Result.success(workOrderPhotoService.uploadPhotos(workOrderId, files, metas));
    }

    @Operation(summary = "查询工单照片列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER','AGENT_ENGINEER')")
    @GetMapping
    public Result<List<WorkOrderPhotoVO>> list(@PathVariable Long workOrderId) {
        return Result.success(workOrderPhotoService.listByWorkOrder(workOrderId));
    }

    @Operation(summary = "按步骤查询照片列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER','AGENT_ENGINEER')")
    @GetMapping("/step/{stepId}")
    public Result<List<WorkOrderPhotoVO>> listByStep(@PathVariable Long workOrderId,
                                                     @PathVariable Long stepId) {
        return Result.success(workOrderPhotoService.listByStep(stepId));
    }

    @Operation(summary = "删除照片")
    @OperationLog(module = "交付管理", type = "DELETE", description = "删除施工照片")
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @DeleteMapping("/{photoId}")
    public Result<Void> delete(@PathVariable Long workOrderId, @PathVariable Long photoId) {
        workOrderPhotoService.deletePhoto(photoId);
        return Result.success();
    }

    /* ============ 私有方法 ============ */

    private WorkOrderPhotoUploadDTO buildMeta(Long stepId, Double longitude, Double latitude,
                                              String address, java.time.LocalDateTime takenTime) {
        WorkOrderPhotoUploadDTO meta = new WorkOrderPhotoUploadDTO();
        meta.setStepId(stepId);
        if (longitude != null || latitude != null) {
            com.vibe.delivery.bo.GpsLocation gps = new com.vibe.delivery.bo.GpsLocation();
            gps.setLongitude(longitude);
            gps.setLatitude(latitude);
            gps.setAddress(address);
            meta.setGps(gps);
        }
        meta.setTakenTime(takenTime);
        return meta;
    }
}
