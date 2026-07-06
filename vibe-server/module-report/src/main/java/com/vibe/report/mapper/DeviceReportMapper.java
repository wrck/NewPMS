package com.vibe.report.mapper;

import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.DeviceReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 设备域报表 Mapper
 *
 * <p>由 {@code ReportMapper} 拆分而来，承担设备相关聚合查询：
 * 设备状态/型号分布、库存状态、BOM 完成率、设备维度计数等。</p>
 *
 * <p>SQL 统一在 XML（{@code mapper/report/DeviceReportMapper.xml}）中维护。</p>
 *
 * @author vibe
 */
@Mapper
public interface DeviceReportMapper {

    /* ============ 设备统计聚合 ============ */

    /**
     * 按状态统计设备数
     *
     * @return 每个状态对应的设备数
     */
    List<ChartDataVO> countDevicesByStatus();

    /* ============ 环比/活跃/总数 ============ */

    /**
     * 统计指定日期之前的设备数
     */
    Long countDevicesBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 统计在网设备数（状态 ONLINE）
     */
    Long countOnlineDevices();

    /**
     * 统计设备总数
     */
    Long countAllDevices();

    /* ============ 业务报表（设备） ============ */

    /**
     * 设备报表 - 汇总指标
     */
    DeviceReportVO.Summary deviceReportSummary(@Param("productLine") String productLine);

    /**
     * 设备报表 - 按状态分组
     */
    List<DeviceReportVO.StatusDist> deviceReportByStatus(@Param("productLine") String productLine);

    /**
     * 设备报表 - 按产品线分组（关联 device_model）
     */
    List<DeviceReportVO.ProductLineDist> deviceReportByProductLine(@Param("productLine") String productLine);

    /**
     * 设备报表 - 各项目 BOM 完成率
     */
    List<DeviceReportVO.BomCompletion> deviceReportBomCompletion();

    /**
     * 设备报表 - 库存状态（按仓库汇总设备数）
     */
    List<DeviceReportVO.InventoryStatus> deviceReportInventory();
}
