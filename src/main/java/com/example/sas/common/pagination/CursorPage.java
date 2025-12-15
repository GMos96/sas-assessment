package com.example.sas.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Generic cursor-based pagination response.
 *
 * Cursor pagination is efficient for large datasets and works well with sorted,
 * time-based data like audit histories. It avoids issues with offset-based pagination
 * where records can be skipped or duplicated if data changes between requests.
 */
@Schema(description = "Cursor-based paginated response")
public class CursorPage<T> {

    @Schema(description = "List of items in this page", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> items;

    @Schema(description = "Cursor to fetch the next page. Null if no more records available.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nextCursor;

    @Schema(description = "Cursor to fetch the previous page. Null if at the beginning.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String previousCursor;

    @Schema(description = "Total number of items returned in this page",
            example = "20")
    private int pageSize;

    @Schema(description = "Whether there are more records after this page",
            example = "true")
    private boolean hasNextPage;

    public CursorPage() {
    }

    public CursorPage(List<T> items, String nextCursor, String previousCursor, int pageSize, boolean hasNextPage) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.previousCursor = previousCursor;
        this.pageSize = pageSize;
        this.hasNextPage = hasNextPage;
    }

    // Getters and setters

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public String getPreviousCursor() {
        return previousCursor;
    }

    public void setPreviousCursor(String previousCursor) {
        this.previousCursor = previousCursor;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }
}

