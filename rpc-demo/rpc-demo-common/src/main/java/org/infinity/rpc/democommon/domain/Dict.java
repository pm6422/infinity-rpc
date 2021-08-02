package org.infinity.rpc.democommon.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.infinity.rpc.democommon.domain.base.AbstractAuditableDomain;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the Dict entity.
 */
@Document(collection = "Dict")
@Data
@ToString(callSuper = true)
@NoArgsConstructor
public class Dict extends AbstractAuditableDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(required = true)
    @NotNull
    @Size(min = 2, max = 50)
    @Indexed(unique = true)
    private String dictCode;

    @ApiModelProperty(required = true)
    @NotNull
    @Size(min = 2, max = 50)
    private String dictName;

    private String remark;

    private Boolean enabled;

    public Dict(String dictName, Boolean enabled) {
        this.dictName = dictName;
        this.enabled = enabled;
    }

    public Dict(String dictCode, String dictName, String remark, Boolean enabled) {
        super();
        this.dictCode = dictCode;
        this.dictName = dictName;
        this.remark = remark;
        this.enabled = enabled;
    }
}