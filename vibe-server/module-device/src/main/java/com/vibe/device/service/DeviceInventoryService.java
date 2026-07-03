package com.vibe.device.service;

import com.vibe.device.dto.DeviceInventoryActionDTO;
import com.vibe.device.vo.DeviceInventoryLogVO;
import com.vibe.device.vo.InventoryLedgerRow;
import com.vibe.device.vo.InventoryWarningVO;

import java.util.List;

/**
 * 设备出入库与库存服务。
 *
 * <p>负责设备入库/出库/退库/调拨操作（同步更新设备归属与状态并记录流水）、
 * 库存台账聚合查询、库存预警（基于 warehouse.safety_stock JSON 配置比对）。</p>
 *
 * @author vibe
 */
public interface DeviceInventoryService {

    /**
     * 设备出入库操作（入库/出库/退库/调拨）。
     *
     * <p>根据操作类型同步更新 device_instance 的仓库/项目归属，并写入 device_inventory_log。</p>
     *
     * @param dto 操作参数
     */
    void action(DeviceInventoryActionDTO dto);

    /**
     * 出入库流水查询（按仓库/项目/操作类型筛选）。
     */
    List<DeviceInventoryLogVO> logList(Long warehouseId, Long projectId, String actionType);

    /**
     * 库存台账聚合查询（各仓库各型号在库 IN_FACTORY 设备数量）。
     *
     * @param warehouseId 仓库ID（可空，空表示全部仓库）
     * @param modelId     型号ID（可空，空表示全部型号）
     */
    List<InventoryLedgerRow> ledger(Long warehouseId, Long modelId);

    /**
     * 库存预警：扫描所有仓库，比对在库数量与 safety_stock 配置，
     * 返回在库数量低于安全库存阈值的清单。
     */
    List<InventoryWarningVO> warnings();
}
