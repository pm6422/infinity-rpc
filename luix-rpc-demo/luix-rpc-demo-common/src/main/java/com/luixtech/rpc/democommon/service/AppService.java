package com.luixtech.rpc.democommon.service;

import com.luixtech.rpc.core.server.response.FutureResponse;
import com.luixtech.rpc.democommon.domain.App;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppService {

    Page<App> findAll(Pageable pageable);

//    Optional<App> findById(String id);

    App findById(String id);

    FutureResponse insert(App domain);

    void update(App domain);

    void deleteById(String id);
}