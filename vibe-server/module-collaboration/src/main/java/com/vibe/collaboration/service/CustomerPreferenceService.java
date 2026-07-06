package com.vibe.collaboration.service;

import com.vibe.collaboration.dto.CustomerPreferenceDTO;
import com.vibe.collaboration.vo.CustomerPreferenceVO;

import java.util.List;

/**
 * 客户偏好服务
 *
 * <p>提供客户偏好的 CRUD 基础方法与按客户/键查询的业务方法。</p>
 *
 * @author vibe
 */
public interface CustomerPreferenceService {

    /* ============ CRUD 基础方法 ============ */

    /**
     * 查询偏好详情。
     */
    CustomerPreferenceVO getDetail(Long id);

    /**
     * 新增偏好。
     */
    Long create(CustomerPreferenceDTO dto);

    /**
     * 更新偏好。
     */
    void update(CustomerPreferenceDTO dto);

    /**
     * 删除偏好。
     */
    void delete(Long id);

    /* ============ 业务方法 ============ */

    /**
     * 查询指定客户的全部偏好。
     */
    List<CustomerPreferenceVO> listByCustomerId(Long customerId);

    /**
     * 按客户ID + 偏好键查询偏好。
     */
    CustomerPreferenceVO getByCustomerIdAndKey(Long customerId, String preferenceKey);

    /**
     * 批量保存指定客户的偏好（upsert：存在则更新，不存在则新增）。
     *
     * @param customerId 客户ID
     * @param dtos       偏好列表（customerId 由本方法统一填充）
     */
    void updatePreferences(Long customerId, List<CustomerPreferenceDTO> dtos);
}
