package com.luixtech.rpc.democommon.domain;

import com.luixtech.rpc.democommon.domain.base.AbstractAuditableDomain;
import com.luixtech.rpc.democommon.dto.AdminMenuDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the AdminMenu entity.
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class AdminMenu extends AbstractAuditableDomain implements Serializable {

    private static final long   serialVersionUID = 5423774898556939254L;
    public static final  String FIELD_LEVEL      = "level";
    public static final  String FIELD_SEQUENCE   = "sequence";

    @Schema(required = true)
    @NotNull
    @Size(min = 1, max = 30)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "{EP5901}")
    @Indexed(unique = true)
    private String code;

    @Schema(required = true)
    @NotNull
    @Size(min = 1, max = 30)
    private String name;

    @Schema(required = true)
    @Min(1)
    @Max(9)
    private Integer level;

    @Schema(required = true)
    @NotNull
    @Size(min = 3, max = 200)
    private String url;

    @Schema(required = true)
    @Min(1)
    @Max(999)
    private Integer sequence;

    private String parentId;

    @Transient
    private Boolean checked;

    public AdminMenu(String code, String name, Integer level, String url,
                     Integer sequence, String parentId) {
        super();
        this.code = code;
        this.name = name;
        this.level = level;
        this.url = url;
        this.sequence = sequence;
        this.parentId = parentId;
    }

    public AdminMenuDTO toDTO() {
        AdminMenuDTO dto = new AdminMenuDTO();
        BeanUtils.copyProperties(this, dto);
        return dto;
    }
}