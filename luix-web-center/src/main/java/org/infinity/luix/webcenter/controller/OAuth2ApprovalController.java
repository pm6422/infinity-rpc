package org.infinity.luix.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.repository.OAuth2ApprovalRepository;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.domain.MongoOAuth2Approval;
import org.infinity.luix.webcenter.exception.DataNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Slf4j
public class OAuth2ApprovalController {

    @Resource
    private OAuth2ApprovalRepository oAuth2ApprovalRepository;
    @Resource
    private MongoTemplate            mongoTemplate;
    @Resource
    private HttpHeaderCreator        httpHeaderCreator;

    @ApiOperation("find approval list")
    @GetMapping("/api/oauth2-approvals")
    @Secured(Authority.ADMIN)
    public ResponseEntity<List<MongoOAuth2Approval>> find(Pageable pageable,
                                                          @ApiParam(value = "approval ID") @RequestParam(value = "approvalId", required = false) String approvalId,
                                                          @ApiParam(value = "client ID") @RequestParam(value = "clientId", required = false) String clientId,
                                                          @ApiParam(value = "user name") @RequestParam(value = "userName", required = false) String userName) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(approvalId)) {
            query.addCriteria(Criteria.where("id").is(approvalId));
        }
        if (StringUtils.isNotEmpty(clientId)) {
            query.addCriteria(Criteria.where("clientId").is(clientId));
        }
        if (StringUtils.isNotEmpty(userName)) {
            query.addCriteria(Criteria.where("userId").is(userName));
        }
        long totalCount = mongoTemplate.count(query, MongoOAuth2Approval.class);
        query.with(pageable);
        Page<MongoOAuth2Approval> approvals = new PageImpl<>(mongoTemplate.find(query, MongoOAuth2Approval.class), pageable, totalCount);
        HttpHeaders headers = HttpHeaderUtils.generatePageHeaders(approvals);
        return ResponseEntity.ok().headers(headers).body(approvals.getContent());
    }

    @ApiOperation("find approval by ID")
    @GetMapping("/api/oauth2-approvals/{id}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<MongoOAuth2Approval> findById(
            @ApiParam(value = "ID", required = true) @PathVariable String id) {
        MongoOAuth2Approval domain = oAuth2ApprovalRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation(value = "delete approval by ID", notes = "the data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/oauth2-approvals/{id}")
    @Secured(Authority.ADMIN)
    public ResponseEntity<Void> delete(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete oauth2 approval: {}", id);
        oAuth2ApprovalRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        oAuth2ApprovalRepository.deleteById(id);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }
}
