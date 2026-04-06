package com.aiinventorysort.sorting;

import java.util.function.Consumer;

/**
 * Client-safe interface for AI inventory sorting.
 * Full implementation in client source set.
 */
public interface AiSorter {

    /**
     * Sort asynchronously. Calls onDone on main thread when finished.
     * @param onDone callback - null on success, error msg on failure
     */
    void sortAsync(Consumer<String> onDone);

    /**
     * Take inventory snapshot to config.
     */
    void takeSnapshot();
}
