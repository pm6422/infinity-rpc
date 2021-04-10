package org.infinity.rpc.democlient.domain;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the Provider entity.
 */
@ApiModel("服务提供者")
@Document(collection = "Provider")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provider implements Serializable {
    public static final String FIELD_INTERFACE_NAME = "interfaceName";
    public static final String FIELD_APPLICATION    = "application";
    public static final String FIELD_REGISTRY_URL   = "registryUrl";
    public static final String FIELD_ACTIVE         = "active";

    @Id
    protected String id;

    private String interfaceName;

    private String form;

    private String version;

    private String application;

    private String host;

    private String address;

    private String providerUrl;

    private String registryUrl;

    private Boolean active;

    protected Instant createdTime;

    protected Instant modifiedTime;
}
