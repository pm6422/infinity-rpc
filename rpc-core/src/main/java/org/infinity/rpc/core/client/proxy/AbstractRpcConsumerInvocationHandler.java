package org.infinity.rpc.core.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.ClusterHolder;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.impl.RequestContext;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.switcher.SwitcherService;
import org.infinity.rpc.core.url.Url;

import java.util.List;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public abstract class AbstractRpcConsumerInvocationHandler<T> {
    /**
     * The interface class of the consumer
     */
    protected Class<T>           interfaceClass;
    protected SwitcherService    switcherService;
    protected InfinityProperties infinityProperties;

    /**
     * @param request    RPC request
     * @param returnType return type of method
     * @param async      async call flag
     * @return return result of method
     */
    protected Object processRequest(Requestable request, Class returnType, boolean async) {
        RequestContext threadRpcContext = RequestContext.getThreadRpcContext();
        threadRpcContext.setAsyncCall(async);

        // Copy values from context to request object
        copyContextToRequest(threadRpcContext, request);

//        RequestContext.initialize(request);

        // The RPC framework supports multiple protocols
        // One cluster for one protocol
        List<ProviderCluster<T>> providerClusters = ClusterHolder.getInstance().getClusters();
        for (ProviderCluster<T> providerCluster : providerClusters) {
            Url clientUrl = providerCluster.getHighAvailability().getClientUrl();
            request.addAttachment(Url.PARAM_APP, infinityProperties.getApplication().getName());

            Responseable response = null;
            boolean throwException = true;
            try {
                // provider cluster call => cluster HA call => provider requester call
                // Only one server node under one cluster can process the request
                response = providerCluster.call(request);
                return response.getResult();
            } catch (Exception ex) {
                log.error("", ex);
            }
        }
        throw new RpcServiceException("No cluster!");
    }

    /**
     * Copy values from context to request object
     *
     * @param threadRpcContext RPC context object
     * @param request          request object
     */
    private void copyContextToRequest(RequestContext threadRpcContext, Requestable request) {
        // Copy attachments from RPC context to request object
        threadRpcContext.getAttachments().forEach(request::addAttachment);

        // Copy client request id from RPC context to request object
        request.setClientRequestId(threadRpcContext.getClientRequestId());
    }

}
