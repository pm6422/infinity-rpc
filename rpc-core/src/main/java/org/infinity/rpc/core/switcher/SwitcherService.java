package org.infinity.rpc.core.switcher;


import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

@Spi(scope = Scope.SINGLETON)
public interface SwitcherService {
    String REGISTRY_HEARTBEAT_SWITCHER = "feature.configserver.heartbeat";

    /**
     * 获取接口降级开关
     *
     * @param name
     * @return
     */
    Switcher getSwitcher(String name);

    /**
     * 获取所有接口降级开关
     *
     * @return
     */
    List<Switcher> getAllSwitchers();

    /**
     * 初始化开关。
     *
     * @param switcherName
     * @param initialValue
     */
    void initSwitcher(String switcherName, boolean initialValue);

    /**
     * 检查开关是否开启。
     *
     * @param switcherName
     * @return true ：设置来开关，并且开关值为true false：未设置开关或开关为false
     */
    boolean isOpen(String switcherName);

    /**
     * 检查开关是否开启，如果开关不存在则将开关置默认值，并返回。
     *
     * @param switcherName
     * @param defaultValue
     * @return 开关存在时返回开关值，开关不存在时设置开关为默认值，并返回默认值。
     */
    boolean isOpen(String switcherName, boolean defaultValue);

    /**
     * 设置开关状态。
     *
     * @param switcherName
     * @param value
     */
    void setValue(String switcherName, boolean value);

    /**
     * register a listener for switcher value change, register a listener twice will only fire once
     *
     * @param switcherName
     * @param listener
     */
    void registerListener(String switcherName, SwitcherListener listener);

    /**
     * unregister a listener
     *
     * @param switcherName
     * @param listener     the listener to be unregistered, null for all listeners for this switcherName
     */
    void unRegisterListener(String switcherName, SwitcherListener listener);
}
