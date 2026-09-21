package com.forever1996Fyk.ai.agentplus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forever1996Fyk.ai.agentplus.auth.dto.DeptSimpleVO;
import com.forever1996Fyk.ai.agentplus.auth.dto.LoginUserVO;
import com.forever1996Fyk.ai.agentplus.auth.dto.RoleSimpleVO;
import com.forever1996Fyk.ai.agentplus.auth.service.AuthService;
import com.forever1996Fyk.ai.agentplus.common.enums.DataScope;
import com.forever1996Fyk.ai.agentplus.service.SensitiveFilter;
import com.forever1996Fyk.ai.agentplus.service.UserContextBuilder;
import com.forever1996Fyk.ai.agentplus.sys.entity.SysUserProfile;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysUserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/21 16:10
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserContextBuilder implements UserContextBuilder {
    private final AuthService authService;
    private final SysUserProfileMapper userProfileMapper;
    private final SensitiveFilter sensitiveFilter;

    @Value("${data-agent.data-permission.enabled:false}")
    private boolean dataPermissionEnabled;

    @Value("${data-agent.sensitive-filter.enabled:false}")
    private boolean sensitiveFilterEnabled;

    @Value("${data-agent.sensitive-filter.mask-fields:sys_user.password,user_profile.id_card}")
    private List<String> maskFields;

    @Override
    public String build(Long userId) {
        if (userId == null) {
            return "";
        }
        LoginUserVO user;
        try {
            user = authService.getUserById(userId);
        } catch (Exception e) {
            log.warn("[UserContext] 加载用户失败 userId={}: {}", userId, e.getMessage());
            return "";
        }
        if (user == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n\n## 当前用户上下文\n\n");
        sb.append("- 用户ID：").append(user.getId()).append("\n");
        sb.append("- 用户名：").append(nullSafe(user.getUsername())).append("\n");
        sb.append("- 昵称：").append(nullSafe(user.getNickname())).append("\n");
        appendRoles(sb, user);
        appendDepts(sb, user);
        appendProfile(sb, userId);
        appendModeNote(sb);
        appendScopeNote(sb, user);
        return sb.toString();
    }

    private void appendRoles(StringBuilder sb, LoginUserVO user) {
        List<RoleSimpleVO> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            sb.append("- 角色：未分配\n");
            return;
        }
        String roleStr = roles.stream()
                .map(r -> nullSafe(r.getName()) + "(" + nullSafe(r.getCode()) + ")")
                .collect(Collectors.joining(", "));
        sb.append("- 角色：").append(roleStr).append("\n");
    }

    private void appendDepts(StringBuilder sb, LoginUserVO user) {
        List<DeptSimpleVO> depts = user.getDepts();
        if (depts == null || depts.isEmpty()) {
            sb.append("- 部门：未挂载\n");
            return;
        }
        String deptStr = depts.stream()
                .map(d -> nullSafe(d.getName()) + "(" + d.getId() + ")")
                .collect(Collectors.joining(", "));
        sb.append("- 部门：").append(deptStr).append("\n");
    }


    /**
     * 追加 user_profile，无对应行时跳过。
     */
    private void appendProfile(StringBuilder sb, Long userId) {
        SysUserProfile profile;
        try {
            profile = userProfileMapper.selectOne(
                    new LambdaQueryWrapper<SysUserProfile>().eq(SysUserProfile::getUserId, userId));
        } catch (Exception e) {
            log.debug("[UserContext] user_profile 不可用 userId={}: {}", userId, e.getMessage());
            return;
        }
        if (profile == null) {
            return;
        }
        sb.append("\n### 个人档案\n");
        appendIfPresent(sb, "真实姓名", profile.getRealName());
        appendIfPresent(sb, "身份证号", sensitiveFilter.maskValue("user_profile", "id_card", profile.getIdCard()));
        appendIfPresent(sb, "年龄", profile.getAge());
        appendIfPresent(sb, "学历", profile.getEducation());
        appendIfPresent(sb, "家庭住址", sensitiveFilter.maskValue("user_profile", "home_address", profile.getHomeAddress()));
    }

    private void appendScopeNote(StringBuilder sb, LoginUserVO user) {
        sb.append("\n### 用户问\"我的 xxx\"时的指代\n");
        sb.append("- 指当前用户（ID=").append(user.getId()).append("）的数据。\n");
        if (!dataPermissionEnabled) {
            return;
        }
        DataScope scope = DataScope.fromString(user.getDataScope());
        sb.append("- 数据范围 ").append(scope.name()).append("：");
        switch (scope) {
            case ALL -> sb.append("无数据权限限制。\n");
            case DEPT_AND_SUB -> sb.append("系统自动过滤本部门及子部门数据，无需手写 dept_id 条件。\n");
            case DEPT -> sb.append("系统自动过滤本部门数据，无需手写 dept_id 条件。\n");
            case SELF -> sb.append("系统自动过滤仅返回本人数据，无需手写 user_id 条件。\n");
        }
    }

    /**
     * 执行模式：数据权限/敏感字段脱敏开关状态，引导 LLM 不要盲目重试或绕过。
     */
    private void appendModeNote(StringBuilder sb) {
        sb.append("\n### 执行模式\n");
        sb.append("- 数据权限过滤：").append(dataPermissionEnabled ? "已开启" : "未开启").append("\n");
        if (dataPermissionEnabled) {
            sb.append("  - 查询会按当前用户角色自动注入 WHERE（dept_id / user_id），无需手写。\n");
        } else {
            sb.append("  - 查询不做权限过滤，所有数据可见。\n");
        }
        sb.append("- 敏感字段脱敏：").append(sensitiveFilterEnabled ? "已开启" : "未开启").append("\n");
        if (sensitiveFilterEnabled) {
            String cols = (maskFields == null || maskFields.isEmpty())
                    ? "（未配置列）"
                    : maskFields.stream()
                      .map(s -> {
                          int dot = s.lastIndexOf('.');
                          return (dot >= 0) ? s.substring(dot + 1) : s;
                      })
                      .collect(Collectors.joining("、"));
            sb.append("  - 命中列（").append(cols).append("）的值会返回 `********`，这是系统行为，不要尝试绕过或重试拿真实值。\n");
        }
    }

    private void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value == null) {
            return;
        }
        sb.append("- ").append(label).append("：").append(value).append("\n");
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
