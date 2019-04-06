package com.vjsai.mini.mina.commons.api;

public interface IoFilterChain {
    /**
     * adds filter to chain
     * @param name
     * @param filter
     * @return
     */
    boolean addFilter(String name,IoFilter filter);

    /**
     * Removes filter from the chain
     * @param filter
     * @return
     */
    boolean removeFilter(IoFilter filter);
}
