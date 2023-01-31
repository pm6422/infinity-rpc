package com.luixtech.rpc.spring.boot.starter.bean;

import com.luixtech.rpc.core.client.annotation.RpcConsumer;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.spring.boot.starter.config.LuixProperties;
import com.luixtech.rpc.spring.boot.starter.utils.AnnotationBeanDefinitionUtils;
import com.luixtech.rpc.spring.boot.starter.utils.AnnotationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static com.luixtech.rpc.core.client.stub.ConsumerStub.buildConsumerStubBeanName;
import static com.luixtech.rpc.core.constant.ConsumerConstants.*;
import static com.luixtech.rpc.core.constant.ProtocolConstants.PROTOCOL;
import static com.luixtech.rpc.core.constant.ProtocolConstants.SERIALIZER;
import static com.luixtech.rpc.spring.boot.starter.utils.ProxyUtils.getTargetClass;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;


/**
 * Scan all spring bean to discover the fields and method annotated with {@link RpcConsumer} annotation
 * and injected with the proxyInstance.
 * The class implements {@link BeanPostProcessor} means that all spring beans will be processed by
 * {@link ConsumerBeanPostProcessor#postProcessBeforeInitialization(Object, String)} after initialized bean
 * <p>
 * BeanPostProcessor: Factory hook that allows for custom modification of new bean instances —
 * for example, checking for marker interfaces or wrapping beans with proxies.
 */
@Slf4j
public class ConsumerBeanPostProcessor implements BeanPostProcessor, EnvironmentAware, BeanFactoryAware {

    private final List<String>               scanBasePackages;
    private       Environment                env;
    /**
     * {@link DefaultListableBeanFactory} can register bean definition
     */
    private       DefaultListableBeanFactory beanFactory;

    public ConsumerBeanPostProcessor(List<String> scanBasePackages) {
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setEnvironment(@NonNull Environment env) {
        this.env = env;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory,
                "It requires an instance of ".concat(DefaultListableBeanFactory.class.getSimpleName()));
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    /**
     * Inject RPC consumer proxy and register {@link ConsumerStub} instance
     *
     * @param bean     bean instance to be injected
     * @param beanName bean name to be injected
     * @return processed bean instance
     * @throws BeansException if BeansException throws
     */
    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> clazz = getTargetClass(bean);

        if (!matchScanPackages(clazz)) {
            return bean;
        }

        // Inject consumer proxy instances to fields
        injectConsumerToField(bean, clazz);

        // Inject consumer proxy instances to method parameters
        injectConsumerToMethodParam(bean, clazz);
        return bean;
    }

    private boolean matchScanPackages(Class<?> clazz) {
        return scanBasePackages
                .stream()
                .map(x -> env.resolvePlaceholders(x))
                .anyMatch(pkg -> clazz.getName().startsWith(pkg));
    }

    /**
     * Inject RPC consumer proxy instances to fields which annotated with {@link RpcConsumer} by reflection
     * and register its {@link ConsumerStub} instance to spring context
     *
     * @param bean      bean instance to be injected
     * @param beanClass bean class to be injected
     */
    private void injectConsumerToField(Object bean, Class<?> beanClass) {
        Field[] fields = beanClass.getDeclaredFields();
        Arrays.stream(fields).filter(this::isConsumerAnnotatedField).forEach(field -> {
            try {
                RpcConsumer rpcConsumerAnnotation = field.getAnnotation(RpcConsumer.class);
                if (rpcConsumerAnnotation == null) {
                    // No @Consumer annotated field found
                    return;
                }
                AnnotationAttributes attributes = getConsumerAnnotationAttributes(field);
                // Register consumer stub instance to spring context
                ConsumerStub<?> consumerStub = registerConsumerStub(rpcConsumerAnnotation, attributes, field.getType());
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                // Inject RPC consumer proxy instance
                field.set(bean, consumerStub.getProxyInstance());
            } catch (Throwable t) {
                throw new BeanInitializationException("Failed to inject RPC consumer proxy to field [" + field.getName()
                        + "] of " + bean.getClass().getName(), t);
            }
        });
    }

    /**
     * Inject RPC consumer proxy instances to setter method parameters which annotated with {@link RpcConsumer} by reflection
     * and register its {@link ConsumerStub} instance to spring context
     *
     * @param bean      bean instance to be injected
     * @param beanClass bean class to be injected
     */
    private void injectConsumerToMethodParam(Object bean, Class<?> beanClass) {
        Method[] methods = beanClass.getMethods();
        Arrays.stream(methods).filter(this::isConsumerAnnotatedMethod).forEach(method -> {
            try {
                // The Java compiler generates the bridge method, in order to be compatible with the byte code
                // under previous JDK version of JDK 1.5, for the generic erasure occasion
                Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
                RpcConsumer rpcConsumerAnnotation = bridgedMethod.getAnnotation(RpcConsumer.class);
                if (rpcConsumerAnnotation == null) {
                    // No @Consumer annotated method found
                    return;
                }

                AnnotationAttributes attributes = getConsumerAnnotationAttributes(bridgedMethod);
                // Register consumer stub instance to spring context
                ConsumerStub<?> consumerStub = registerConsumerStub(rpcConsumerAnnotation, attributes, method.getParameterTypes()[0]);
                // Inject RPC consumer proxy instance
                method.invoke(bean, consumerStub.getProxyInstance());
            } catch (Throwable t) {
                throw new BeanInitializationException("Failed to inject RPC consumer proxy to parameter of method ["
                        + method.getName() + "] of " + bean.getClass().getName(), t);
            }
        });
    }

    private boolean isConsumerAnnotatedField(Field field) {
        return !Modifier.isStatic(field.getModifiers())
                && field.isAnnotationPresent(RpcConsumer.class);
    }

    private boolean isConsumerAnnotatedMethod(Method method) {
        return method.getName().startsWith("set")
                && method.getParameterTypes().length == 1
                && Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.isAnnotationPresent(RpcConsumer.class);
    }

    private AnnotationAttributes getConsumerAnnotationAttributes(AnnotatedElement element) {
        return AnnotationUtils.getAnnotationAttributes(element, RpcConsumer.class, env, true, true);
    }

    /**
     * Register consumer stub to spring context
     *
     * @param rpcConsumerAnnotation  {@link RpcConsumer} annotation
     * @param attributes             {@link AnnotationAttributes annotation attributes}
     * @param consumerInterfaceClass Consumer interface class
     * @return ConsumerStub instance
     */
    private ConsumerStub<?> registerConsumerStub(RpcConsumer rpcConsumerAnnotation,
                                                 AnnotationAttributes attributes,
                                                 Class<?> consumerInterfaceClass) {
        // Resolve the interface class of the consumer proxy instance
        Class<?> resolvedConsumerInterfaceClass = AnnotationUtils.resolveInterfaceClass(attributes, consumerInterfaceClass);

        // Build the consumer stub bean name
        String consumerStubBeanName = buildConsumerStubBeanName(resolvedConsumerInterfaceClass.getName(), attributes);
        if (!existsConsumerStub(consumerStubBeanName)) {
            AbstractBeanDefinition stubBeanDefinition =
                    buildConsumerStubDefinition(consumerStubBeanName, consumerInterfaceClass, rpcConsumerAnnotation);
            beanFactory.registerBeanDefinition(consumerStubBeanName, stubBeanDefinition);
            log.info("Registered RPC consumer stub [{}] to spring context", consumerStubBeanName);
        }
        // Method getBean() will trigger bean initialization
        return beanFactory.getBean(consumerStubBeanName, ConsumerStub.class);
    }

    private boolean existsConsumerStub(String consumerStubBeanName) {
        return beanFactory.containsBeanDefinition(consumerStubBeanName);
    }

    /**
     * Build {@link ConsumerStub} definition
     *
     * @param beanName       consumer stub bean name
     * @param interfaceClass consumer interface class
     * @param annotation     {@link RpcConsumer} annotation
     * @return {@link ConsumerStub} bean definition
     */
    private AbstractBeanDefinition buildConsumerStubDefinition(String beanName,
                                                               Class<?> interfaceClass,
                                                               RpcConsumer annotation) {
        // Create and load luixProperties bean
        LuixProperties luixProperties = beanFactory.getBean(LuixProperties.class);
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ConsumerStub.class);

        AnnotationBeanDefinitionUtils.addPropertyValue(builder, BEAN_NAME, beanName);
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, INTERFACE_CLASS, interfaceClass);
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, INTERFACE_NAME, interfaceClass.getName());

        String protocol = defaultIfEmpty(annotation.protocol(), luixProperties.getProtocol().getName());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, PROTOCOL, protocol);

        String serializer = defaultIfEmpty(annotation.serializer(), luixProperties.getProtocol().getSerializer());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, SERIALIZER, serializer);

        String form = defaultIfEmpty(annotation.form(), luixProperties.getConsumer().getForm());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, FORM, form);

        String version = defaultIfEmpty(annotation.version(), luixProperties.getConsumer().getVersion());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, VERSION, version);

        String invoker = defaultIfEmpty(annotation.invoker(), luixProperties.getConsumer().getInvoker());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, INVOKER, invoker);

        String faultTolerance = defaultIfEmpty(annotation.faultTolerance(), luixProperties.getConsumer().getFaultTolerance());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, FAULT_TOLERANCE, faultTolerance);

        String loadBalancer = defaultIfEmpty(annotation.loadBalancer(), luixProperties.getConsumer().getLoadBalancer());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, LOAD_BALANCER, loadBalancer);

        String proxyFactory = defaultIfEmpty(annotation.proxyFactory(), luixProperties.getConsumer().getProxyFactory());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, PROXY, proxyFactory);

        Integer requestTimeout = StringUtils.isEmpty(annotation.requestTimeout())
                ? luixProperties.getConsumer().getRequestTimeout() : Integer.valueOf(annotation.requestTimeout());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, REQUEST_TIMEOUT, requestTimeout);

        Integer retryCount = StringUtils.isEmpty(annotation.retryCount())
                ? luixProperties.getConsumer().getRetryCount() : Integer.valueOf(annotation.retryCount());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, RETRY_COUNT, retryCount);

        AnnotationBeanDefinitionUtils.addPropertyValue(builder, LIMIT_RATE, luixProperties.getConsumer().isLimitRate());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, MAX_PAYLOAD, luixProperties.getConsumer().getMaxPayload());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, PROVIDER_ADDRESSES, annotation.providerAddresses(), env);

        return builder.getBeanDefinition();
    }

    /**
     * @param bean     bean instance
     * @param beanName bean name
     * @return bean instance
     * @throws BeansException if any {@link BeansException} thrown
     */
    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        return bean;
    }
}
