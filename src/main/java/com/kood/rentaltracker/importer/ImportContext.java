package com.kood.rentaltracker.importer;

import java.util.HashMap;
import java.util.Map;

public class ImportContext {

    private final Map<Integer, Integer> userIds = new HashMap<>();
    private final Map<Integer, Integer> itemIds = new HashMap<>();

    public void mapUser(int sourceId, int localId) {
        userIds.put(sourceId, localId);
    }

    public void mapItem(int sourceId, int localId) {
        itemIds.put(sourceId, localId);
    }

    public Integer resolveUser(int sourceId) {
        return userIds.get(sourceId);
    }

    public Integer resolveItem(int sourceId) {
        return itemIds.get(sourceId);
    }
}
