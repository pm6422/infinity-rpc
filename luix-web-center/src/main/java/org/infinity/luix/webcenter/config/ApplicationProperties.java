package org.infinity.luix.webcenter.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.cors.CorsConfiguration;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Properties specific to Application.
 *
 * <p>
 * Properties are configured in the application.yml file.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Validated
@Getter
public class ApplicationProperties {
    private final Http               http               = new Http();
    private final Swagger            swagger            = new Swagger();
    private final Metrics            metrics            = new Metrics();
    private final AopLogging         aopLogging         = new AopLogging();
    private final ElapsedTimeLogging elapsedTimeLogging = new ElapsedTimeLogging();
    private final CorsConfiguration  cors               = new CorsConfiguration();
    private final UserEventAudit     userEventAudit     = new UserEventAudit();
    private final Account            account            = new Account();
    private final Ribbon             ribbon             = new Ribbon();
    private final Security           security           = new Security();

    @Data
    public static class Http {
        private final Cache cache = new Cache();

        @Data
        public static class Cache {
            /**
             * Expired days
             */
            private Long expiredAfter = 31L;
        }
    }

    @Data
    public static class Swagger {
        private       boolean enabled;
        private       String  version;
        private       String  termsOfServiceUrl;
        private       String  contactName;
        private       String  contactUrl;
        private       String  contactEmail;
        private       String  license;
        private       String  licenseUrl;
        private       String  host;
        private final Api     api     = new Api();
        private final OpenApi openApi = new OpenApi();

        @Data
        public static class Api {
            private String title;
            private String description;
        }

        @Data
        public static class OpenApi {
            private String title;
            private String description;
        }
    }

    @Data
    public static class Metrics {
        private final Logs     logs     = new Logs();
        private final Graphite graphite = new Graphite();

        @Data
        public static class Spark {
            private boolean enabled = false;
            private String  host    = "localhost";
            private int     port    = 9999;
        }

        @Data
        public static class Graphite {
            private boolean enabled = false;
            private String  host    = "localhost";
            private int     port    = 2003;
            private String  prefix  = "";
        }

        @Data
        public static class Logs {
            private boolean enabled         = false;
            private int     reportFrequency = 60;
        }
    }

    @Data
    public static class AopLogging {
        private boolean      enabled;
        private boolean      methodWhitelistMode;
        private List<String> methodWhitelist;
    }

    @Data
    public static class ElapsedTimeLogging {
        private boolean enabled;
        private int     slowExecutionThreshold;
    }

    @Data
    public static class UserEventAudit {
        private boolean enabled;
    }

    @Data
    public static class Account {
        private String defaultPassword;
    }

    @Data
    public static class Ribbon {
        private String[] displayOnActiveProfiles;
    }

    @Data
    public static class Security {
        private String              contentSecurityPolicy;
        private ClientAuthorization clientAuthorization = new ClientAuthorization();
        private Authentication      authentication      = new Authentication();
        private RememberMe          rememberMe          = new RememberMe();
        private OAuth2              oauth2              = new OAuth2();

        @Data
        public static class ClientAuthorization {
            private String accessTokenUri;
            private String tokenServiceId;
            private String clientId;
            private String clientSecret;
        }

        @Data
        public static class Authentication {
            private Jwt jwt = new Jwt();

            @Data
            public static class Jwt {
                private String secret;
                private String base64Secret;
                private long   tokenValidityInSeconds              = 1800; // 30 minutes
                private long   tokenValidityInSecondsForRememberMe = 2592000; // 30 days
            }
        }

        @Data
        public static class RememberMe {
            @NotNull
            private String key;
        }

        public static class OAuth2 {
            private List<String> audience = new ArrayList<>();

            public List<String> getAudience() {
                return Collections.unmodifiableList(audience);
            }

            public void setAudience(@NotNull List<String> audience) {
                this.audience.addAll(audience);
            }
        }
    }
}
