package com.nuix.relativityclient.relativitytypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResponse<T> {
    @JsonProperty("TotalResultCount")
    private long totalResultCount;

    public long getTotalResultCount() {
        return totalResultCount;
    }

    @JsonProperty("ResultCount")
    private long resultCount;

    public long getResultCount() {
        return resultCount;
    }

    @JsonProperty("Results")
    private List<T> results;

    public List<T> getResults() {
        return results;
    }
}
