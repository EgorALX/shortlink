package com.shortlink.infra;

import com.shortlink.core.model.Link;
import com.shortlink.core.storage.LinkStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryLinkStorage implements LinkStorage {

    private final Map<String, Link> links = new LinkedHashMap<>();

    @Override
    public void save(Link link) {
        links.put(link.getCode(), link);
    }

    @Override
    public Link find(String code) {
        return links.get(code);
    }

    @Override
    public boolean exists(String code) {
        return links.containsKey(code);
    }

    @Override
    public void delete(String code) {
        links.remove(code);
    }

    @Override
    public List<Link> findByOwner(UUID ownerId) {
        List<Link> result = new ArrayList<>();
        for (Link link : links.values()) {
            if (link.isOwnedBy(ownerId)) {
                result.add(link);
            }
        }
        return result;
    }

    @Override
    public List<Link> findAll() {
        return new ArrayList<>(links.values());
    }
}
