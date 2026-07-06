package com.vibe.integration.adapter.erp;

import com.vibe.integration.adapter.erp.dto.ErpCustomerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * ERP 客户主数据 Feign 客户端
 *
 * <p>对接 ERP 系统 {@code ${integration.erp.url}} 基础数据接口，拉取客户主数据。</p>
 *
 * <p>命名 {@code erp-customer}，{@code url} 通过 {@code integration.erp.url} 配置注入，
 * 不依赖服务发现（无 Spring Cloud LoadBalancer）。</p>
 *
 * @author vibe
 */
@FeignClient(name = "erp-customer", url = "${integration.erp.url}")
public interface ErpCustomerFeignClient {

    /**
     * 按 customerId 拉取单个客户主数据。
     *
     * @param customerId ERP 客户主键
     * @return 客户主数据
     */
    @GetMapping("/customers/{customerId}")
    ErpCustomerDTO getCustomerById(@PathVariable("customerId") Long customerId);

    /**
     * 拉取所有客户主数据（分页）。
     *
     * <p>当 {@code updatedAfter} 不为空时，仅返回 ERP 中 {@code lastModifiedAt} 大于该时间的客户。</p>
     *
     * @param updatedAfter 增量同步起始时间（ISO-8601 字符串，例如 {@code 2024-01-01T00:00:00}）
     * @param page         页码（1-based）
     * @param size         每页大小
     * @return 客户列表
     */
    @GetMapping("/customers")
    List<ErpCustomerDTO> listCustomers(@RequestParam(value = "updatedAfter", required = false) String updatedAfter,
                                       @RequestParam(value = "page", defaultValue = "1") Integer page,
                                       @RequestParam(value = "size", defaultValue = "100") Integer size);
}
