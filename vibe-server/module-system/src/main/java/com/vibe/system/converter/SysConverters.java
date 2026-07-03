package com.vibe.system.converter;

import com.vibe.system.entity.SysConfigEntity;
import com.vibe.system.entity.SysDictDataEntity;
import com.vibe.system.entity.SysDictTypeEntity;
import com.vibe.system.entity.SysMenuEntity;
import com.vibe.system.entity.SysNoticeEntity;
import com.vibe.system.entity.SysNoticeTemplateEntity;
import com.vibe.system.entity.SysOrgEntity;
import com.vibe.system.entity.SysPositionEntity;
import com.vibe.system.entity.SysRoleEntity;
import com.vibe.system.entity.SysUserEntity;
import com.vibe.system.vo.SysConfigVO;
import com.vibe.system.vo.SysDictDataVO;
import com.vibe.system.vo.SysDictTypeVO;
import com.vibe.system.vo.SysMenuVO;
import com.vibe.system.vo.SysNoticeTemplateVO;
import com.vibe.system.vo.SysNoticeVO;
import com.vibe.system.vo.SysOrgVO;
import com.vibe.system.vo.SysPositionVO;
import com.vibe.system.vo.SysRoleVO;
import com.vibe.system.vo.SysUserVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统模块 Entity &lt;-&gt; VO 手工转换工具
 *
 * <p>为避免引入 MapStruct 注解处理器带来的编译复杂度，module-system 内统一使用本工具类进行转换。</p>
 *
 * @author vibe
 */
public final class SysConverters {

    private SysConverters() {
    }

    public static SysUserVO toUserVo(SysUserEntity e) {
        if (e == null) {
            return null;
        }
        SysUserVO vo = new SysUserVO();
        vo.setId(e.getId());
        vo.setUsername(e.getUsername());
        vo.setRealName(e.getRealName());
        vo.setPhone(e.getPhone());
        vo.setEmail(e.getEmail());
        vo.setAvatar(e.getAvatar());
        vo.setStatus(e.getStatus());
        vo.setTenantType(e.getTenantType());
        vo.setTenantId(e.getTenantId());
        vo.setOrgId(e.getOrgId());
        vo.setLastLoginTime(e.getLastLoginTime());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static SysRoleVO toRoleVo(SysRoleEntity e) {
        if (e == null) {
            return null;
        }
        SysRoleVO vo = new SysRoleVO();
        vo.setId(e.getId());
        vo.setRoleName(e.getRoleName());
        vo.setRoleCode(e.getRoleCode());
        vo.setDescription(e.getDescription());
        vo.setStatus(e.getStatus());
        vo.setDataScope(e.getDataScope());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static SysMenuVO toMenuVo(SysMenuEntity e) {
        if (e == null) {
            return null;
        }
        SysMenuVO vo = new SysMenuVO();
        vo.setId(e.getId());
        vo.setParentId(e.getParentId());
        vo.setMenuName(e.getMenuName());
        vo.setMenuType(e.getMenuType());
        vo.setPath(e.getPath());
        vo.setComponent(e.getComponent());
        vo.setPerms(e.getPerms());
        vo.setIcon(e.getIcon());
        vo.setSortOrder(e.getSortOrder());
        vo.setVisible(e.getVisible());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static SysOrgVO toOrgVo(SysOrgEntity e) {
        if (e == null) {
            return null;
        }
        SysOrgVO vo = new SysOrgVO();
        vo.setId(e.getId());
        vo.setParentId(e.getParentId());
        vo.setOrgName(e.getOrgName());
        vo.setOrgCode(e.getOrgCode());
        vo.setSortOrder(e.getSortOrder());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static SysPositionVO toPositionVo(SysPositionEntity e) {
        if (e == null) {
            return null;
        }
        SysPositionVO vo = new SysPositionVO();
        vo.setId(e.getId());
        vo.setOrgId(e.getOrgId());
        vo.setPositionName(e.getPositionName());
        vo.setPositionCode(e.getPositionCode());
        vo.setSortOrder(e.getSortOrder());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static SysDictTypeVO toDictTypeVo(SysDictTypeEntity e) {
        if (e == null) {
            return null;
        }
        SysDictTypeVO vo = new SysDictTypeVO();
        vo.setId(e.getId());
        vo.setDictName(e.getDictName());
        vo.setDictType(e.getDictType());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static SysDictDataVO toDictDataVo(SysDictDataEntity e) {
        if (e == null) {
            return null;
        }
        SysDictDataVO vo = new SysDictDataVO();
        vo.setId(e.getId());
        vo.setDictType(e.getDictType());
        vo.setDictLabel(e.getDictLabel());
        vo.setDictValue(e.getDictValue());
        vo.setSortOrder(e.getSortOrder());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static SysConfigVO toConfigVo(SysConfigEntity e) {
        if (e == null) {
            return null;
        }
        SysConfigVO vo = new SysConfigVO();
        vo.setId(e.getId());
        vo.setConfigName(e.getConfigName());
        vo.setConfigKey(e.getConfigKey());
        vo.setConfigValue(e.getConfigValue());
        vo.setConfigType(e.getConfigType());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static SysNoticeVO toNoticeVo(SysNoticeEntity e) {
        if (e == null) {
            return null;
        }
        SysNoticeVO vo = new SysNoticeVO();
        vo.setId(e.getId());
        vo.setNoticeTitle(e.getNoticeTitle());
        vo.setNoticeType(e.getNoticeType());
        vo.setNoticeContent(e.getNoticeContent());
        vo.setRecipientId(e.getRecipientId());
        vo.setReadStatus(e.getReadStatus());
        vo.setSendTime(e.getSendTime());
        return vo;
    }

    public static SysNoticeTemplateVO toTemplateVo(SysNoticeTemplateEntity e) {
        if (e == null) {
            return null;
        }
        SysNoticeTemplateVO vo = new SysNoticeTemplateVO();
        vo.setId(e.getId());
        vo.setTemplateCode(e.getTemplateCode());
        vo.setTemplateName(e.getTemplateName());
        vo.setTitleTemplate(e.getTitleTemplate());
        vo.setContentTemplate(e.getContentTemplate());
        vo.setChannels(e.getChannels());
        vo.setRecipientType(e.getRecipientType());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    /**
     * 构建菜单树（按 parentId 组装）
     */
    public static List<SysMenuVO> buildMenuTree(List<SysMenuVO> all) {
        List<SysMenuVO> roots = new ArrayList<>();
        if (all == null || all.isEmpty()) {
            return roots;
        }
        for (SysMenuVO m : all) {
            if (m.getParentId() == null || m.getParentId() == 0L) {
                roots.add(m);
            }
        }
        for (SysMenuVO root : roots) {
            buildMenuChildren(root, all);
        }
        return roots;
    }

    private static void buildMenuChildren(SysMenuVO parent, List<SysMenuVO> all) {
        List<SysMenuVO> children = new ArrayList<>();
        for (SysMenuVO m : all) {
            if (parent.getId() != null && parent.getId().equals(m.getParentId())) {
                children.add(m);
                buildMenuChildren(m, all);
            }
        }
        parent.setChildren(children.isEmpty() ? null : children);
    }

    /**
     * 构建组织树
     */
    public static List<SysOrgVO> buildOrgTree(List<SysOrgVO> all) {
        List<SysOrgVO> roots = new ArrayList<>();
        if (all == null || all.isEmpty()) {
            return roots;
        }
        for (SysOrgVO o : all) {
            if (o.getParentId() == null || o.getParentId() == 0L) {
                roots.add(o);
            }
        }
        for (SysOrgVO root : roots) {
            buildOrgChildren(root, all);
        }
        return roots;
    }

    private static void buildOrgChildren(SysOrgVO parent, List<SysOrgVO> all) {
        List<SysOrgVO> children = new ArrayList<>();
        for (SysOrgVO o : all) {
            if (parent.getId() != null && parent.getId().equals(o.getParentId())) {
                children.add(o);
                buildOrgChildren(o, all);
            }
        }
        parent.setChildren(children.isEmpty() ? null : children);
    }
}
