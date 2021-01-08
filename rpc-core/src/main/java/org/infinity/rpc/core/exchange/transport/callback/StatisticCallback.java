package org.infinity.rpc.core.exchange.transport.callback;

/**
 * 统计扩展接口，需要统计的资源自行实现后 registry到StatsUtil便可以
 * 
 *
 */
public interface StatisticCallback {

    String statisticCallback();
}
