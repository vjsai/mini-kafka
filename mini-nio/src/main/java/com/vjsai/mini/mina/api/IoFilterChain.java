package com.vjsai.mini.mina.api;

public interface IoFilterChain extends IoFilter {
    /**
     * adds filter to chain
     * @param name
     * @param filter
     * @return
     */
    boolean addFilter(String name,IoFilter filter);

    /**
     * Removes filter from the chain
     * @param name
     * @return
     */
    boolean removeFilter(String name);
}
