package org.infinity.luix.webcenter.task.polling.queue;

import org.infinity.luix.webcenter.dto.StatisticDTO;

import java.util.HashMap;
import java.util.Map;

public class StatisticResultHolder {

    private static final Map<String, StatisticDTO> MAP = new HashMap<>(16);

    public static void put(String id, StatisticDTO msg) {
        MAP.put(id, msg);
    }

    public static StatisticDTO get(String id) {
        return MAP.remove(id);
    }
}
