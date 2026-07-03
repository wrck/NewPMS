package com.vibe.device.service;

import com.vibe.common.result.PageResult;
import com.vibe.device.bo.DeviceImportRow;
import com.vibe.device.dto.DeviceInstanceDTO;
import com.vibe.device.dto.DeviceInstanceQueryDTO;
import com.vibe.device.dto.DeviceStatusTransitionDTO;
import com.vibe.device.vo.DeviceImportResultVO;
import com.vibe.device.vo.DeviceInstanceDetailVO;
import com.vibe.device.vo.DeviceInstanceVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 设备实例服务。
 *
 * <p>负责 SN 唯一校验、单条录入、详情、编辑、搜索、Excel 批量导入、
 * 设备状态机管理及状态变更记录。</p>
 *
 * @author vibe
 */
public interface DeviceInstanceService {

    /**
     * 分页查询设备实例（含型号名/项目名/仓库名，受数据权限控制）。
     */
    PageResult<DeviceInstanceVO> page(DeviceInstanceQueryDTO query);

    /**
     * 设备详情（含状态轨迹与出入库历史）。
     */
    DeviceInstanceDetailVO getDetail(Long id);

    /**
     * 单条录入设备（SN 唯一校验，初始状态 IN_FACTORY）。
     */
    Long create(DeviceInstanceDTO dto);

    /**
     * 编辑设备信息。
     */
    void update(DeviceInstanceDTO dto);

    /**
     * 删除设备。
     */
    void delete(Long id);

    /**
     * Excel 批量导入设备（导入前校验 SN 重复，重复行跳过并输出错误清单）。
     */
    DeviceImportResultVO importDevices(MultipartFile file);

    /**
     * 设备状态流转（校验状态机合法性，记录 device_status_log）。
     */
    void transition(Long deviceId, DeviceStatusTransitionDTO dto);

    /**
     * 提供 Excel 导入模板行模型（供下载模板使用）。
     */
    Class<DeviceImportRow> importRowClass();
}
