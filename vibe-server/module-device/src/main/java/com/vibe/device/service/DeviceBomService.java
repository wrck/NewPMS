package com.vibe.device.service;

import com.vibe.device.dto.DeviceBomDTO;
import com.vibe.device.vo.DeviceBomVO;

import java.util.List;

/**
 * 项目设备清单（BOM）服务。
 *
 * @author vibe
 */
public interface DeviceBomService {

    /**
     * 查询项目 BOM 列表（含型号信息与进度数量）。
     */
    List<DeviceBomVO> listByProject(Long projectId);

    /**
     * BOM 详情。
     */
    DeviceBomVO getDetail(Long id);

    /**
     * 新增/维护 BOM 行（同项目同型号唯一，存在则更新计划数量）。
     */
    Long save(DeviceBomDTO dto);

    /**
     * 编辑 BOM 行（变更计划数量/备注）。
     */
    void update(DeviceBomDTO dto);

    /**
     * 删除 BOM 行。
     */
    void delete(Long id);

    /**
     * BOM 变更：增加/减少/替换型号（同项目同型号唯一）。
     *
     * @param projectId    项目ID
     * @param fromModelId  原型号ID（替换时必填，减少时可空）
     * @param toModelId    新型号ID（替换/增加时必填）
     * @param deltaQty     数量变化（正为增加，负为减少）
     * @param remark       备注
     */
    void changeBom(Long projectId, Long fromModelId, Long toModelId, int deltaQty, String remark);

    /**
     * 按 BOM 维度统计到货/安装/验收数量（按型号维度）。
     */
    List<DeviceBomVO> statProgress(Long projectId);
}
