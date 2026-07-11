package io.nebulaaudio.source;

import java.util.List;

/**
 * Contract for a pluggable audio source. Each implementation handles one
 * "search prefix" (ytsearch:, scsearch:, dzsearch:, spsearch:) or one URI
 * scheme (http(s)://, file:) and knows how to turn a query/identifier into
 * one or more playable AudioTracks.
 */
public interface AudioSourceManager {

    /** Short source name, e.g. "youtube", "http", "local". */
    String getName();

    /** Whether this manager can handle the given identifier/query. */
    boolean canHandle(String identifier);

    /**
     * Resolves an identifier (search query, URL, or path) into a load result.
     * Implementations should not throw for "not found" — return a NOT_FOUND
     * LoadResult instead. Throw only for genuine transport/parsing errors.
     */
    LoadResult loadItem(String identifier) throws Exception;
}
