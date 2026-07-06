package com.vibe.lowcode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.base.PageQuery;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.constant.LowcodeConstant;
import com.vibe.lowcode.dto.LowcodeTabConfigDTO;
import com.vibe.lowcode.entity.LowcodeTabConfigEntity;
import com.vibe.lowcode.entity.LowcodeTemplateEntity;
import com.vibe.lowcode.mapper.LowcodeTabConfigMapper;
import com.vibe.lowcode.mapper.LowcodeTemplateMapper;
import com.vibe.lowcode.service.LowcodeTabConfigService;
import com.vibe.lowcode.util.JsonSchemaValidator;
import com.vibe.lowcode.vo.LowcodeTabConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码标签页配置 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class LowcodeTabConfigServiceImpl implements LowcodeTabConfigService {

    private final LowcodeTabConfigMapper tabConfigMapper;
    private final LowcodeTemplateMapper templateMapper;

    @Override
    public PageResult<LowcodeTabConfigVO> page(PageQuery query, String keyword) {
        LambdaQueryWrapper<LowcodeTabConfigEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(LowcodeTabConfigEntity::getConfigCode, keyword)
                    .or().like(LowcodeTabConfigEntity::getConfigName, keyword));
        }
        wrapper.orderByDesc(LowcodeTabConfigEntity::getCreateTime);

        Page<LowcodeTabConfigEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<LowcodeTabConfigEntity> result = tabConfigMapper.selectPage(page, wrapper);

        List<LowcodeTabConfigVO> records = new ArrayList<>();
        for (LowcodeTabConfigEntity e : result.getRecords()) {
            LowcodeTabConfigVO vo = new LowcodeTabConfigVO();
            BeanUtils.copyProperties(e, vo);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public LowcodeTabConfigVO getById(Long id) {
        LowcodeTabConfigEntity entity = tabConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("标签页配置");
        }
        LowcodeTabConfigVO vo = new LowcodeTabConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(LowcodeTabConfigDTO dto) {
        validateConfigCodeUnique(dto.getConfigCode());
        validateSchema(dto.getSchemaJson());

        LowcodeTabConfigEntity entity = new LowcodeTabConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        tabConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, LowcodeTabConfigDTO dto) {
        LowcodeTabConfigEntity entity = tabConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("标签页配置");
        }
        validateConfigCodeUniqueExcludeSelf(dto.getConfigCode(), id);
        validateSchema(dto.getSchemaJson());

        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        tabConfigMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LowcodeTabConfigEntity entity = tabConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("标签页配置");
        }
        tabConfigMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        LowcodeTabConfigEntity source = tabConfigMapper.selectById(id);
        if (source == null) {
            throw BusinessException.notFound("标签页配置");
        }
        LowcodeTabConfigEntity copy = new LowcodeTabConfigEntity();
        BeanUtils.copyProperties(source, copy);
        copy.setId(null);
        copy.setConfigCode(source.getConfigCode() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setConfigName(source.getConfigName() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setTemplateId(null);
        copy.setCreatorId(UserContextHolder.getUserId());
        tabConfigMapper.insert(copy);
        return copy.getId();
    }

    @Override
    public String exportJson(Long id) {
        LowcodeTabConfigEntity entity = tabConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("标签页配置");
        }
        return entity.getSchemaJson();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importJson(LowcodeTabConfigDTO dto) {
        validateSchema(dto.getSchemaJson());
        validateConfigCodeUnique(dto.getConfigCode());

        LowcodeTabConfigEntity entity = new LowcodeTabConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        tabConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long instantiateFromTemplate(Long templateId, String configName) {
        LowcodeTemplateEntity template = templateMapper.selectById(templateId);
        if (template == null) {
            throw BusinessException.notFound("低代码模板");
        }
        if (!LowcodeConstant.TEMPLATE_TYPE_TAB.equals(template.getTemplateType())) {
            throw BusinessException.stateNotAllowed("模板类型不是 TAB，无法实例化为标签页配置");
        }
        if (LowcodeConstant.STATUS_DISABLED.equals(template.getStatus())) {
            throw BusinessException.stateNotAllowed("模板已禁用，无法实例化");
        }

        LowcodeTabConfigEntity entity = new LowcodeTabConfigEntity();
        entity.setConfigCode(generateConfigCode(template.getTemplateCode()));
        entity.setConfigName(LowcodeConstant.INSTANTIATE_NAME_PREFIX + configName);
        entity.setSchemaJson(template.getSchemaJson());
        entity.setTemplateId(templateId);
        entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        entity.setDescription(template.getDescription());
        entity.setCreatorId(UserContextHolder.getUserId());
        tabConfigMapper.insert(entity);

        Integer currentCount = template.getUsageCount() == null ? 0 : template.getUsageCount();
        template.setUsageCount(currentCount + 1);
        templateMapper.updateById(template);

        return entity.getId();
    }

    /* ============ 校验辅助方法 ============ */

    private void validateConfigCodeUnique(String configCode) {
        Long count = tabConfigMapper.selectCount(
                new LambdaQueryWrapper<LowcodeTabConfigEntity>()
                        .eq(LowcodeTabConfigEntity::getConfigCode, configCode));
        if (count > 0) {
            throw BusinessException.conflict("配置编码已存在：" + configCode);
        }
    }

    private void validateConfigCodeUniqueExcludeSelf(String configCode, Long id) {
        Long count = tabConfigMapper.selectCount(
                new LambdaQueryWrapper<LowcodeTabConfigEntity>()
                        .eq(LowcodeTabConfigEntity::getConfigCode, configCode)
                        .ne(LowcodeTabConfigEntity::getId, id));
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

    private String generateConfigCode(String templateCode) {
        return templateCode + "_" + System.currentTimeMillis();
    }
}
