package org.infinity.rpc.spring.boot.bean;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.ProviderConfig;
import org.infinity.rpc.core.server.annotation.Provider;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.spring.boot.bean.name.DefaultBeanNameGenerator;
import org.infinity.rpc.spring.boot.bean.registry.ClassPathBeanDefinitionRegistryScanner;
import org.infinity.rpc.spring.boot.utils.AnnotationUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.constant.ProtocolConstants.PROTOCOL;
import static org.infinity.rpc.core.constant.ServiceConstants.*;
import static org.infinity.rpc.core.server.stub.ProviderStub.buildProviderStubBeanName;
import static org.infinity.rpc.spring.boot.config.InfinityProperties.readProtocolConfig;
import static org.infinity.rpc.spring.boot.config.InfinityProperties.readProviderConfig;
import static org.infinity.rpc.spring.boot.utils.AnnotationBeanDefinitionUtils.addPropertyReference;
import static org.infinity.rpc.spring.boot.utils.AnnotationBeanDefinitionUtils.addPropertyValue;

/**
 * Register provider bean and provider stub under specified scan base packages to spring context
 * by {@link BeanDefinitionRegistry}
 */
@Slf4j
public class ProviderBeanDefinitionRegistryPostProcessor implements EnvironmentAware, BeanFactoryAware, ResourceLoaderAware,
        BeanClassLoaderAware, BeanDefinitionRegistryPostProcessor {

    private final Set<String>                scanBasePackages;
    private       ConfigurableEnvironment    env;
    private       ResourceLoader             resourceLoader;
    private       ClassLoader                classLoader;
    /**
     * {@link DefaultListableBeanFactory} can register bean definition
     */
    private       DefaultListableBeanFactory beanFactory;

    public ProviderBeanDefinitionRegistryPostProcessor(Set<String> scanBasePackages) {
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        this.env = (ConfigurableEnvironment) environment;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory,
                "It requires an instance of ".concat(DefaultListableBeanFactory.class.getSimpleName()));
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Modify the application context's internal bean definition registry after its
     * standard initialization. All regular bean definitions will have been loaded,
     * but no beans will have been instantiated yet. This allows for adding further
     * bean definitions before the next post-processing phase kicks in.
     *
     * @param registry the bean definition registry used by the application context
     * @throws org.springframework.beans.BeansException in case of errors
     */
    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
        // Register provider and provider stub beans
        registerProviderBeans(registry);
    }

    /**
     * Register provider and provider stub beans
     *
     * @param registry current bean definition registry
     */
    private void registerProviderBeans(BeanDefinitionRegistry registry) {
        Set<String> resolvedScanBasePackages = resolvePackagePlaceholders();
        if (CollectionUtils.isEmpty(resolvedScanBasePackages)) {
            log.warn("No package to be scanned for registering providers!");
            return;
        }
        doRegisterProviderBeans(registry, resolvedScanBasePackages);
    }

    /**
     * Resolve the placeholder in package name
     *
     * @return replaced packages
     */
    private Set<String> resolvePackagePlaceholders() {
        return scanBasePackages
                .stream()
                .filter(StringUtils::isNotEmpty)
                .map(x -> env.resolvePlaceholders(x.trim()))
                .collect(Collectors.toSet());
    }

    /**
     * Register provider and provider stub beans
     *
     * @param registry                 current bean definition registry
     * @param resolvedScanBasePackages provider packages to be scanned
     */
    private void doRegisterProviderBeans(BeanDefinitionRegistry registry, Set<String> resolvedScanBasePackages) {
        BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.create();
        ClassPathBeanDefinitionRegistryScanner providerScanner = createProviderScanner(registry, beanNameGenerator);

        resolvedScanBasePackages.forEach(scanBasePackage -> {
            // Register provider stub first
            boolean registered = registerProviderStubBeans(registry, beanNameGenerator, providerScanner, scanBasePackage);
            if (registered) {
                // Then register provider beans
                registerProviderBeans(providerScanner, scanBasePackage);
                log.info("Registered RPC provider instances to spring context");
            }
        });
    }

    /**
     * Create provider registry scanner which can found the below service
     *
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @return bean definition registry scanner
     *
     * <code>
     * '@Provider(maxRetries=1)
     * public class AppServiceImpl {
     * ...
     * ...
     * }'
     * </code>
     */
    private ClassPathBeanDefinitionRegistryScanner createProviderScanner(BeanDefinitionRegistry registry,
                                                                         BeanNameGenerator beanNameGenerator) {
        ClassPathBeanDefinitionRegistryScanner scanner = new ClassPathBeanDefinitionRegistryScanner(registry, env, resourceLoader);
        scanner.setBeanNameGenerator(beanNameGenerator);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));
        return scanner;
    }

    /**
     * Register provider beans with {@link Provider} annotation
     *
     * @param providerScanner provider bean definition registry scanner
     * @param scanBasePackage provider packages to be scanned
     */
    private void registerProviderBeans(ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage) {
        // The 'scan' method can register @Provider bean instance to spring context
        providerScanner.scan(scanBasePackage);
    }

    /**
     * Register provider stub {@link ProviderStub} beans
     *
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @param providerScanner   provider bean definition registry scanner
     * @param scanBasePackage   provider packages to be scanned
     * @return {@code true} if it was registered and {@code false} otherwise
     */
    private boolean registerProviderStubBeans(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator,
                                              ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage) {
        // Next we need to register ProviderBean which is the stub of service provider to spring context
        Set<BeanDefinitionHolder> holders =
                findProviderBeanDefinitionHolders(providerScanner, scanBasePackage, registry, beanNameGenerator);
        if (CollectionUtils.isEmpty(holders)) {
            return false;
        }
        holders.forEach(holder -> registerProviderStubBean(holder, registry, providerScanner));
        return true;
    }

    /**
     * Find already registered provider bean definitions
     *
     * @param providerScanner   provider bean definition registry scanner
     * @param scanBasePackage   provider packages to be scanned
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @return provider bean definition holders
     */
    private Set<BeanDefinitionHolder> findProviderBeanDefinitionHolders(ClassPathBeanDefinitionRegistryScanner providerScanner,
                                                                        String scanBasePackage,
                                                                        BeanDefinitionRegistry registry,
                                                                        BeanNameGenerator beanNameGenerator) {
        // Find the provider components satisfying the condition
        Set<BeanDefinition> beanDefinitions = providerScanner.findCandidateComponents(scanBasePackage);
        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<>(beanDefinitions.size());
        beanDefinitions.forEach(beanDefinition -> {
            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            beanDefinitionHolders.add(new BeanDefinitionHolder(beanDefinition, beanName));
        });
        return beanDefinitionHolders;
    }

    /**
     * Register {@link ProviderStub} beans
     *
     * @param providerBeanDefinitionHolder provider bean definition holders
     * @param registry                     current bean definition registry
     * @param providerScanner              provider bean definition registry scanner
     */
    private void registerProviderStubBean(BeanDefinitionHolder providerBeanDefinitionHolder,
                                          BeanDefinitionRegistry registry,
                                          ClassPathBeanDefinitionRegistryScanner providerScanner) {
        Class<?> providerInstanceClass = resolveProviderClass(providerBeanDefinitionHolder);
        Provider providerAnnotation = findProviderAnnotation(providerInstanceClass);
        Class<?> providerInterfaceClass = resolveProviderInterface(providerAnnotation, providerInstanceClass);

        String providerStubBeanName = buildProviderStubBeanName(providerInterfaceClass,
                providerAnnotation.form(), providerAnnotation.version());
        AbstractBeanDefinition stubBeanDefinition = buildProviderStubDefinition(providerStubBeanName,
                providerInterfaceClass, providerAnnotation, providerBeanDefinitionHolder.getBeanName());

        // Check duplicated candidate bean
        if (providerScanner.checkCandidate(providerStubBeanName, stubBeanDefinition)) {
            registry.registerBeanDefinition(providerStubBeanName, stubBeanDefinition);
            log.info("Registered RPC provider stub [{}] to spring context", providerStubBeanName);
        }
    }

    /**
     * Create provider class
     *
     * @param beanDefinitionHolder provider bean definition holders
     * @return provider class
     */
    private Class<?> resolveProviderClass(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return ClassUtils.resolveClassName(Objects.requireNonNull(beanClassName), classLoader);
    }

    /**
     * Get {@link Provider} annotation
     *
     * @param beanClass provider bean class
     * @return {@link Provider} annotation
     */
    private Provider findProviderAnnotation(Class<?> beanClass) {
        return beanClass.getAnnotation(Provider.class);
    }

    /**
     * Get provider interface class
     *
     * @param providerAnnotation    {@link Provider} annotation
     * @param providerInstanceClass provider instance class, e.g AppServiceImpl
     * @return provider interface
     */
    private Class<?> resolveProviderInterface(Provider providerAnnotation, Class<?> providerInstanceClass) {
        AnnotationAttributes annotationAttributes = AnnotationUtils
                .getAnnotationAttributes(providerInstanceClass, Provider.class, env, false, true);
        return AnnotationUtils.resolveInterfaceClass(annotationAttributes, providerInstanceClass);
    }

    /**
     * Build {@link ProviderStub} definition
     *
     * @param beanName             provider stub bean name
     * @param interfaceClass       provider interface class
     * @param annotation           {@link Provider} annotation
     * @param providerInstanceName provider instance name
     * @return {@link ProviderStub} bean definition
     */
    private AbstractBeanDefinition buildProviderStubDefinition(String beanName,
                                                               Class<?> interfaceClass,
                                                               Provider annotation,
                                                               String providerInstanceName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ProviderStub.class);
        ProtocolConfig protocolConfig = readProtocolConfig(env);
        ProviderConfig providerConfig = readProviderConfig(env);

        addPropertyValue(builder, BEAN_NAME, beanName);
        addPropertyValue(builder, INTERFACE_CLASS, interfaceClass);
        addPropertyValue(builder, INTERFACE_NAME, interfaceClass.getName());

        if (StringUtils.isEmpty(annotation.protocol())) {
            addPropertyValue(builder, PROTOCOL, protocolConfig.getName());
        } else {
            addPropertyValue(builder, PROTOCOL, annotation.protocol());
        }
        if (StringUtils.isEmpty(annotation.form())) {
            addPropertyValue(builder, FORM, providerConfig.getForm());
        } else {
            addPropertyValue(builder, FORM, annotation.form());
        }
        if (StringUtils.isEmpty(annotation.version())) {
            addPropertyValue(builder, VERSION, providerConfig.getVersion());
        } else {
            addPropertyValue(builder, VERSION, annotation.version());
        }
        if (StringUtils.isEmpty(annotation.healthChecker())) {
            addPropertyValue(builder, HEALTH_CHECKER, providerConfig.getHealthChecker());
        } else {
            addPropertyValue(builder, HEALTH_CHECKER, annotation.healthChecker());
        }
        if (Integer.MAX_VALUE == annotation.requestTimeout()) {
            addPropertyValue(builder, REQUEST_TIMEOUT, providerConfig.getRequestTimeout());
        } else {
            addPropertyValue(builder, REQUEST_TIMEOUT, annotation.requestTimeout());
        }
        if (Integer.MAX_VALUE == annotation.maxRetries()) {
            addPropertyValue(builder, MAX_RETRIES, providerConfig.getMaxRetries());
        } else {
            addPropertyValue(builder, MAX_RETRIES, annotation.maxRetries());
        }

        addPropertyValue(builder, MAX_PAYLOAD, providerConfig.getMaxPayload());

        // Obtain the instance by instance name then assign it to the property
        addPropertyReference(builder, "instance", providerInstanceName, env);
        return builder.getBeanDefinition();
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Leave blank intentionally
    }
}