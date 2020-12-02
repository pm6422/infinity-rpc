package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.domain.App;
import org.infinity.app.common.dto.AppDTO;
import org.infinity.app.common.service.AppService;
import org.infinity.rpc.appclient.component.HttpHeaderCreator;
import org.infinity.rpc.appclient.exception.NoDataException;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.*;
import static org.infinity.rpc.appclient.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing apps.
 */
@RestController
@Api(tags = "应用管理")
@Slf4j
public class AppController {

    @Consumer(timeout = 10000)
    private AppService appService;

    private final HttpHeaderCreator httpHeaderCreator;

    public AppController(HttpHeaderCreator httpHeaderCreator) {
        this.httpHeaderCreator = httpHeaderCreator;
    }

    @ApiOperation("创建应用")
    @ApiResponses(value = {@ApiResponse(code = SC_CREATED, message = "成功创建")})
    @PostMapping("/api/app/apps")
    public ResponseEntity<Void> create(@ApiParam(value = "应用", required = true) @Valid @RequestBody AppDTO dto) {
        log.debug("REST request to create app: {}", dto);
        appService.insert(dto.getName(), dto.getEnabled(), dto.getAuthorities());
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("notification.app.created", dto.getName())).build();
    }

    @ApiOperation("(分页)检索应用列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("/api/app/apps")
    public ResponseEntity<List<AppDTO>> find(Pageable pageable) throws URISyntaxException {
        Page<App> apps = appService.findAll(pageable);
        List<AppDTO> DTOs = apps.getContent().stream().map(App::toDTO).collect(Collectors.toList());
        HttpHeaders headers = generatePageHeaders(apps, "/api/app/apps");
        return ResponseEntity.ok().headers(headers).body(DTOs);
    }

    @ApiOperation("根据名称检索应用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "应用不存在")})
    @GetMapping("/api/app/apps/{name}")
    public ResponseEntity<AppDTO> findById(@ApiParam(value = "应用名称", required = true) @PathVariable String name) {
        App app = appService.findById(name).orElseThrow(() -> new NoDataException(name));
        return ResponseEntity.ok(new AppDTO(name, app.getEnabled()));
    }

    @ApiOperation("更新应用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功更新"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "应用不存在")})
    @PutMapping("/api/app/apps")
    public ResponseEntity<Void> update(@ApiParam(value = "新的应用", required = true) @Valid @RequestBody AppDTO dto) {
        log.debug("REST request to update app: {}", dto);
        appService.findById(dto.getName()).orElseThrow(() -> new NoDataException(dto.getName()));
        appService.update(dto.getName(), dto.getEnabled(), dto.getAuthorities());
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.app.updated", dto.getName())).build();
    }

    @ApiOperation(value = "根据名称删除应用", notes = "数据有可能被其他数据所引用，删除之后可能出现一些问题")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功删除"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "应用不存在")})
    @DeleteMapping("/api/app/apps/{name}")
    public ResponseEntity<Void> delete(@ApiParam(value = "应用名称", required = true) @PathVariable String name) {
        log.debug("REST request to delete app: {}", name);
        appService.findById(name).orElseThrow(() -> new NoDataException(name));
        appService.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.app.deleted", name)).build();
    }
}
