package searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import searchengine.model.Information;

import java.util.List;

@Data
@Builder
public class SearchedData {
    private boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer count;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Information> data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;


}
