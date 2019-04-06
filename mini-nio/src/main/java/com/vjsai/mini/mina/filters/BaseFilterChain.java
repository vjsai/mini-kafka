package com.vjsai.mini.mina.filters;

import com.vjsai.mini.mina.api.IoFilter;
import com.vjsai.mini.mina.api.IoFilterChain;

/**
 * Chain of Responsibilty is handled here.If a new filter wants to be used can be added here
 */
public class BaseFilterChain implements IoFilterChain {
    public boolean addFilter(String name, IoFilter filter) {
        return false;
    }

    public boolean removeFilter(IoFilter filter) {
        return false;
    }
}
