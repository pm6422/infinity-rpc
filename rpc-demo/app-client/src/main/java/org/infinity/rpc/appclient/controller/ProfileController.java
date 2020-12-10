package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.infinity.rpc.appclient.config.ApplicationProperties;
import org.infinity.rpc.appclient.dto.ProfileInfoDTO;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@Api(tags = "系统环境")
public class ProfileController {

    private final Environment env;

    private final ApplicationProperties applicationProperties;

    public ProfileController(Environment env, ApplicationProperties applicationProperties) {
        this.env = env;
        this.applicationProperties = applicationProperties;
    }

    @ApiOperation("检索系统Profile")
    @GetMapping("/open-api/profile-info")
    public ResponseEntity<ProfileInfoDTO> getProfileInfo() {
        ProfileInfoDTO profileInfoDTO = new ProfileInfoDTO(env.getActiveProfiles(), applicationProperties.getSwagger().isEnabled(), getRibbonEnv());
        return ResponseEntity.ok(profileInfoDTO);
    }

    private String getRibbonEnv() {
        String[] activeProfiles = env.getActiveProfiles();
        String[] displayOnActiveProfiles = applicationProperties.getRibbon().getDisplayOnActiveProfiles();
        if (displayOnActiveProfiles == null) {
            return null;
        }

        List<String> ribbonProfiles = new ArrayList<>(Arrays.asList(displayOnActiveProfiles));
        List<String> springBootProfiles = Arrays.asList(activeProfiles);
        ribbonProfiles.retainAll(springBootProfiles);

        if (ribbonProfiles.size() > 0) {
            return ribbonProfiles.get(0);
        }
        return null;
    }
}
