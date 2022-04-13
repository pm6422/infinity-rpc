package com.luixtech.luixrpc.spring.boot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.luixrpc.spring.boot.bean.ConsumerBeanPostProcessor;
import com.luixtech.luixrpc.spring.boot.bean.ProviderBeanDefinitionRegistryPostProcessor;
import com.luixtech.luixrpc.spring.boot.bean.registry.AnnotatedBeanDefinitionRegistry;
import com.luixtech.luixrpc.spring.boot.config.RpcAutoConfiguration;
import com.luixtech.luixrpc.spring.boot.startup.RpcLifecycleApplicationListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Bean definition registrar used to register {@link ProviderBeanDefinitionRegistryPostProcessor},
 * {@link ConsumerBeanPostProcessor} and common RPC auto configurations {@link RpcAutoConfiguration}
 */
@Slf4j
public class RpcServiceScanRegistrar implements ImportBeanDefinitionRegistrar {

    /**
     * Register service providers and consumers bean definitions
     *
     * @param importingClassMetadata annotation metadata of the importing class
     * @param registry               current bean definition registry
     */
    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                        @NonNull BeanDefinitionRegistry registry) {
        List<String> scanBasePackages = getScanBasePackages(importingClassMetadata);
        registerRpcAutoConfiguration(registry);
        registerRpcLifecycleApplicationListener(registry);
        registerProviderBeanDefinitionRegistryPostProcessor(registry, scanBasePackages);
        registerConsumerBeanPostProcessor(registry, scanBasePackages);
    }

    /**
     * Get the packages to be scanned for service providers and consumers
     *
     * @param metadata annotation metadata
     * @return packages to be scanned
     */
    private List<String> getScanBasePackages(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableLuixRpc.class.getName()));
        String[] scanBasePackages = Objects.requireNonNull(attributes).getStringArray("scanBasePackages");
        // Remove duplicated packages
        List<String> packagesToScan = Arrays
                .stream(scanBasePackages)
                .map(StringUtils::trim)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(packagesToScan)) {
            String packageName = ClassUtils.getPackageName(metadata.getClassName());
            log.debug("Default RPC provider and consumer scan base package: [{}]", packageName);
            Assert.hasText(packageName, "No RPC provider and consumer scan base package!");
            return Collections.singletonList(packageName);
        } else {
            Assert.notEmpty(packagesToScan, "User defined RPC provider and consumer scan base packages must NOT be empty!");
            log.debug("User defined RPC provider and consumer scan base packages: [{}]", packagesToScan);
        }
        return packagesToScan;
    }

    /**
     * Register bean of RPC auto configuration
     *
     * @param registry current bean definition registry
     */
    private void registerRpcAutoConfiguration(BeanDefinitionRegistry registry) {
        AnnotatedBeanDefinitionRegistry.registerBeans(registry, RpcAutoConfiguration.class);
    }

    /**
     * Register bean of RPC lifecycle listener
     *
     * @param registry current bean definition registry
     */
    private void registerRpcLifecycleApplicationListener(BeanDefinitionRegistry registry) {
        AnnotatedBeanDefinitionRegistry.registerBeans(registry, RpcLifecycleApplicationListener.class);
    }

    /**
     * Register bean definition of service providers with @Provider annotation
     *
     * @param registry         current bean definition registry
     * @param scanBasePackages packages to be scanned
     */
    private void registerProviderBeanDefinitionRegistryPostProcessor(BeanDefinitionRegistry registry,
                                                                     List<String> scanBasePackages) {
        registerBeanDefinition(registry, ProviderBeanDefinitionRegistryPostProcessor.class, scanBasePackages);
    }

    /**
     * Register bean definition of service consumers with @Consumer annotation
     *
     * @param registry         current bean definition registry
     * @param scanBasePackages packages to be scanned
     */
    private void registerConsumerBeanPostProcessor(BeanDefinitionRegistry registry, List<String> scanBasePackages) {
        registerBeanDefinition(registry, ConsumerBeanPostProcessor.class, scanBasePackages);
    }

    /**
     * @param scanBasePackages packages to be scanned
     * @param registry         current bean definition registry
     * @param beanType         class to be registered
     */
    private void registerBeanDefinition(BeanDefinitionRegistry registry, Class<?> beanType,
                                        List<String> scanBasePackages) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(beanType);
        if (scanBasePackages != null) {
            builder.addConstructorArgValue(scanBasePackages);
        }
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }
}
