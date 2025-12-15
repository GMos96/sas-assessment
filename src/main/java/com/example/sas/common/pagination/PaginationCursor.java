package com.example.sas.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * Cursor for pagination, encoding the timestamp and ID of the last item.
 *
 * Format: Base64(timestamp|id)
 * This allows efficient "fetch items after this cursor" queries using indexed fields.
 */
@Schema(description = "Encoded cursor for pagination")
public class PaginationCursor {

    private OffsetDateTime timestamp;
    private String id;

    public PaginationCursor(OffsetDateTime timestamp, String id) {
        this.timestamp = timestamp;
        this.id = id;
    }

    /**
     * Encode cursor to Base64 string for API response
     */
    public String encode() {
        String raw = timestamp + "|" + id;
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    /**
     * Decode cursor from Base64 string from API request
     */
    public static PaginationCursor decode(String encoded) {
        String raw = new String(Base64.getDecoder().decode(encoded));
        String[] parts = raw.split("\\|");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid cursor format");
        }
        OffsetDateTime timestamp = OffsetDateTime.parse(parts[0]);
        String id = parts[1];
        return new PaginationCursor(timestamp, id);
    }

    // Getters

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public String getId() {
        return id;
    }
}

