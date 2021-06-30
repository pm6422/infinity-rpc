package org.infinity.rpc.webcenter.domain;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.rpc.core.url.Url;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.DigestUtils;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the RpcService entity.
 */
@ApiModel("RPC service")
@Document(collection = "RpcService")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcService implements Serializable {
    private static final long   serialVersionUID        = 1L;
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_INTERFACE_NAME    = "interfaceName";

    @Id
    private String  id;
    private String  registryIdentity;
    private String  interfaceName;
    private Boolean active;
    @Transient
    private Boolean providing;
    @Transient
    private Boolean consuming;

    public static RpcService of(String interfaceName, Url registryUrl) {
        RpcService rpcService = new RpcService();
        String id = DigestUtils.md5DigestAsHex((interfaceName + "@" + registryUrl.getIdentity()).getBytes());
        rpcService.setId(id);
        rpcService.setRegistryIdentity(registryUrl.getIdentity());
        rpcService.setInterfaceName(interfaceName);
        return rpcService;
    }
}
