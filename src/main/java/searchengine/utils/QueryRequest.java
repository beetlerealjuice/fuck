package searchengine.utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class QueryRequest {
    private String webSite;
    private String query;
}
