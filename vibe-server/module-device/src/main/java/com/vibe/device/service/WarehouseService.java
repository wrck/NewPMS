package com.vibe.device.service;

import com.vibe.common.result.PageResult;
import com.vibe.device.dto.WarehouseDTO;
import com.vibe.device.vo.WarehouseVO;

import java.util.List;

/**
 * 仓库服务。
 *
 * @author vibe
 */
public interface WarehouseService {

    /**
     * 分页查询仓库。
     */
    PageResult<WarehouseVO> page(Integer page, Integer size, String keyword, String region);

    /**
     * 查询全部仓库（下拉选项）。
     */
    List<WarehouseVO> listAll();

    /**
     * 新增仓库。
     */
    Long create(WarehouseDTO dto);

    /**
     * 编辑仓库。
     */
    void update(WarehouseDTO dto);

    /**
     * 删除仓库。
     */
    void delete(Long id);

    /**
     * 仓库详情。
     */
    WarehouseVO getDetail(Long id);
}
