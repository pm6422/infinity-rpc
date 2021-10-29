package org.infinity.luix.webcenter.initializer;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.utilities.id.IdGenerator;
import org.infinity.luix.webcenter.domain.*;
import org.infinity.luix.webcenter.service.RpcApplicationService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.infinity.luix.webcenter.domain.RpcScheduledTask.UNIT_MINUTES;

/**
 * Creates the initial database
 */
@ChangeLog(order = "01")
public class DatabaseInitializer {

    private static final String APP_NAME = "rpc-web-center";

    @ChangeSet(order = "01", author = "Louis", id = "addApps", runAlways = true)
    public void addApps(MongockTemplate mongoTemplate) {
        App app = new App(APP_NAME, true);
        mongoTemplate.save(app);
    }

    @ChangeSet(order = "02", author = "Louis", id = "addAuthorities", runAlways = true)
    public void addAuthorities(MongockTemplate mongoTemplate) {
        mongoTemplate.save(new Authority(Authority.USER, true));
        mongoTemplate.save(new Authority(Authority.ADMIN, true));
        mongoTemplate.save(new Authority(Authority.DEVELOPER, true));
        mongoTemplate.save(new Authority(Authority.ANONYMOUS, true));

        mongoTemplate.save(new AppAuthority(APP_NAME, Authority.USER));
        mongoTemplate.save(new AppAuthority(APP_NAME, Authority.ADMIN));
        mongoTemplate.save(new AppAuthority(APP_NAME, Authority.DEVELOPER));
        mongoTemplate.save(new AppAuthority(APP_NAME, Authority.ANONYMOUS));
    }

    @ChangeSet(order = "03", author = "Louis", id = "addUserAndAuthorities", runAlways = true)
    public void addUserAndAuthorities(MongockTemplate mongoTemplate) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // Creates 'user' user and corresponding authorities
        User userRoleUser = new User();
        userRoleUser.setUserName("user");
        userRoleUser.setFirstName("User");
        userRoleUser.setLastName("User");
        userRoleUser.setEmail("user@localhost");
        userRoleUser.setMobileNo("15000899479");
        // Raw password: user
        userRoleUser.setPasswordHash(passwordEncoder.encode("user"));
        userRoleUser.setActivated(true);
        userRoleUser.setActivationKey(null);
        userRoleUser.setResetKey(null);
        userRoleUser.setResetTime(null);
        userRoleUser.setEnabled(true);
        mongoTemplate.save(userRoleUser);

        mongoTemplate.save(new UserAuthority(userRoleUser.getId(), Authority.USER));

        // Creates 'admin' user and corresponding authorities
        User adminRoleUser = new User();
        adminRoleUser.setUserName("admin");
        adminRoleUser.setFirstName("Admin");
        adminRoleUser.setLastName("Admin");
        adminRoleUser.setEmail("admin@localhost");
        adminRoleUser.setMobileNo("15000899477");
        // Raw password: admin
        adminRoleUser.setPasswordHash(passwordEncoder.encode("admin"));
        adminRoleUser.setActivated(true);
        adminRoleUser.setActivationKey(null);
        adminRoleUser.setResetKey(null);
        adminRoleUser.setResetTime(null);
        adminRoleUser.setEnabled(true);
        mongoTemplate.save(adminRoleUser);

        mongoTemplate.save(new UserAuthority(adminRoleUser.getId(), Authority.USER));
        mongoTemplate.save(new UserAuthority(adminRoleUser.getId(), Authority.ADMIN));

        // Creates 'system' user and corresponding authorities
        User adminRoleSystemUser = new User();
        adminRoleSystemUser.setUserName("system");
        adminRoleSystemUser.setFirstName("System");
        adminRoleSystemUser.setLastName("System");
        adminRoleSystemUser.setEmail("system@localhost");
        adminRoleSystemUser.setMobileNo("15000899422");
        // Raw password: system
        adminRoleSystemUser.setPasswordHash(passwordEncoder.encode("system"));
        adminRoleSystemUser.setActivated(true);
        adminRoleSystemUser.setActivationKey(null);
        adminRoleSystemUser.setResetKey(null);
        adminRoleSystemUser.setResetTime(null);
        adminRoleSystemUser.setEnabled(true);
        mongoTemplate.save(adminRoleSystemUser);

        mongoTemplate.save(new UserAuthority(adminRoleSystemUser.getId(), Authority.USER));
        mongoTemplate.save(new UserAuthority(adminRoleSystemUser.getId(), Authority.ADMIN));

        // Creates 'louis' user and corresponding authorities
        User developerRoleUser = new User();
        developerRoleUser.setUserName("louis");
        developerRoleUser.setFirstName("Louis");
        developerRoleUser.setLastName("Lau");
        developerRoleUser.setEmail("louis@pm6422.club");
        developerRoleUser.setMobileNo("15000899488");
        // Raw password: louis
        developerRoleUser.setPasswordHash(passwordEncoder.encode("louis"));
        developerRoleUser.setActivated(true);
        developerRoleUser.setActivationKey(null);
        developerRoleUser.setResetKey(null);
        developerRoleUser.setResetTime(null);
        developerRoleUser.setEnabled(true);
        mongoTemplate.save(developerRoleUser);

        mongoTemplate.save(new UserAuthority(developerRoleUser.getId(), Authority.USER));
        mongoTemplate.save(new UserAuthority(developerRoleUser.getId(), Authority.ADMIN));
        mongoTemplate.save(new UserAuthority(developerRoleUser.getId(), Authority.DEVELOPER));
    }

    @ChangeSet(order = "05", author = "Louis", id = "addOAuth2ClientDetails", runAlways = true)
    public void addOAuth2ClientDetails(MongockTemplate mongoTemplate) {
        MongoOAuth2ClientDetails oAuth2ClientDetails = new MongoOAuth2ClientDetails();
        oAuth2ClientDetails.setClientId(MongoOAuth2ClientDetails.INTERNAL_CLIENT_ID);
        oAuth2ClientDetails.setRawClientSecret(MongoOAuth2ClientDetails.INTERNAL_RAW_CLIENT_SECRET);
        oAuth2ClientDetails.setClientSecret(
                new BCryptPasswordEncoder().encode(MongoOAuth2ClientDetails.INTERNAL_RAW_CLIENT_SECRET));
        oAuth2ClientDetails.setScope(Arrays.asList("read", "write"));
        // It will auto approve if autoApproveScopes exactly match the scopes.
        oAuth2ClientDetails.setAutoApproveScopes(Collections.singletonList("read"));
        oAuth2ClientDetails.setAuthorizedGrantTypes(
                Arrays.asList("password", "authorization_code", "refresh_token", "client_credentials"));
        // Note: localhost and 127.0.0.1 must be saved twice.
        oAuth2ClientDetails.setRegisteredRedirectUri(
                new HashSet<>(Arrays.asList("http://127.0.0.1:9020/login", "http://localhost:9020/login")));
        oAuth2ClientDetails.setAccessTokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(7));
        oAuth2ClientDetails.setRefreshTokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(7));
        // 这个authority还不知道其作用
        oAuth2ClientDetails.setAuthorities(Arrays.asList(new SimpleGrantedAuthority(Authority.DEVELOPER),
                new SimpleGrantedAuthority(Authority.ADMIN), new SimpleGrantedAuthority(Authority.USER),
                new SimpleGrantedAuthority(Authority.ANONYMOUS)));
        mongoTemplate.save(oAuth2ClientDetails);
    }

    @ChangeSet(order = "06", author = "Louis", id = "addScheduledTasks", runAlways = true)
    public void addScheduledTasks(MongockTemplate mongoTemplate) {
        RpcScheduledTask rpcScheduledTask = new RpcScheduledTask();
        rpcScheduledTask.setName("T" + IdGenerator.generateShortId());
        rpcScheduledTask.setRegistryIdentity("zookeeper://localhost:2181/registry");
        rpcScheduledTask.setInterfaceName(RpcApplicationService.class.getName());
        rpcScheduledTask.setForm(StringUtils.EMPTY);
        rpcScheduledTask.setVersion(StringUtils.EMPTY);
        rpcScheduledTask.setMethodName("updateStatus");
        rpcScheduledTask.setMethodSignature("updateStatus(void)");
        rpcScheduledTask.setMethodParamTypes(new String[0]);
        rpcScheduledTask.setFixedInterval(2L);
        rpcScheduledTask.setFixedIntervalUnit(UNIT_MINUTES);
        rpcScheduledTask.setEnabled(true);
        mongoTemplate.save(rpcScheduledTask);
    }
}
