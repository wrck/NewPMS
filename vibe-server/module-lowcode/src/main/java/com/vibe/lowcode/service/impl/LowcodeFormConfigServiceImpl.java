package com.vibe.lowcode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.base.PageQuery;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.constant.LowcodeConstant;
import com.vibe.lowcode.dto.LowcodeFormConfigDTO;
import com.vibe.lowcode.entity.LowcodeFormConfigEntity;
import com.vibe.lowcode.entity.LowcodeTemplateEntity;
import com.vibe.lowcode.mapper.LowcodeFormConfigMapper;
import com.vibe.lowcode.mapper.LowcodeTemplateMapper;
import com.vibe.lowcode.service.LowcodeFormConfigService;
import com.vibe.lowcode.util.JsonSchemaValidator;
import com.vibe.lowcode.vo.LowcodeFormConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码表单配置 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class LowcodeFormConfigServiceImpl implements LowcodeFormConfigService {

    private final LowcodeFormConfigMapper formConfigMapper;
    private final LowcodeTemplateMapper templateMapper;

    @Override
    public PageResult<LowcodeFormConfigVO> page(PageQuery query, String keyword) {
        LambdaQueryWrapper<LowcodeFormConfigEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(LowcodeFormConfigEntity::getConfigCode, keyword)
                    .or().like(LowcodeFormConfigEntity::getConfigName, keyword));
        }
        wrapper.orderByDesc(LowcodeFormConfigEntity::getCreateTime);

        Page<LowcodeFormConfigEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<LowcodeFormConfigEntity> result = formConfigMapper.selectPage(page, wrapper);

        List<LowcodeFormConfigVO> records = new ArrayList<>();
        for (LowcodeFormConfigEntity e : result.getRecords()) {
            LowcodeFormConfigVO vo = new LowcodeFormConfigVO();
            BeanUtils.copyProperties(e, vo);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public LowcodeFormConfigVO getById(Long id) {
        LowcodeFormConfigEntity entity = formConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("表单配置");
        }
        LowcodeFormConfigVO vo = new LowcodeFormConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(LowcodeFormConfigDTO dto) {
        validateConfigCodeUnique(dto.getConfigCode());
        validateSchema(dto.getSchemaJson());

        LowcodeFormConfigEntity entity = new LowcodeFormConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        formConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, LowcodeFormConfigDTO dto) {
        LowcodeFormConfigEntity entity = formConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("表单配置");
        }
        validateConfigCodeUniqueExcludeSelf(dto.getConfigCode(), id);
        validateSchema(dto.getSchemaJson());

        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        formConfigMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LowcodeFormConfigEntity entity = formConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("表单配置");
        }
        formConfigMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        LowcodeFormConfigEntity source = formConfigMapper.selectById(id);
        if (source == null) {
            throw BusinessException.notFound("表单配置");
        }
        LowcodeFormConfigEntity copy = new LowcodeFormConfigEntity();
        BeanUtils.copyProperties(source, copy);
        copy.setId(null);
        copy.setConfigCode(source.getConfigCode() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setConfigName(source.getConfigName() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setTemplateId(null);
        copy.setCreatorId(UserContextHolder.getUserId());
        formConfigMapper.insert(copy);
        return copy.getId();
    }

    @Override
    public String exportJson(Long id) {
        LowcodeFormConfigEntity entity = formConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("表单配置");
        }
        return entity.getSchemaJson();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importJson(LowcodeFormConfigDTO dto) {
        validateSchema(dto.getSchemaJson());
        validateConfigCodeUnique(dto.getConfigCode());

        LowcodeFormConfigEntity entity = new LowcodeFormConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        formConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long instantiateFromTemplate(Long templateId, String configName) {
        LowcodeTemplateEntity template = templateMapper.selectById(templateId);
        if (template == null) {
            throw BusinessException.notFound("低代码模板");
        }
        if (!LowcodeConstant.TEMPLATE_TYPE_FORM.equals(template.getTemplateType())) {
            throw BusinessException.stateNotAllowed("模板类型不是 FORM，无法实例化为表单配置");
        }
        if (LowcodeConstant.STATUS_DISABLED.equals(template.getStatus())) {
            throw BusinessException.stateNotAllowed("模板已禁用，无法实例化");
        }

        LowcodeFormConfigEntity entity = new LowcodeFormConfigEntity();
        entity.setConfigCode(generateConfigCode(template.getTemplateCode()));
        entity.setConfigName(LowcodeConstant.INSTANTIATE_NAME_PREFIX + configName);
        entity.setSchemaJson(template.getSchemaJson());
        entity.setTemplateId(templateId);
        entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        entity.setDescription(template.getDescription());
        entity.setCreatorId(UserContextHolder.getUserId());
        formConfigMapper.insert(entity);

        // 模板 usageCount +1
        Integer currentCount = template.getUsageCount() == null ? 0 : template.getUsageCount();
        template.setUsageCount(currentCount + 1);
        templateMapper.updateById(template);

        return entity.getId();
    }

    /* ============ 校验辅助方法 ============ */

    private void validateConfigCodeUnique(String configCode) {
        Long count = formConfigMapper.selectCount(
                new LambdaQueryWrapper<LowcodeFormConfigEntity>()
                        .eq(LowcodeFormConfigEntity::getConfigCode, configCode));
        if (count > 0) {
            throw BusinessException.conflict("配置编码已存在：" + configCode);
        }
    }

    private void validateConfigCodeUniqueExcludeSelf(String configCode, Long id) {
        Long count = formConfigMapper.selectCount(
                new LambdaQueryWrapper<LowcodeFormConfigEntity>()
                        .eq(LowcodeFormConfigEntity::getConfigCode, configCode)
                        .ne(LowcodeFormConfigEntity::getId, id));
        if (count > 0) {
            throw BusinessException.conflict("配置编码已存在：" + configCode);
        }
    }

    private void validateSchema(String schemaJson) {
        if (!JsonSchemaValidator.isValid(schemaJson)) {
            List<String> errors = JsonSchemaValidator.validate(schemaJson);
            String detail = errors.isEmpty() ? "JSON Schema 格式不合法" : String.join("; ", errors);
            throw BusinessException.of(40000, "JSON Schema Draft 7 校验失败：" + detail);
        }
    }

    /** 基于模板编码生成唯一配置编码 */
    private String generateConfigCode(String templateCode) {
        return templateCode + "_" + System.currentTimeMillis();
    }
}
