package com.luixtech.rpc.webcenter.controller;

import com.google.common.collect.ImmutableMap;
import com.luixtech.rpc.webcenter.component.HttpHeaderCreator;
import com.luixtech.rpc.webcenter.domain.Authority;
import com.luixtech.rpc.webcenter.exception.DataNotFoundException;
import com.luixtech.rpc.webcenter.exception.DuplicationException;
import com.luixtech.rpc.webcenter.repository.AuthorityRepository;
import com.luixtech.rpc.webcenter.utils.HttpHeaderUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * REST controller for managing authorities.
 */
@RestController
@Slf4j
public class AuthorityController {

    @Resource
    private AuthorityRepository authorityRepository;
    @Resource
    private HttpHeaderCreator   httpHeaderCreator;

    @Operation(summary = "create authority")
    @PostMapping("/api/authorities")
    public ResponseEntity<Void> create(
            @Parameter(description = "authority", required = true) @Valid @RequestBody Authority domain) {
        log.debug("REST request to create authority: {}", domain);
        authorityRepository.findById(domain.getName()).ifPresent(app -> {
            throw new DuplicationException(ImmutableMap.of("name", domain.getName()));
        });
        authorityRepository.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName()))
                .build();
    }

    @Operation(summary = "find authority list")
    @GetMapping("/api/authorities")
    public ResponseEntity<List<Authority>> find(Pageable pageable) {
        Page<Authority> authorities = authorityRepository.findAll(pageable);
        HttpHeaders headers = HttpHeaderUtils.generatePageHeaders(authorities);
        return ResponseEntity.ok().headers(headers).body(authorities.getContent());
    }

    @Operation(summary = "find authority by name")
    @GetMapping("/api/authorities/{name}")
    public ResponseEntity<Authority> findById(
            @Parameter(description = "name", required = true) @PathVariable String name) {
        Authority domain = authorityRepository.findById(name).orElseThrow(() -> new DataNotFoundException(name));
        return ResponseEntity.ok(domain);
    }

    @Operation(summary = "update authority")
    @PutMapping("/api/authorities")
    public ResponseEntity<Void> update(
            @Parameter(description = "new authority", required = true) @Valid @RequestBody Authority domain) {
        log.debug("REST request to update authority: {}", domain);
        authorityRepository.findById(domain.getName()).orElseThrow(() -> new DataNotFoundException(domain.getName()));
        authorityRepository.save(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @Operation(summary = "delete authority by name", description = "the data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/authorities/{name}")
    public ResponseEntity<Void> delete(@Parameter(description = "name", required = true) @PathVariable String name) {
        log.debug("REST request to delete authority: {}", name);
        authorityRepository.findById(name).orElseThrow(() -> new DataNotFoundException(name));
        authorityRepository.deleteById(name);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", name)).build();
    }
}
