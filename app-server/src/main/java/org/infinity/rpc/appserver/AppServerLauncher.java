package org.infinity.rpc.appserver;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.infinity.rpc.appserver.config.ApplicationConstants;
import org.infinity.rpc.appserver.utils.NetworkIpUtils;
import org.infinity.rpc.core.annotation.EnableRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

@EnableRpc
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@Slf4j
public class AppServerLauncher {

    @Autowired
    private Environment env;

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(AppServerLauncher.class);
        Environment env = app.run(args).getEnvironment();
        printAppInfo(env);
    }

    private static void printAppInfo(Environment env) throws IOException {
        String appBanner = StreamUtils.copyToString(new ClassPathResource("config/banner-app.txt").getInputStream(),
                Charset.defaultCharset());
        log.info(appBanner, env.getProperty("spring.application.name"),
                StringUtils.isEmpty(env.getProperty("server.ssl.key-store")) ? "http" : "https",
                NetworkIpUtils.INTRANET_IP,
                env.getProperty("server.port"),
                StringUtils.defaultString(env.getProperty("server.servlet.context-path")),
                StringUtils.isEmpty(env.getProperty("server.ssl.key-store")) ? "http" : "https",
                NetworkIpUtils.INTERNET_IP,
                env.getProperty("server.port"),
                StringUtils.defaultString(env.getProperty("server.servlet.context-path")),
                org.springframework.util.StringUtils.arrayToCommaDelimitedString(env.getActiveProfiles()),
                env.getProperty("PID"),
                Charset.defaultCharset(),
                env.getProperty("LOG_PATH") + "-" + DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(new Date()) + ".log");
    }

    @PostConstruct
    private void validateProfiles() {
        Assert.notEmpty(env.getActiveProfiles(), "No Spring profile configured.");
        Arrays.asList(env.getActiveProfiles()).stream()
                .filter(activeProfile -> !ArrayUtils.contains(ApplicationConstants.AVAILABLE_PROFILES, activeProfile))
                .findFirst().ifPresent((activeProfile) -> {
            log.error("Misconfigured application with an illegal profile '{}'!", activeProfile);
            System.exit(0);
        });
    }
}
