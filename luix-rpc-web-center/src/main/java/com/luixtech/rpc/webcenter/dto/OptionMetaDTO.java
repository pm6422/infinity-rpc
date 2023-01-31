package com.luixtech.rpc.webcenter.dto;

import com.luixtech.rpc.core.server.stub.OptionMeta;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionMetaDTO extends OptionMeta {
    /**
     * Option value
     */
    private String value;

    public Integer getIntValue() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean getBooleanValue() {
        return StringUtils.isEmpty(value) ? null : "true".equals(value);
    }

    public static OptionMetaDTO of(OptionMeta optionMeta) {
        OptionMetaDTO dto = new OptionMetaDTO();
        BeanUtils.copyProperties(optionMeta, dto);
        return dto;
    }
}
