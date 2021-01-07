package org.infinity.rpc.transport.netty4;

import org.infinity.rpc.core.exchange.transport.Channel;

public interface MessageHandler {

    Object handle(Channel channel, Object message);

}