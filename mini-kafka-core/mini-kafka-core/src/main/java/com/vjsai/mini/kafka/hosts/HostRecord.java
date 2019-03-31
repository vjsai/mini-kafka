package com.vjsai.mini.kafka.hosts;

import java.io.Serializable;

public class HostRecord implements Serializable {
    private final String host;
    private final Integer port;
    public HostRecord(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        return ((HostRecord) o) != null
                &&  o instanceof HostRecord
                && ((HostRecord) o).host.equals(host)
                && ((HostRecord) o).getPort().equals(port);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (host == null ? 0 : host.hashCode());
        hash = 31 * hash + (port == null ? 0 : port.hashCode());
        return hash;
    }
}
