package org.infinity.rpc.registry.zookeeper;

import java.util.Arrays;

/**
 * Zookeeper active status node name
 */
public enum StatusDir {

    ACTIVE("active"),
    INACTIVE("inactive"),
    // todo: remove
    CLIENT("client");

    StatusDir(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }

    public static StatusDir fromValue(String value) {
        return Arrays.stream(StatusDir.values()).filter(x -> x.getValue().equals(value)).findFirst().orElse(null);
    }
}
