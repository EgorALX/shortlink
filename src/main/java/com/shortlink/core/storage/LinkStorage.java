package com.shortlink.core.storage;

import com.shortlink.core.model.Link;

import java.util.List;
import java.util.UUID;

public interface LinkStorage {

    void save(Link link);

    Link find(String code);

    boolean exists(String code);

    void delete(String code);

    List<Link> findByOwner(UUID ownerId);

    List<Link> findAll();
}
