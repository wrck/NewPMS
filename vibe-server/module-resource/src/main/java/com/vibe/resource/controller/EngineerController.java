package com.vibe.resource.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.resource.constant.ResourceConstant;
import com.vibe.resource.dto.EngineerDTO;
import com.vibe.resource.dto.EngineerQueryDTO;
import com.vibe.resource.dto.EngineerSkillDTO;
import com.vibe.resource.service.EngineerService;
import com.vibe.resource.vo.EngineerSkillVO;
import com.vibe.resource.vo.EngineerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工程师资源池 Controller
 *
 * <p>路径前缀 {@code /api/v1/engineers}。角色控制：SUPER_ADMIN / DISPATCHER 可写，
 * ENGINEER / PM 可读。数据权限：默认按工程师本人过滤，管理员绕过。</p>
 *
 * @author vibe
 */
@Tag(name = "工程师资源池", description = "工程师档案 CRUD、技能管理、可用工程师查询")
@RestController
@RequestMapping("/api/v1/engineers")
@RequiredArgsConstructor
public class EngineerController {

    private final EngineerService engineerService;

    @Operation(summary = "分页查询工程师")
    @OperationLog(module = "工程师管理", type = ResourceConstant.BIZ_QUERY, description = "分页查询工程师")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping
    public Result<PageResult<EngineerVO>> page(@ParameterObject EngineerQueryDTO query) {
        return Result.success(engineerService.page(query));
    }

    @Operation(summary = "工程师详情（含技能列表）")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping("/{id}")
    public Result<EngineerVO> detail(@PathVariable Long id) {
        return Result.success(engineerService.getDetail(id));
    }

    @Operation(summary = "按用户ID查询工程师档案")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @GetMapping("/user/{userId}")
    public Result<EngineerVO> getByUserId(@PathVariable Long userId) {
        return Result.success(engineerService.getByUserId(userId));
    }

    @Operation(summary = "新增工程师")
    @OperationLog(module = "工程师管理", type = ResourceConstant.BIZ_INSERT,
            description = "新增工程师", saveResponse = true)
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody EngineerDTO dto) {
        return Result.success(engineerService.create(dto));
    }

    @Operation(summary = "编辑工程师")
    @OperationLog(module = "工程师管理", type = ResourceConstant.BIZ_UPDATE, description = "编辑工程师")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EngineerDTO dto) {
        dto.setId(id);
        engineerService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除工程师")
    @OperationLog(module = "工程师管理", type = ResourceConstant.BIZ_DELETE, description = "删除工程师")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        engineerService.delete(id);
        return Result.success();
    }

    @Operation(summary = "变更工程师状态（在职/离职）")
    @OperationLog(module = "工程师管理", type = ResourceConstant.BIZ_UPDATE, description = "变更工程师状态")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @Parameter(description = "状态 ACTIVE/RESIGNED", required = true)
                                     @RequestParam String status) {
        engineerService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "保存工程师技能列表（先删后插）")
    @OperationLog(module = "工程师管理", type = ResourceConstant.BIZ_UPDATE, description = "保存工程师技能")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @PutMapping("/{id}/skills")
    public Result<Void> saveSkills(@PathVariable Long id,
                                   @Valid @RequestBody List<EngineerSkillDTO> skills) {
        engineerService.saveSkills(id, skills);
        return Result.success();
    }

    @Operation(summary = "查询工程师技能列表")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping("/{id}/skills")
    public Result<List<EngineerSkillVO>> listSkills(@PathVariable Long id) {
        return Result.success(engineerService.listSkills(id));
    }

    @Operation(summary = "按技能/区域查询可用工程师（含当前负荷）")
    @OperationLog(module = "工程师管理", type = ResourceConstant.BIZ_QUERY, description = "查询可用工程师")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @GetMapping("/available")
    public Result<List<EngineerVO>> listAvailable(
            @Parameter(description = "技能标签列表（逗号分隔）") @RequestParam(required = false) String skillTags,
            @Parameter(description = "所属区域") @RequestParam(required = false) String region) {
        List<String> tags = skillTags == null || skillTags.isBlank()
                ? null : List.of(skillTags.split(","));
        return Result.success(engineerService.listAvailable(tags, region));
    }
}
