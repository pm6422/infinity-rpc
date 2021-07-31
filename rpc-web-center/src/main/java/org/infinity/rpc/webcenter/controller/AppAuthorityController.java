package org.infinity.rpc.webcenter.controller;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.AppAuthority;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.exception.DuplicationException;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.AppAuthorityRepository;
import org.infinity.rpc.webcenter.service.AppAuthorityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing the app authority.
 */
@RestController
@Slf4j
public class AppAuthorityController {

    @Resource
    private AppAuthorityRepository appAuthorityRepository;
    @Resource
    private AppAuthorityService    appAuthorityService;
    @Resource
    private HttpHeaderCreator      httpHeaderCreator;

    @ApiOperation("create application authority")
    @PostMapping("/api/app-authorities")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> create(
            @ApiParam(value = "application authority", required = true) @Valid @RequestBody AppAuthority domain) {
        log.debug("REST request to create app authority: {}", domain);
        appAuthorityRepository.findOneByAppNameAndAuthorityName(domain.getAppName(), domain.getAuthorityName())
                .ifPresent((existingEntity) -> {
                    throw new DuplicationException(ImmutableMap.of("appName", domain.getAppName(), "authorityName", domain.getAuthorityName()));
                });

        AppAuthority appAuthority = appAuthorityRepository.insert(domain);
        return ResponseEntity
                .status(HttpStatus.CREATED).headers(httpHeaderCreator.createSuccessHeader("SM1001", appAuthority.getAuthorityName()))
                .build();
    }

    @ApiOperation("find application authority list")
    @GetMapping("/api/app-authorities")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<AppAuthority>> find(Pageable pageable,
                                                   @ApiParam(value = "application name") @RequestParam(value = "appName", required = false) String appName,
                                                   @ApiParam(value = "authority name") @RequestParam(value = "authorityName", required = false) String authorityName) {
        Page<AppAuthority> appAuthorities = appAuthorityService.find(pageable, appName, authorityName);
        HttpHeaders headers = generatePageHeaders(appAuthorities);
        return ResponseEntity.ok().headers(headers).body(appAuthorities.getContent());
    }

    @ApiOperation("find application authority by ID")
    @GetMapping("/api/app-authorities/{id}")
    @Secured({Authority.DEVELOPER, Authority.USER})
    public ResponseEntity<AppAuthority> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to get app authority : {}", id);
        AppAuthority domain = appAuthorityRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("update application authority")
    @PutMapping("/api/app-authorities")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> update(
            @ApiParam(value = "new application authority", required = true) @Valid @RequestBody AppAuthority domain) {
        log.debug("REST request to update app authority: {}", domain);
        appAuthorityRepository.findById(domain.getId()).orElseThrow(() -> new NoDataFoundException(domain.getId()));
        appAuthorityRepository.save(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getAuthorityName())).build();
    }

    @ApiOperation(value = "delete application authority by ID", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/app-authorities/{id}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> delete(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete app authority: {}", id);
        AppAuthority appAuthority = appAuthorityRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        appAuthorityRepository.deleteById(id);
        log.info("Deleted app authority");
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", appAuthority.getAuthorityName())).build();
    }
}
