package com.joinup.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户主表实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`user`")
public class UserEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("password_hash")
    private String passwordHash;

    /** 取值见 UserStatusEnum。 */
    @TableField("status")
    private Integer status;

    /** 当前信用分，后续会被报名/爽约规则直接读取。 */
    @TableField("credit_score")
    private Integer creditScore;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @Version
    /** 乐观锁版本，预留给信用分更新等并发写场景。 */
    @TableField("version")
    private Integer version;
}
