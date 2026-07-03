package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.device.entity.DeviceModelEntity;
import com.vibe.device.vo.DeviceModelVO;
import com.vibe.device.vo.InventoryLedgerRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备型号 Mapper。
 *
 * @author vibe
 */
@Mapper
public interface DeviceModelMapper extends BaseMapper<DeviceModelEntity> {

    /**
     * 分页查询型号（含产品线/类别筛选）。
     */
    List<DeviceModelVO> selectModelList(@Param("keyword") String keyword,
                                        @Param("productLine") String productLine,
                                        @Param("category") String category);

    /**
     * 按 ID 查询型号详情。
     */
    DeviceModelVO selectVoById(@Param("id") Long id);

    /**
     * 库存台账聚合：各仓库各型号在库（IN_FACTORY）设备数量。
     *
     * <p>用于库存台账看板，按 warehouse_id + model_id 维度聚合 count。</p>
     */
    List<InventoryLedgerRow> selectInventoryLedger(@Param("warehouseId") Long warehouseId,
                                                   @Param("modelId") Long modelId);
}
