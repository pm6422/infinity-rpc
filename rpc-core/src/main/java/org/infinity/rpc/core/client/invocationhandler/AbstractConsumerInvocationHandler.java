package org.infinity.rpc.core.client.invocationhandler;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.ExchangeContext;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.network.NetworkUtils;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public abstract class AbstractConsumerInvocationHandler<T> {
    protected ConsumerStub<T> consumerStub;
    protected SwitcherService switcherService;

    /**
     * @param request    RPC request
     * @param returnType return type of method
     * @param async      async call flag
     * @return return result of method
     */
    protected Object processRequest(Requestable request, Class<?> returnType, boolean async) {
        ExchangeContext threadRpcContext = ExchangeContext.getInstance();
        threadRpcContext.setAsyncCall(async);

        // Copy values from context to request object
        copyContextToRequest(threadRpcContext, request);
//        RequestContext.initialize(request);

        // We do NOT know the host and port of provider right now, so we use client URL
        Url clientUrl = Url.clientUrl(consumerStub.getProviderCluster().getProtocol(),
                NetworkUtils.INTRANET_IP, consumerStub.getInterfaceName(), consumerStub.getGroup(), consumerStub.getVersion());
        consumerStub.getProviderCluster().getFaultTolerance().setClientUrl(clientUrl);

//            request.addAttachment(Url.PARAM_APP, infinityProperties.getApplication().getName());
//        request.setProtocol(consumerStub.getProviderCluster().getProtocol());

        Responseable response;
//            boolean throwException = true;
        try {
            // Call chain: provider cluster call => cluster fault tolerance strategy =>
            // LB select node => provider caller call
            // Only one server node under one cluster can process the request
            response = consumerStub.getProviderCluster().call(request);
            return response.getResult();
        } catch (Exception ex) {
            throw new RpcServiceException(ex);
        }
    }

    /**
     * Copy values from context to request object
     *
     * @param threadRpcContext RPC context object
     * @param request          request object
     */
    private void copyContextToRequest(ExchangeContext threadRpcContext, Requestable request) {
        // Copy attachments from RPC context to request object
        threadRpcContext.getAttachments().forEach(request::addOption);

        // Copy client request id from RPC context to request object
//        request.setClientRequestId(threadRpcContext.getClientRequestId());
    }

}
