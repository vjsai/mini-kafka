package com.vjsai.mini.mina.filters;

import com.vjsai.mini.mina.api.IoFilter;
import com.vjsai.mini.mina.api.IoFilterChain;
import com.vjsai.mini.mina.exceptions.NioBaseWriteException;
import com.vjsai.mini.mina.session.SocketSessionState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chain of Responsibilty is handled here. If a new filter wants to be used can be added here
 */
public class BaseFilterChain implements IoFilterChain {

    private volatile List<IoFilter> filterList = new ArrayList<IoFilter>();
    private volatile Map<String, IoFilter> filterMap = new HashMap<String, IoFilter>();

    public boolean addFilter(String filterName, IoFilter filter) {
        if (!filterMap.containsKey(filterName)) {
            this.filterList.add(filter);
            this.filterMap.put(filterName, filter);
            return true;
        }
        return false;
    }

    public boolean removeFilter(String filterName) {
        if (filterMap.containsKey(filterName)) {
            IoFilter removed = this.filterMap.remove(filterName);
            this.filterList.remove(removed);
            return true;
        }
        return false;
    }

    public byte[] filterReceive(SocketSessionState session, byte[] filterBytes) throws IOException {
        /**
         * Iterate over all filters and apply byte transforms
         */
        for (IoFilter filter : filterList) {
            filterBytes = filter.filterReceive(session, filterBytes);
        }
        return filterBytes;
    }

    public void writeFilter(SocketSessionState session, byte[] writeBytes) throws NioBaseWriteException, IOException {
        for(IoFilter filter : filterList){
            filter.writeFilter(session, writeBytes);
        }
    }

    public void removeFilter() {
        if (filterMap != null) {
            filterMap.clear();
            filterMap = null;
        }
        if (filterList != null) {
            filterList.clear();
            filterList = null;
        }
    }
}
