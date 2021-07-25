package org.infinity.rpc.core.server.stub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionMeta implements Serializable {
    private static final long serialVersionUID = -3886061954129097031L;

    /**
     * Option name
     */
    private String       name;
    /**
     * Option value
     */
    private String       value;
    /**
     * Option values
     */
    private List<String> values;
    /**
     * Value type
     */
    private String       type;
    /**
     * Default value
     */
    private Object       defaultValue;

    public Integer getIntValue() {
        return StringUtils.isEmpty(value) || "true".equals(value) || "false".equals(value) ? null : Integer.parseInt(value);
    }

    public void setIntValue(Integer intValue) {
        if (intValue != null) {
            value = intValue.toString();
        }
    }
}
