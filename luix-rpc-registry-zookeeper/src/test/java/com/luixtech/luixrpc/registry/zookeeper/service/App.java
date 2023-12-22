package com.luixtech.luixrpc.registry.zookeeper.service;

import lombok.Data;

import java.io.Serializable;

@Data
public class App implements Serializable {
    private static final long    serialVersionUID = 3003940031070666048L;
    private              String  name;
    private              Boolean enabled;
}
