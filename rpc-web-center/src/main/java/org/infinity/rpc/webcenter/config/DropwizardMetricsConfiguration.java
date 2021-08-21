package org.infinity.rpc.webcenter.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.*;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Dropwizard metrics used to monitor and collect data for one node, but it can NOT
 * aggregate data for all nodes
 */
@Slf4j
@Configuration
//@EnableMetrics(proxyTargetClass = true)
public class DropwizardMetricsConfiguration extends MetricsConfigurerAdapter {

    private static final String                PROP_METRIC_REG_JVM_MEMORY  = "jvm.memory";
    private static final String                PROP_METRIC_REG_JVM_GARBAGE = "jvm.garbage";
    private static final String                PROP_METRIC_REG_JVM_THREADS = "jvm.threads";
    private static final String                PROP_METRIC_REG_JVM_FILES   = "jvm.files";
    private static final String                PROP_METRIC_REG_JVM_BUFFERS = "jvm.buffers";
    private              MetricRegistry        metricRegistry              = new MetricRegistry();
    private              HealthCheckRegistry   healthCheckRegistry         = new HealthCheckRegistry();
    @Resource
    private              ApplicationProperties applicationProperties;

    @Bean
    @Override
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    @Bean
    @Override
    public HealthCheckRegistry getHealthCheckRegistry() {
        return healthCheckRegistry;
    }

    @PostConstruct
    public void init() {
        metricRegistry.register(PROP_METRIC_REG_JVM_MEMORY, new MemoryUsageGaugeSet());
        metricRegistry.register(PROP_METRIC_REG_JVM_GARBAGE, new GarbageCollectorMetricSet());
        metricRegistry.register(PROP_METRIC_REG_JVM_THREADS, new ThreadStatesGaugeSet());
        metricRegistry.register(PROP_METRIC_REG_JVM_FILES, new FileDescriptorRatioGauge());
        metricRegistry.register(PROP_METRIC_REG_JVM_BUFFERS, new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
        log.info("Registered JVM gauge");

        if (applicationProperties.getMetrics().getLogs().isEnabled()) {
            Marker metricsMarker = MarkerFactory.getMarker("metrics");
            final Slf4jReporter reporter = Slf4jReporter.forRegistry(metricRegistry)
                    .outputTo(LoggerFactory.getLogger("metrics")).markWith(metricsMarker)
                    .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
            reporter.start(applicationProperties.getMetrics().getLogs().getReportFrequency(), TimeUnit.SECONDS);
            log.info("Registered metrics log reporting");
        }
    }

    @Configuration
    @ConditionalOnClass(Graphite.class)
    @Slf4j
    public static class GraphiteRegistry {
        @Resource
        private ApplicationProperties applicationProperties;
        @Resource
        private MetricRegistry        metricRegistry;

        @PostConstruct
        private void init() {
            if (applicationProperties.getMetrics().getGraphite().isEnabled()) {
                String graphiteHost = applicationProperties.getMetrics().getGraphite().getHost();
                Integer graphitePort = applicationProperties.getMetrics().getGraphite().getPort();
                String graphitePrefix = applicationProperties.getMetrics().getGraphite().getPrefix();
                Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
                GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
                        .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
                        .prefixedWith(graphitePrefix).build(graphite);
                graphiteReporter.start(1, TimeUnit.MINUTES);
                log.info("Initialized metrics graphite reporting");
            }
        }
    }
}
