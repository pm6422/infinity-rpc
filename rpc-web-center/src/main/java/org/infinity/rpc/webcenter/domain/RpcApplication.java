package org.infinity.rpc.webcenter.domain;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the RpcApplication entity.
 */
@ApiModel("RPC application")
@Document(collection = "RpcApplication")
@Data
@NoArgsConstructor
public class RpcApplication extends ApplicationConfig implements Serializable {
    private static final long   serialVersionUID = 1L;
    public static final  String FIELD_NAME       = "name";
    public static final  String FIELD_ACTIVE     = "active";
    public static final  String FIELD_PROVIDING  = "providing";
    public static final  String FIELD_CONSUMING  = "consuming";

    @Id
    private String  id;
    private String  registryIdentity;
    private Boolean active;
    @Transient
    private Boolean providing;
    @Transient
    private Boolean consuming;
    private Instant createdTime;
    private Instant modifiedTime;
}
