package org.infinity.luix.core.server.buildin;

import lombok.Data;

@Data
public class ServerInfo {
    private String osName;
    private String osVersion;
    private String timeZone;
    private String systemTime;
    private String jdkVendor;
    private String jdkVersion;
    private int    cpuCores;
    private String memoryStatistic;
}
