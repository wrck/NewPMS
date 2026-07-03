package com.vibe.device.service;

import com.vibe.common.result.PageResult;
import com.vibe.device.dto.DeviceModelDTO;
import com.vibe.device.dto.DeviceModelQueryDTO;
import com.vibe.device.vo.DeviceModelVO;

import java.util.List;

/**
 * 设备型号服务。
 *
 * @author vibe
 */
public interface DeviceModelService {

    /**
     * 分页查询型号。
     */
    PageResult<DeviceModelVO> page(DeviceModelQueryDTO query);

    /**
     * 按产品线/类别查询型号列表。
     */
    List<DeviceModelVO> list(String productLine, String category);

    /**
     * 查询全部型号（下拉选项）。
     */
    List<DeviceModelVO> listAll();

    /**
     * 新增型号。
     */
    Long create(DeviceModelDTO dto);

    /**
     * 编辑型号。
     */
    void update(DeviceModelDTO dto);

    /**
     * 删除型号。
     */
    void delete(Long id);

    /**
     * 型号详情。
     */
    DeviceModelVO getDetail(Long id);
}
