package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Service;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.kv.model.GetValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.registry.consul.utils.ConsulUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.infinity.luix.registry.consul.utils.ConsulUtils.CONSUL_PROVIDING_SERVICES_PREFIX;
import static org.infinity.luix.registry.consul.utils.ConsulUtils.CONSUL_SERVICE_INSTANCE_DELIMITER;

@Slf4j
public class LuixConsulClient {
    /**
     * Consul query timeout in seconds
     */
    public static        long         CONSUL_QUERY_TIMEOUT_SECONDS   = TimeUnit.MINUTES.toSeconds(9);
    /**
     * Command key prefix
     */
    public static final  String       CONSUL_LUIX_COMMAND_KEY_PREFIX = "luix/command/";
    /**
     * Service instance ID prefix
     */
    private static final String       SERVICE_INSTANCE_ID_PREFIX     = "service:";
    /**
     * Consul client instance
     */
    public static        ConsulClient consulClient;

    public LuixConsulClient(String host, int port) {
        consulClient = new ConsulClient(host, port);
        log.info("Initialized consul client with host: [{}] and port: [{}]", host, port);
    }

    public void registerService(ConsulService service) {
        consulClient.agentServiceRegister(service.toNewService());
    }

    public void deregisterService(String serviceInstanceId) {
        consulClient.agentServiceDeregister(serviceInstanceId);
        log.debug("Deregistered service instance with ID: [{}]", serviceInstanceId);
    }

    public void activate(String serviceInstanceId) {
        consulClient.agentCheckPass(SERVICE_INSTANCE_ID_PREFIX + serviceInstanceId);
    }

    public void deactivate(String serviceInstanceId) {
        consulClient.agentCheckFail(SERVICE_INSTANCE_ID_PREFIX + serviceInstanceId);
    }

    public Set<String> getAllProviderPaths() {
        Response<Map<String, Service>> response = consulClient.getAgentServices();
        if (response == null || MapUtils.isEmpty(response.getValue())) {
            return Collections.emptySet();
        }
        Set<String> paths = response.getValue().entrySet().stream()
                .filter(entry -> entry.getValue().getService().startsWith(CONSUL_PROVIDING_SERVICES_PREFIX))
                .map(entry -> entry.getKey().substring(0, entry.getKey().indexOf(CONSUL_SERVICE_INSTANCE_DELIMITER)))
                .collect(Collectors.toSet());
        return paths;
    }

    public Response<List<ConsulService>> queryActiveServiceInstances(String serviceName, long lastConsulIndex) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setQueryParams(new QueryParams(CONSUL_QUERY_TIMEOUT_SECONDS, lastConsulIndex))
                .setPassing(true)
                .build();
        Response<List<HealthService>> response = consulClient.getHealthServices(serviceName, request);
        if (response == null) {
            return null;
        }
        List<ConsulService> activeServiceInstances = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(response.getValue())) {
            for (HealthService activeServiceInstance : response.getValue()) {
                try {
                    activeServiceInstances.add(ConsulService.byProviderUrl(activeServiceInstance));
                } catch (Exception e) {
                    String serviceInstanceId = activeServiceInstance.getService() != null
                            ? activeServiceInstance.getService().getId()
                            : null;
                    log.error("Failed to convert to consul service with ID: [" + serviceInstanceId + "]", e);
                }
            }
        }
        return new Response<>(activeServiceInstances, response.getConsulIndex(),
                response.isConsulKnownLeader(), response.getConsulLastContact());
    }

    public String queryCommand(String form) {
        String key = CONSUL_LUIX_COMMAND_KEY_PREFIX + ConsulUtils.buildProviderServiceName(form);
        GetValue value = consulClient.getKVValue(key).getValue();
        String command = StringUtils.EMPTY;
        if (value == null) {
            log.warn("No command found with form: [{}]", form);
        } else if (value.getValue() != null) {
            command = value.getDecodedValue();
        }
        return command;
    }
}
