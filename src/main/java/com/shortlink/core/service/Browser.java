package com.shortlink.core.service;

import java.io.IOException;

public interface Browser {

    void open(String url) throws IOException;
}
