package com.shortlink.infra;

import com.shortlink.core.Messages;
import com.shortlink.core.service.Browser;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DesktopBrowser implements Browser {

    @Override
    public void open(String url) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException(Messages.BROWSER_UNAVAILABLE);
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (URISyntaxException e) {
            throw new IOException(String.format(Messages.BROWSER_BAD_URL, url));
        }
    }
}
