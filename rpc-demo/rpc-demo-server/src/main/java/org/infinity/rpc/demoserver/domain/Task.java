package org.infinity.rpc.demoserver.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.infinity.rpc.democommon.domain.base.AbstractAuditableDomain;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the Task entity.
 */
@Document(collection = "Task")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Task extends AbstractAuditableDomain implements Serializable {
    private static final long    serialVersionUID = 8878535528271740314L;
    /**
     * Task name
     */
    @Indexed(unique = true)
    private              String  name;
    /**
     * Spring bean name
     */
    @NotEmpty
    @Indexed(unique = true)
    private              String  beanName;
    /**
     * Method arguments JSON string
     */
    private              String  argumentsJson;
    /**
     * Cron expression
     * https://cron.qqe2.com
     */
    private              String  cronExpression;
    /**
     * Fixed rate interval
     */
    private              Long    fixedRateInterval;
    /**
     * Remarks
     */
    private              String  remark;
    /**
     * Enabled
     */
    @NotNull
    private              Boolean enabled;
}