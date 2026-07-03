package com.vibe.device.service;

import com.vibe.common.result.PageResult;
import com.vibe.device.dto.SparePartActionDTO;
import com.vibe.device.dto.SparePartDTO;
import com.vibe.device.vo.SparePartLogVO;
import com.vibe.device.vo.SparePartVO;

import java.util.List;

/**
 * 备件服务（入库/领用/归还/返修/台账）。
 *
 * @author vibe
 */
public interface SparePartService {

    /**
     * 分页查询备件台账。
     */
    PageResult<SparePartVO> page(Integer page, Integer size, String keyword, Long warehouseId, Long modelId);

    /**
     * 备件详情。
     */
    SparePartVO getDetail(Long id);

    /**
     * 新增备件。
     */
    Long create(SparePartDTO dto);

    /**
     * 编辑备件。
     */
    void update(SparePartDTO dto);

    /**
     * 删除备件。
     */
    void delete(Long id);

    /**
     * 备件操作（入库/领用/归还/返修），同步更新库存数量并记录流水。
     */
    void action(SparePartActionDTO dto);

    /**
     * 备件操作流水查询。
     */
    List<SparePartLogVO> logList(Long sparePartId, Long projectId, String actionType);
}
