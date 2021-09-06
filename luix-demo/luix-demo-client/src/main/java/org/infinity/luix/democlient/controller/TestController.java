package org.infinity.luix.democlient.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.client.annotation.RpcConsumer;
import org.infinity.luix.democlient.restservice.AppRestService;
import org.infinity.luix.democommon.domain.App;
import org.infinity.luix.democommon.domain.Authority;
import org.infinity.luix.democommon.service.AppService;
import org.infinity.luix.democommon.service.AuthorityService;
import org.infinity.luix.utilities.id.IdGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static org.infinity.luix.democommon.domain.Authority.ADMIN;
import static org.infinity.luix.democommon.domain.Authority.USER;

@RestController
@Slf4j
public class TestController {

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private Environment      env;
    @Resource
    private AppRestService   appRestService;
    @RpcConsumer
    private AuthorityService authorityService;
    @RpcConsumer(providerAddresses = "127.0.0.1:26010", form = "f2")
    private AppService         appService;

    @ApiOperation("test kryo serialization and deserialization")
    @GetMapping("/api/authority-names")
    public ResponseEntity<List<String>> find() {
        Query query = Query.query(Criteria.where("name").in(ADMIN, USER));
        List<String> authorities = authorityService.find(query).stream().map(Authority::getName).collect(Collectors.toList());
        return ResponseEntity.ok(authorities);
    }

    @ApiOperation("direct connect")
    @GetMapping("/api/tests/direct-url")
    public List<App> directUrl() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<App> all = appService.findAll(pageable);
        return all.getContent();
    }

    @ApiOperation("create app by forest http client")
    @PostMapping("/api/tests/app")
    public void createApp() {
        App app = new App(String.valueOf(IdGenerator.generateShortId()), true);
        appRestService.create(app);
    }
}