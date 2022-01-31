package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.infinity.luix.core.registry.CommandFailbackAbstractRegistry;
import org.infinity.luix.core.registry.listener.CommandListener;
import org.infinity.luix.core.registry.listener.ProviderListener;
import org.infinity.luix.core.server.listener.ConsumerProcessable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.registry.consul.utils.ConsulUtils;
import org.infinity.luix.utilities.destory.Destroyable;
import org.infinity.luix.utilities.destory.ShutdownHook;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.infinity.luix.core.constant.RegistryConstants.DISCOVERY_INTERVAL;
import static org.infinity.luix.core.constant.RegistryConstants.DISCOVERY_INTERVAL_VAL_DEFAULT;

@Slf4j
@ThreadSafe
public class ConsulRegistry extends CommandFailbackAbstractRegistry implements Destroyable {

    /**
     * Consul client
     */
    private final LuixConsulClient                                                    consulClient;
    /**
     * Consul service instance status updater
     */
    private final ConsulStatusUpdater                                                 consulStatusUpdater;
    /**
     * Service discovery interval in milliseconds
     */
    private final int                                                                 discoverInterval;
    /**
     * Provider service notification thread pool
     */
    private final ThreadPoolExecutor                                                  notificationThreadPool;
    /**
     * Cache used to store provider urls
     * Key: form
     * Value: form to urls map
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<Url>>>     urlCache         = new ConcurrentHashMap<>();
    /**
     * Cache used to store commands
     * Key: form
     * Value: command string
     */
    private final ConcurrentHashMap<String, String>                                   commandCache     = new ConcurrentHashMap<>();
    /**
     * Key: form
     * Value: lastConsulIndexId
     */
    private final ConcurrentHashMap<String, Long>                                     form2ConsulIndex = new ConcurrentHashMap<>();
    /**
     * Key: form
     * Value: command string
     */
    private final ConcurrentHashMap<String, String>                                   form2Command     = new ConcurrentHashMap<>();
    /**
     * Key: form plus path
     * Value: url to providerListener map
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Url, ProviderListener>> serviceListeners = new ConcurrentHashMap<>();
    /**
     * Key: form plus path
     * Value: url to commandListener map
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Url, CommandListener>>  commandListeners = new ConcurrentHashMap<>();

    public ConsulRegistry(Url url, LuixConsulClient consulClient) {
        super(url);
        this.consulClient = consulClient;
        consulStatusUpdater = new ConsulStatusUpdater(consulClient);
        consulStatusUpdater.start();
        discoverInterval = registryUrl.getIntOption(DISCOVERY_INTERVAL, DISCOVERY_INTERVAL_VAL_DEFAULT);
        notificationThreadPool = createNotificationThreadPool();
        ShutdownHook.add(this);
        log.info("Initialized consul registry");
    }

    private ThreadPoolExecutor createNotificationThreadPool() {
        return new ThreadPoolExecutor(10, 30, 30 * 1_000,
                TimeUnit.MILLISECONDS, createWorkQueue(), new ThreadPoolExecutor.AbortPolicy());
    }

    private BlockingQueue<Runnable> createWorkQueue() {
        return new ArrayBlockingQueue<>(20_000);
    }

    @Override
    protected void doRegister(Url providerUrl) {
        ConsulService service = ConsulService.of(providerUrl);
        consulClient.registerService(service);
    }

    @Override
    protected void doDeregister(Url providerUrl) {
        ConsulService service = ConsulService.of(providerUrl);
        consulClient.deregisterService(service.getInstanceId());
    }

    @Override
    protected void doActivate(Url url) {
        if (url == null) {
            // Activate all service instances
            consulStatusUpdater.updateStatus(true);
        } else {
            // Activate specified service instance
            consulStatusUpdater.activate(ConsulUtils.buildServiceInstanceId(url));
        }
    }

    @Override
    protected void doDeactivate(Url url) {
        if (url == null) {
            // Deactivate all service instances
            consulStatusUpdater.updateStatus(false);
        } else {
            // Deactivate specified service instance
            consulStatusUpdater.deactivate(ConsulUtils.buildServiceInstanceId(url));
        }
    }

    @Override
    protected List<Url> discoverActiveProviders(Url consumerUrl) {
        String protocolPlusPath = ConsulUtils.getProtocolPlusPath(consumerUrl);
        String form = consumerUrl.getForm();
        List<Url> providerUrls = new ArrayList<>();
        ConcurrentHashMap<String, List<Url>> form2Urls = urlCache.get(form);
        if (form2Urls == null) {
            synchronized (form.intern()) {
                form2Urls = urlCache.get(form);
                if (form2Urls == null) {
                    ConcurrentHashMap<String, List<Url>> path2Urls = doDiscoverActiveProviders(form);
                    updateProviderUrlsCache(form, path2Urls, false);
                    form2Urls = urlCache.get(form);
                }
            }
        }
        if (form2Urls != null) {
            providerUrls = form2Urls.get(protocolPlusPath);
        }
        return providerUrls;
    }

    private ConcurrentHashMap<String, List<Url>> doDiscoverActiveProviders(String form) {
        ConcurrentHashMap<String, List<Url>> path2Urls = new ConcurrentHashMap<>();
        Long lastConsulIndexId = form2ConsulIndex.get(form) == null ? 0L : form2ConsulIndex.get(form);
        Response<List<ConsulService>> response = queryActiveServiceInstances(form, lastConsulIndexId);
        if (response != null) {
            List<ConsulService> activeServiceInstances = response.getValue();
            if (CollectionUtils.isNotEmpty(activeServiceInstances) && response.getConsulIndex() > lastConsulIndexId) {
                for (ConsulService activeServiceInstance : activeServiceInstances) {
                    try {
                        Url url = ConsulUtils.buildUrl(activeServiceInstance);
                        String protocolPlusPath = ConsulUtils.getProtocolPlusPath(url);
                        List<Url> urls = path2Urls.computeIfAbsent(protocolPlusPath, k -> new ArrayList<>());
                        urls.add(url);
                    } catch (Exception e) {
                        log.error("Failed to build url from consul service instance: " + activeServiceInstance, e);
                    }
                }
                form2ConsulIndex.put(form, response.getConsulIndex());
                return path2Urls;
            } else {
                log.info("No consul index update");
            }
        }
        return path2Urls;
    }

    /**
     * update service cache of the group.
     * update local cache when service list changed,
     * if need notify, notify service
     */
    private void updateProviderUrlsCache(String form, ConcurrentHashMap<String, List<Url>> path2Urls, boolean needNotify) {
        if (MapUtils.isNotEmpty(path2Urls)) {
            ConcurrentHashMap<String, List<Url>> groupMap = urlCache.putIfAbsent(form, path2Urls);
            for (Map.Entry<String, List<Url>> entry : path2Urls.entrySet()) {
                boolean change = true;
                if (groupMap != null) {
                    List<Url> oldUrls = groupMap.get(entry.getKey());
                    List<Url> newUrls = entry.getValue();
                    if (CollectionUtils.isEmpty(newUrls) || ConsulUtils.isSame(entry.getValue(), oldUrls)) {
                        change = false;
                    } else {
                        groupMap.put(entry.getKey(), newUrls);
                    }
                }
                if (change && needNotify) {
                    notificationThreadPool.execute(new NotifyService(entry.getKey(), entry.getValue()));
                    log.info("service notify-service: " + entry.getKey());
                    StringBuilder sb = new StringBuilder();
                    for (Url url : entry.getValue()) {
                        sb.append(url.getUri()).append(";");
                    }
                    log.info("consul notify urls:" + sb);
                }
            }
        }
    }

    private Response<List<ConsulService>> queryActiveServiceInstances(String serviceName, Long lastConsulIndexId) {
        return consulClient.queryActiveServiceInstances(ConsulUtils.buildServiceName(serviceName), lastConsulIndexId);
    }

    @Override
    protected void subscribeProviderListener(Url consumerUrl, ProviderListener listener) {
        addServiceListener(consumerUrl, listener);
        startListenerThreadIfNewService(consumerUrl);
    }

    private void addServiceListener(Url url, ProviderListener providerListener) {
        String protocolPlusPath = ConsulUtils.getProtocolPlusPath(url);
        ConcurrentHashMap<Url, ProviderListener> map = serviceListeners.get(protocolPlusPath);
        if (map == null) {
            serviceListeners.putIfAbsent(protocolPlusPath, new ConcurrentHashMap<>());
            map = serviceListeners.get(protocolPlusPath);
        }
        synchronized (map) {
            map.put(url, providerListener);
        }
    }

    /**
     * if new group registered, start a new lookup thread
     * each group start a lookup thread to discover service
     */
    private void startListenerThreadIfNewService(Url url) {
        String group = url.getForm();
        if (!form2ConsulIndex.containsKey(group)) {
            Long value = form2ConsulIndex.putIfAbsent(group, 0L);
            if (value == null) {
                ServiceLookupThread lookupThread = new ServiceLookupThread(group);
                lookupThread.setDaemon(true);
                lookupThread.start();
            }
        }
    }

    @Override
    protected void unsubscribeProviderListener(Url consumerUrl, ProviderListener listener) {
        ConcurrentHashMap<Url, ProviderListener> listeners = serviceListeners.get(ConsulUtils.getProtocolPlusPath(consumerUrl));
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(consumerUrl);
            }
        }
    }

    @Override
    protected void subscribeCommandListener(Url consumerUrl, CommandListener listener) {
        addCommandListener(consumerUrl, listener);
        startListenerThreadIfNewCommand(consumerUrl);
    }

    private void addCommandListener(Url url, CommandListener commandListener) {
        String group = url.getForm();
        ConcurrentHashMap<Url, CommandListener> map = commandListeners.get(group);
        if (map == null) {
            commandListeners.putIfAbsent(group, new ConcurrentHashMap<>());
            map = commandListeners.get(group);
        }
        synchronized (map) {
            map.put(url, commandListener);
        }
    }

    private void startListenerThreadIfNewCommand(Url url) {
        String group = url.getForm();
        if (!form2Command.containsKey(group)) {
            String command = form2Command.putIfAbsent(group, "");
            if (command == null) {
                CommandLookupThread lookupThread = new CommandLookupThread(group);
                lookupThread.setDaemon(true);
                lookupThread.start();
            }
        }
    }

    @Override
    protected void unsubscribeCommandListener(Url consumerUrl, CommandListener listener) {
        ConcurrentHashMap<Url, CommandListener> listeners = commandListeners.get(consumerUrl.getForm());
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(consumerUrl);
            }
        }
    }

    @Override
    protected String readCommand(Url consumerUrl) {
        String group = consumerUrl.getForm();
        String command = lookupCommandUpdate(group);
        updateCommandCache(group, command, false);
        return command;
    }

    @Override
    public List<String> getAllProviderPaths() {
        return null;
    }

    @Override
    public List<String> discoverActiveProviderAddress(String providerPath) {
        return null;
    }

    @Override
    public void subscribeConsumerListener(String interfaceName, ConsumerProcessable consumerProcessor) {

    }

    private String lookupCommandUpdate(String group) {
        String command = consulClient.queryCommand(group);
        form2Command.put(group, command);
        return command;
    }

    /**
     * update command cache of the group.
     * update local cache when command changed,
     * if need notify, notify command
     */
    private void updateCommandCache(String group, String command, boolean needNotify) {
        String oldCommand = commandCache.get(group);
        if (!command.equals(oldCommand)) {
            commandCache.put(group, command);
            if (needNotify) {
                notificationThreadPool.execute(new NotifyCommand(group, command));
                log.info(String.format("command data change: group=%s, command=%s: ", group, command));
            }
        } else {
            log.info(String.format("command data not change: group=%s, command=%s: ", group, command));
        }
    }

    protected Url getUrl() {
        return super.registryUrl;
    }

    private class NotifyService implements Runnable {
        private final String    service;
        private final List<Url> urls;

        public NotifyService(String service, List<Url> urls) {
            this.service = service;
            this.urls = urls;
        }

        @Override
        public void run() {
            ConcurrentHashMap<Url, ProviderListener> listeners = serviceListeners.get(service);
            if (listeners != null) {
                synchronized (listeners) {
                    for (Map.Entry<Url, ProviderListener> entry : listeners.entrySet()) {
                        ProviderListener serviceListener = entry.getValue();
                        serviceListener.onNotify(entry.getKey(), getUrl(), urls);
                    }
                }
            } else {
                log.debug("need not notify service:" + service);
            }
        }
    }

    private class NotifyCommand implements Runnable {
        private final String group;
        private final String command;

        public NotifyCommand(String group, String command) {
            this.group = group;
            this.command = command;
        }

        @Override
        public void run() {
            ConcurrentHashMap<Url, CommandListener> listeners = commandListeners.get(group);
            synchronized (listeners) {
                for (Map.Entry<Url, CommandListener> entry : listeners.entrySet()) {
                    CommandListener commandListener = entry.getValue();
                    commandListener.onNotify(entry.getKey(), command);
                }
            }
        }
    }

    private class ServiceLookupThread extends Thread {
        private final String form;

        public ServiceLookupThread(String form) {
            this.form = form;
        }

        @Override
        public void run() {
            log.info("start group lookup thread. lookup interval: " + discoverInterval + "ms, group: " + form);
            while (true) {
                try {
                    sleep(discoverInterval);
                    ConcurrentHashMap<String, List<Url>> urlsPerPath = doDiscoverActiveProviders(form);
                    updateProviderUrlsCache(form, urlsPerPath, true);
                } catch (Throwable e) {
                    log.error("group lookup thread fail!", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    private class CommandLookupThread extends Thread {
        private final String group;

        public CommandLookupThread(String group) {
            this.group = group;
        }

        @Override
        public void run() {
            log.info("start command lookup thread. lookup interval: " + discoverInterval + "ms, group: " + group);
            while (true) {
                try {
                    sleep(discoverInterval);
                    String command = lookupCommandUpdate(group);
                    updateCommandCache(group, command, true);
                } catch (Throwable e) {
                    log.error("group lookup thread fail!", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    @Override
    public void destroy() {
        notificationThreadPool.shutdown();
        consulStatusUpdater.close();
        log.info("Destroyed consul registry");
    }
}
