package org.infinity.rpc.democommon.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.infinity.rpc.democommon.domain.AdminMenu;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AdminMenuTreeDTO extends AdminMenu implements Serializable {

    private static final long                   serialVersionUID = 1L;
    private              List<AdminMenuTreeDTO> children;
}

