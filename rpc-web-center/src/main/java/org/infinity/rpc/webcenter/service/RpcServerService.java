package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.RpcServer;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcServerService {

    boolean exists(String registryIdentity, String address);

    Page<RpcServer> find(Pageable pageable, String registryIdentity, String address);

    void inactivate(String registryIdentity, String address);
}
