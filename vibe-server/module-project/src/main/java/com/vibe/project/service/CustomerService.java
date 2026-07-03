package com.vibe.project.service;

import com.vibe.common.result.PageResult;
import com.vibe.project.dto.CustomerDTO;
import com.vibe.project.dto.CustomerQueryDTO;
import com.vibe.project.vo.CustomerVO;

/**
 * 客户服务
 *
 * @author vibe
 */
public interface CustomerService {

    /**
     * 分页查询客户
     */
    PageResult<CustomerVO> page(CustomerQueryDTO query);

    /**
     * 新增客户
     */
    Long create(CustomerDTO dto);

    /**
     * 编辑客户
     */
    void update(CustomerDTO dto);

    /**
     * 删除客户
     */
    void delete(Long id);

    /**
     * 客户详情
     */
    CustomerVO getDetail(Long id);
}
