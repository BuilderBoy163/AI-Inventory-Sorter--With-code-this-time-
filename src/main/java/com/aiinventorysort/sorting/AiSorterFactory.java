package com.aiinventorysort.sorting;

/**
 * Dummy factory for common source set.
 * Real instantiation happens in client code.
 */
public class AiSorterFactory {

    public static AiSorter create() {
        throw new UnsupportedOperationException("AiSorterFactory.create() must be called from client context");
    }
}

