package org.infinity.luix.webcenter.service;

import org.infinity.luix.core.url.Url;
import org.infinity.luix.webcenter.domain.RpcServer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcServerService {

    void loadAll();

    void updateStatus();

    boolean exists(String registryIdentity, String address);

    Page<RpcServer> find(Pageable pageable, String registryIdentity, String address);

    void insert(Url registryUrl, Url url, String address);

    void inactivate(String registryIdentity, String address);

    RpcServer loadServer(Url registryUrl, Url url);

    RpcServer loadServer(String registryIdentity, String address);
}
