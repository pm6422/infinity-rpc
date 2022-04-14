package com.luixtech.rpc.webcenter.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.MetricsServlet;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.annotation.Resource;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;

import static java.net.URLDecoder.decode;

/**
 * Web application configuration
 */
@Configuration
@Slf4j
public class WebConfigurer implements ServletContextInitializer, WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

    @Resource
    private Environment           env;
    @Resource
    private ApplicationProperties applicationProperties;
    @Resource
    private MetricRegistry        metricRegistry;

    @Override
    public void onStartup(ServletContext servletContext) {
        EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);
        initMetrics(servletContext, types);
    }

    /**
     * Customize the Servlet engine: Mime types, the document root, the cache.
     */
    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        setWebSocketDeploymentInfo(factory);
        setMimeMappings(factory);
        if (Arrays.asList(env.getActiveProfiles()).contains(ApplicationConstants.SPRING_PROFILE_DEV)) {
            // When running in an IDE or with ./mvnw spring-boot:run, set location of the static web assets.
            setLocationForStaticAssets(factory);
        }
    }

    private void setWebSocketDeploymentInfo(UndertowServletWebServerFactory factory) {
        factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(false, 1024));
            deploymentInfo.addServletContextAttribute("io.undertow.websockets.jsr.WebSocketDeploymentInfo", webSocketDeploymentInfo);
        });
    }

    private void setMimeMappings(WebServerFactory factory) {
        if (factory instanceof ConfigurableServletWebServerFactory) {
            MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
            // IE issue, see https://github.com/jhipster/generator-jhipster/pull/711
            mappings.add("html", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
            // CloudFoundry issue, see https://github.com/cloudfoundry/gorouter/issues/64
            mappings.add("json", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
            ConfigurableServletWebServerFactory servletWebServer = (ConfigurableServletWebServerFactory) factory;
            servletWebServer.setMimeMappings(mappings);
        }
    }

    private void setLocationForStaticAssets(WebServerFactory factory) {
        if (factory instanceof ConfigurableServletWebServerFactory) {
            ConfigurableServletWebServerFactory servletWebServer = (ConfigurableServletWebServerFactory) factory;
            File root;
            String prefixPath = resolvePathPrefix();
            root = new File(prefixPath + "src/main/webapp/");
            if (root.exists() && root.isDirectory()) {
                servletWebServer.setDocumentRoot(root);
            }
        }
    }

    /**
     * Resolve path prefix to static resources.
     */
    private String resolvePathPrefix() {
        String fullExecutablePath;
        try {
            fullExecutablePath = decode(this.getClass().getResource("").getPath(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            /* try without decoding if this ever happens */
            fullExecutablePath = this.getClass().getResource("").getPath();
        }
        String rootPath = Paths.get(".").toUri().normalize().getPath();
        String extractedPath = fullExecutablePath.replace(rootPath, "");
        int extractionEndIndex = extractedPath.indexOf("target/");
        if (extractionEndIndex <= 0) {
            return "";
        }
        return extractedPath.substring(0, extractionEndIndex);
    }

    /**
     * Initializes Metrics.
     */
    private void initMetrics(ServletContext servletContext, EnumSet<DispatcherType> disps) {
        servletContext.setAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE, metricRegistry);
        servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
        log.info("Initialized metrics registry");

        FilterRegistration.Dynamic metricsFilter = servletContext.addFilter("webappMetricsFilter",
                new InstrumentedFilter());
        metricsFilter.addMappingForUrlPatterns(disps, true, "/*");
        metricsFilter.setAsyncSupported(true);
        log.info("Registered metrics filter");

        ServletRegistration.Dynamic metricsAdminServlet = servletContext.addServlet("metricsServlet",
                new MetricsServlet());
        // Visit the following address to get more metrics info
        // http://localhost:9010/api/metrics?access_token=7d305527-d4cc-4555-9de4-77fdd345d3a5
        metricsAdminServlet.addMapping("/api/metrics/*");
        metricsAdminServlet.setAsyncSupported(true);
        metricsAdminServlet.setLoadOnStartup(2);
        log.info("Registered metrics servlet");
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = applicationProperties.getCors();
        if (CollectionUtils.isNotEmpty(config.getAllowedOrigins()) ||
                CollectionUtils.isNotEmpty(config.getAllowedOriginPatterns())) {
            source.registerCorsConfiguration("/api/**", config);
            source.registerCorsConfiguration("/management/**", config);
            source.registerCorsConfiguration("/v2/api-docs", config);
            source.registerCorsConfiguration("/v3/api-docs", config);
            source.registerCorsConfiguration("/swagger-resources", config);
            source.registerCorsConfiguration("/swagger-ui/**", config);
            log.debug("Registered CORS filter");
        }
        return new CorsFilter(source);
    }
}