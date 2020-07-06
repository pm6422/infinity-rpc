package org.infinity.rpc.core.config.spring.bean.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.infinity.rpc.core.config.spring.utils.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.BridgeMethodResolver.findBridgedMethod;
import static org.springframework.core.BridgeMethodResolver.isVisibilityBridgeMethodPair;

@Slf4j
@ThreadSafe
public abstract class AbstractAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
        implements MergedBeanDefinitionPostProcessor, EnvironmentAware {

    private final        Class<? extends Annotation>                       annotationType;
    private              Environment                                       environment;
    private final static int                                               CACHE_SIZE                            = 32;
    private final        ConcurrentMap<String, AnnotatedInjectionMetadata> annotatedInjectionMetadataPerBeanName = new ConcurrentHashMap<>(CACHE_SIZE);
    private final        ConcurrentMap<String, Object>                     injectedObjectsCache                  = new ConcurrentHashMap<>(CACHE_SIZE);

    /**
     * @param annotationType the multiple types of {@link Annotation annotations}
     */
    public AbstractAnnotationBeanPostProcessor(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    public final Class<? extends Annotation> getAnnotationType() {
        return annotationType;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    protected Environment getEnvironment() {
        return environment;
    }

    /**
     * Post-process the given merged bean definition for the specified bean
     *
     * @param beanDefinition the merged bean definition for the bean
     * @param beanType       the actual type of the managed bean instance
     * @param beanName       the name of the bean
     */
    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanType != null) {
            InjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
            metadata.checkConfigMembers(beanDefinition);
        }
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeanCreationException {
        InjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Failed to inject the [" + getAnnotationType().getSimpleName() + "] dependency!", ex);
        }
        return pvs;
    }

    /**
     * Find the annotation metadata info to be injected
     *
     * @param beanName
     * @param clazz
     * @param pvs
     * @return
     */
    private InjectionMetadata findInjectionMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = StringUtils.isNotEmpty(beanName) ? beanName : clazz.getName();
        // Quick check on the concurrent map first, with minimal locking.
        AnnotatedInjectionMetadata metadata = this.annotatedInjectionMetadataPerBeanName.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.annotatedInjectionMetadataPerBeanName) {
                // thread-safe atomic operation
                metadata = this.annotatedInjectionMetadataPerBeanName.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    try {
                        // Build the annotation metadata info based on annotated field and methods
                        metadata = buildAnnotatedMetadata(clazz);
                        // thread-safe atomic operation
                        this.annotatedInjectionMetadataPerBeanName.put(cacheKey, metadata);
                    } catch (NoClassDefFoundError err) {
                        throw new IllegalStateException("Failed to find annotation metadata info on class [" + clazz.getName() + "]", err);
                    }
                }
            }
        }
        return metadata;
    }

    /**
     * Build the annotation metadata info based on annotated fields and methods
     *
     * @param beanClass bean class used to find annotations
     * @return annotation metadata
     */
    private AnnotatedInjectionMetadata buildAnnotatedMetadata(final Class<?> beanClass) {
        Collection<AnnotatedFieldElement> fieldElements = findFieldAnnotationMetadata(beanClass);
        Collection<AnnotatedMethodElement> methodElements = findMethodAnnotationMetadata(beanClass);
        return new AnnotatedInjectionMetadata(beanClass, fieldElements, methodElements);
    }

    /**
     * Find annotation metadata info {@link AnnotatedFieldElement} from annotated fields
     *
     * @param beanClass bean class used to find annotations
     * @return found annotation metadata
     */
    private List<AnnotatedFieldElement> findFieldAnnotationMetadata(Class<?> beanClass) {
        List<AnnotatedFieldElement> annotatedFieldElements = new LinkedList<>();
        // Iterate all the fields of the bean class
        ReflectionUtils.doWithFields(beanClass, field -> {
            // Get the @Consumer annotation attributes value of field,
            // and it will be null if no @Consumer annotation presents on the field
            AnnotationAttributes annotationAttributes = getAnnotationAttributes(field, annotationType, getEnvironment(), true, true);
            if (annotationAttributes != null) {
                if (Modifier.isStatic(field.getModifiers())) {
                    log.warn("Annotation [{}] must NOT present on the static field [{}]", annotationType.getName(), field.getName());
                    return;
                }
                annotatedFieldElements.add(new AnnotatedFieldElement(field, annotationAttributes));
            }
        });
        return annotatedFieldElements;
    }

    /**
     * Find annotation metadata info {@link AnnotatedFieldElement} from annotated methods
     *
     * @param beanClass bean class used to find annotations
     * @return found annotation metadata
     */
    private List<AnnotatedMethodElement> findMethodAnnotationMetadata(final Class<?> beanClass) {
        List<AnnotatedMethodElement> elements = new LinkedList<>();
        // Iterate all the methods of the bean class
        ReflectionUtils.doWithMethods(beanClass, method -> {
            // The Java compiler generates the bridge method, in order to be compatible with the byte code under previous JDK version of JDK 1.5,
            // for the generic erasure occasion
            Method bridgedMethod = findBridgedMethod(method);
            if (!isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                return;
            }
            // Get the @Consumer annotation attributes value of field,
            // and it will be null if no @Consumer annotation presents on the field
            AnnotationAttributes annotationAttributes = getAnnotationAttributes(bridgedMethod, annotationType, getEnvironment(), true, true);
            if (annotationAttributes != null && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))) {
                if (Modifier.isStatic(method.getModifiers())) {
                    log.warn("Annotation [@{}] must NOT present on the static method [{}]", annotationType.getName(), method.getName());
                    return;
                }
                if (method.getParameterTypes().length == 0) {
                    log.warn("[@{}] annotated method must provider parameter", annotationType.getName());
                }
                PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, beanClass);
                elements.add(new AnnotatedMethodElement(method, pd, annotationAttributes));
            }
        });
        return elements;
    }

    /**
     * Get injected-object from specified {@link AnnotationAttributes annotation attributes} and Bean Class
     *
     * @param attributes      {@link AnnotationAttributes the annotation attributes}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link InjectionMetadata.InjectedElement}
     * @return An injected object
     * @throws Exception If getting is failed
     */
    protected Object getInjectedObject(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        String cacheKey = buildInjectedObjectCacheKey(attributes, bean, beanName, injectedType, injectedElement);

        Object injectedObject = injectedObjectsCache.get(cacheKey);

        if (injectedObject == null) {
            injectedObject = doGetInjectedBean(attributes, bean, beanName, injectedType, injectedElement);
            // Customized inject-object if necessary
            injectedObjectsCache.putIfAbsent(cacheKey, injectedObject);
        }

        return injectedObject;

    }

    /**
     * Build a cache key for injected-object. The context objects could help this method if
     * necessary :
     * <ul>
     * <li>{@link #getBeanFactory() BeanFactory}</li>
     * <li>{@link #getClassLoader() ClassLoader}</li>
     * <li>{@link #getEnvironment() Environment}</li>
     * </ul>
     *
     * @param attributes      {@link AnnotationAttributes the annotation attributes}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link InjectionMetadata.InjectedElement}
     * @return Bean cache key
     */
    protected abstract String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                          Class<?> injectedType,
                                                          InjectionMetadata.InjectedElement injectedElement);

    /**
     * Subclass must implement this method to get injected-object. The context objects could help this method if
     * necessary :
     * <ul>
     * <li>{@link #getBeanFactory() BeanFactory}</li>
     * <li>{@link #getClassLoader() ClassLoader}</li>
     * <li>{@link #getEnvironment() Environment}</li>
     * </ul>
     *
     * @param attributes      {@link AnnotationAttributes the annotation attributes}
     * @param bean            Current bean that will be injected
     * @param beanName        Current bean name that will be injected
     * @param injectedType    the type of injected-object
     * @param injectedElement {@link InjectionMetadata.InjectedElement}
     * @return The injected object
     * @throws Exception If resolving an injected object is failed.
     */
    protected abstract Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                                InjectionMetadata.InjectedElement injectedElement) throws Exception;

    /**
     * {@link Annotation Annotated} {@link InjectionMetadata} implementation
     */
    private class AnnotatedInjectionMetadata extends InjectionMetadata {
        private final Collection<AnnotatedFieldElement>  fieldElements;
        private final Collection<AnnotatedMethodElement> methodElements;

        public AnnotatedInjectionMetadata(Class<?> targetClass,
                                          Collection<AnnotatedFieldElement> fieldElements,
                                          Collection<AnnotatedMethodElement> methodElements) {
            super(targetClass, CollectionUtils.union(fieldElements, methodElements));
            this.fieldElements = fieldElements;
            this.methodElements = methodElements;
        }

        public Collection<AnnotatedFieldElement> getFieldElements() {
            return fieldElements;
        }

        public Collection<AnnotatedMethodElement> getMethodElements() {
            return methodElements;
        }
    }

    /**
     * {@link Annotation Annotated} {@link Method} {@link InjectionMetadata.InjectedElement}
     */
    private class AnnotatedMethodElement extends InjectionMetadata.InjectedElement {
        private final    Method               method;
        private final    AnnotationAttributes attributes;
        private volatile Object               object;

        protected AnnotatedMethodElement(Method method, PropertyDescriptor pd, AnnotationAttributes attributes) {
            super(method, pd);
            this.method = method;
            this.attributes = attributes;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            Class<?> injectedType = pd.getPropertyType();
            Object injectedObject = getInjectedObject(attributes, bean, beanName, injectedType, this);
            ReflectionUtils.makeAccessible(method);
            method.invoke(bean, injectedObject);
        }
    }

    /**
     * {@link Annotation Annotated} {@link Field} {@link InjectionMetadata.InjectedElement}
     */
    public class AnnotatedFieldElement extends InjectionMetadata.InjectedElement {
        private final    Field                field;
        private final    AnnotationAttributes attributes;
        private volatile Object               bean;

        protected AnnotatedFieldElement(Field field, AnnotationAttributes attributes) {
            super(field, null);
            this.field = field;
            this.attributes = attributes;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            Class<?> injectedType = field.getType();
            Object injectedObject = getInjectedObject(attributes, bean, beanName, injectedType, this);
            ReflectionUtils.makeAccessible(field);
            field.set(bean, injectedObject);
        }
    }
}
