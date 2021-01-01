package org.infinity.rpc.utilities.spi;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility used to load a specified implementation of a service interface.
 * It carries out similar functions as {@link java.util.ServiceLoader}
 * Service providers can be installed in an implementation of the Java platform in the form of
 * jar files placed into any of the usual extension directories. Providers can also be made available by adding them to the
 * application's class path or by some other platform-specific means.
 * <p>
 * Requirements:
 * _ The service provider interface must be an interface class, not a concrete or abstract class
 * _ The service provider implementation class must have a zero-argument constructor so that they can be instantiated during loading
 * - The service provider is identified by placing a configuration file in the resource directory META-INF/services/
 * - The configuration file must be encoded in UTF-8
 * - The configuration file's name is the fully-qualified name of service provider interface
 * - The configuration file's contents are the fully-qualified name of service provider implementation class
 *
 * @param <T>
 */
@Slf4j
@ThreadSafe
public class ServiceLoader<T> {
    /**
     * Service directory prefix
     */
    private static final String                        SERVICE_DIR_PREFIX          = "META-INF/services/";
    /**
     * Charset of the service configuration file
     */
    public static final  Charset                       SERVICE_CONFIG_FILE_CHARSET = StandardCharsets.UTF_8;
    /**
     * Cached used to store service loader instance associated with the service interface
     */
    private static final Map<String, ServiceLoader<?>> SERVICE_LOADERS_CACHE       = new ConcurrentHashMap<>();
    /**
     * Tab character
     */
    private static final String                        TAB                         = "\t";
    /**
     * The class loader used to locate, load and instantiate service
     */
    private final        ClassLoader                   classLoader;
    /**
     * The interface representing the service being loaded
     */
    private final        Class<T>                      serviceInterface;
    /**
     * The loaded service implementation classes associated with the SPI name
     */
    private final        Map<String, Class<T>>         serviceImplClasses;
    /**
     * The loaded service implementation singleton instances associated with the SPI name
     */
    private final        Map<String, T>                singletonInstances          = new ConcurrentHashMap<>();

    /**
     * Get the service loader associated with service interface type class
     *
     * @param serviceInterface provider interface class annotated @Spi annotation
     * @param <T>              service interface type
     * @return the specified singleton service loader instance
     */
    public static <T> ServiceLoader<T> forClass(Class<T> serviceInterface) {
        Validate.notNull(serviceInterface, "Service interface must not be null!");
        Validate.isTrue(serviceInterface.isInterface(), "Service interface must be an interface class!");
        Validate.isTrue(serviceInterface.isAnnotationPresent(Spi.class), "Service interface must be annotated with @Spi annotation!");

        return createServiceLoader(serviceInterface);
    }

    /**
     * Create a service loader or get it from cache if exists
     *
     * @param serviceInterface service interface
     * @param <T>              service interface type
     * @return service instance loader cache instance
     */
    @SuppressWarnings("unchecked")
    private static synchronized <T> ServiceLoader<T> createServiceLoader(Class<T> serviceInterface) {
        ServiceLoader<T> loader = (ServiceLoader<T>) SERVICE_LOADERS_CACHE.get(serviceInterface.getName());
        if (loader == null) {
            loader = new ServiceLoader<>(Thread.currentThread().getContextClassLoader(), serviceInterface);
            SERVICE_LOADERS_CACHE.put(serviceInterface.getName(), loader);
        }
        return loader;
    }

    /**
     * Prohibit instantiate an instance outside the class
     *
     * @param classLoader      class loader
     * @param serviceInterface service interface
     */
    private ServiceLoader(ClassLoader classLoader, Class<T> serviceInterface) {
        this.classLoader = classLoader;
        this.serviceInterface = serviceInterface;
        serviceImplClasses = loadImplClasses();
    }

    /**
     * Load service implementation class based on the service configuration file
     *
     * @return service implementation class map
     */
    private ConcurrentHashMap<String, Class<T>> loadImplClasses() {
        String serviceFileName = SERVICE_DIR_PREFIX.concat(serviceInterface.getName());
        List<String> serviceImplClassNames = new ArrayList<>();
        try {
            Enumeration<URL> urls = classLoader != null ? classLoader.getResources(serviceFileName) :
                    ClassLoader.getSystemResources(serviceFileName);
            if (CollectionUtils.sizeIsEmpty(urls)) {
                return new ConcurrentHashMap<>(0);
            }
            while (urls.hasMoreElements()) {
                readImplClassNames(urls.nextElement(), serviceInterface, serviceImplClassNames);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load the spi configuration file: ".concat(serviceFileName), e);
        }
        return loadImplClass(serviceImplClassNames);
    }

    /**
     * Read the service implementation class
     *
     * @param fileUrl          file resource url
     * @param serviceInterface service interface
     * @param implClassNames   service implementation class names
     */
    private void readImplClassNames(URL fileUrl, Class<T> serviceInterface, List<String> implClassNames) {
        int lineNum = 0;
        // try-with-resource statement can automatically close the stream after use
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileUrl.openStream(), SERVICE_CONFIG_FILE_CHARSET))) {
            String line;
            // Read and assign value in one statement
            while ((line = reader.readLine()) != null) {
                readLine(fileUrl, line, ++lineNum, serviceInterface, implClassNames);
            }
        } catch (Exception e) {
            // Catch the exception and continue to read next line
            log.error("Failed to read the spi configuration file at line: " + lineNum, e);
        }
    }

    /**
     * Read line of the configuration file
     *
     * @param fileUrl          file resource url
     * @param line             line content
     * @param lineNum          line number
     * @param serviceInterface service interface
     * @param implClassNames   service implementation class names
     */
    private void readLine(URL fileUrl, String line, int lineNum, Class<T> serviceInterface, List<String> implClassNames) {
        int poundSignIdx = line.indexOf('#');
        if (poundSignIdx >= 0) {
            // Get the line string without the comment suffix
            line = line.substring(0, poundSignIdx);
        }

        line = line.trim();
        if (StringUtils.isEmpty(line)) {
            // Skip comment line
            return;
        }

        Validate.isTrue(!line.contains(StringUtils.SPACE) && !line.contains(TAB),
                "Found illegal space or tab key at line: " + lineNum + " of the file " + fileUrl);

        // Returns the character (Unicode code point) at the specified index
        // Codepoint of character 'a' is 97.
        // Codepoint of character 'b' is 98.
        int cp = line.codePointAt(0);
        // Determines if the character (Unicode code point) is permissible as the first character in a Java identifier.
        Validate.isTrue(Character.isJavaIdentifierStart(cp),
                "Found illegal service class name at line: " + lineNum + " of the file " + fileUrl);

        for (int i = Character.charCount(cp); i < line.length(); i += Character.charCount(cp)) {
            cp = line.codePointAt(i);
            Validate.isTrue(Character.isJavaIdentifierPart(cp) || cp == '.',
                    "Found illegal service class name at line: " + lineNum + " of the file " + fileUrl);
        }

        if (!implClassNames.contains(line)) {
            implClassNames.add(line);
        }
    }

    /**
     * Load the service implementation class associated with the interface class name
     *
     * @param implClassNames service implementation class name
     * @return spi name to service implementation class map
     */
    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Class<T>> loadImplClass(List<String> implClassNames) {
        if (CollectionUtils.isEmpty(implClassNames)) {
            return new ConcurrentHashMap<>(0);
        }
        ConcurrentHashMap<String, Class<T>> map = new ConcurrentHashMap<>(implClassNames.size());
        for (String implClassName : implClassNames) {
            try {
                Class<T> implClass;
                if (classLoader == null) {
                    implClass = (Class<T>) Class.forName(implClassName);
                } else {
                    implClass = (Class<T>) Class.forName(implClassName, true, classLoader);
                }
                log.debug("Loaded the service implementation [{}] of interface [{}]", implClassName,
                        serviceInterface.getName().substring(serviceInterface.getName().lastIndexOf(".") + 1));

                // Validate the implementation class
                checkServiceImplClass(implClass);

                // SPI service name, e.g, 'failover' strategy
                String spiName = getSpiServiceName(implClass);

                Validate.isTrue(!map.containsKey(spiName), "Found duplicated SPI name: " + spiName + " for " + implClass.getName());
                map.put(spiName, implClass);
            } catch (Exception e) {
                log.error("Failed to load the spi class: " + implClassName, e);
            }
        }
        return map;
    }

    private void checkServiceImplClass(Class<T> implClass) {
        Validate.isTrue(Modifier.isPublic(implClass.getModifiers()), implClass.getName() + " must be public!");
        Validate.isTrue(serviceInterface.isAssignableFrom(implClass), implClass.getName() + " must be the implementation of " + serviceInterface.getName());
        checkConstructor(implClass);
    }

    private void checkConstructor(Class<T> implClass) {
        Constructor<?>[] constructors = implClass.getConstructors();
        Validate.notEmpty(constructors, implClass.getName() + " has no constructor");

        for (Constructor<?> constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers()) && ArrayUtils.isEmpty(constructor.getParameterTypes())) {
                // Found the public no-arg constructor
                return;
            }
        }
        throw new IllegalArgumentException(implClass.getName() + " has no public no-args constructor");
    }

    /**
     * Manually add service implementation class to service loader
     *
     * @param clz class to add to service loader
     */
    public void addServiceImplClass(Class<T> clz) {
        if (clz == null) {
            return;
        }
        checkServiceImplClass(clz);
        String spiName = getSpiServiceName(clz);
        synchronized (serviceImplClasses) {
            if (serviceImplClasses.containsKey(spiName)) {
                failThrows(clz, ":Error spiName already exist " + spiName);
            } else {
                serviceImplClasses.put(spiName, clz);
            }
        }
    }

    /**
     * Get service implementation class by name
     *
     * @param name service implementation service name
     * @return implementation service class
     */
    public Class<T> getServiceImplClass(String name) {
        return serviceImplClasses.get(name);
    }

    /**
     * Get SPI service name from {@link ServiceName}
     *
     * @param implClass service implementation class
     * @return SPI service name
     */
    private String getSpiServiceName(Class<?> implClass) {
        ServiceName serviceName = implClass.getAnnotation(ServiceName.class);
        return serviceName != null && StringUtils.isNotEmpty(serviceName.value()) ? serviceName.value() : implClass.getSimpleName();
    }

    /**
     * Get service implementation instance by name
     *
     * @param name service implementation service name
     * @return implementation service instance
     */
    public T load(String name) {
        Validate.notEmpty(name, "Service name must NOT be empty!");

        try {
            Spi spi = serviceInterface.getAnnotation(Spi.class);
            if (spi.scope() == SpiScope.SINGLETON) {
                return getSingletonServiceImpl(name);
            } else {
                return getPrototypeServiceImpl(name);
            }
        } catch (Exception e) {
            failThrows(serviceInterface, "Error when getExtension " + name, e);
        }
        return null;
    }

    private T getSingletonServiceImpl(String name) throws InstantiationException, IllegalAccessException {
        T obj = singletonInstances.get(name);
        if (obj != null) {
            return obj;
        }

        Class<T> clz = serviceImplClasses.get(name);
        if (clz == null) {
            return null;
        }

        synchronized (singletonInstances) {
            obj = singletonInstances.get(name);
            if (obj != null) {
                return obj;
            }
            obj = clz.newInstance();
            singletonInstances.put(name, obj);
        }
        return obj;
    }

    private T getPrototypeServiceImpl(String name) throws IllegalAccessException, InstantiationException {
        Class<T> clz = serviceImplClasses.get(name);
        if (clz == null) {
            return null;
        }
        return clz.newInstance();
    }

    private static <T> void failThrows(Class<T> type, String msg, Throwable cause) {
        throw new RuntimeException();
        //todo
//        throw new MotanFrameworkException(type.getName() + ": " + msg, cause);
    }

    private static <T> void failThrows(Class<T> type, String msg) {
        throw new RuntimeException();
        //todo
//        throw new MotanFrameworkException(type.getName() + ": " + msg);
    }
}
