package searchengine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class SearchedData {
    private boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer count;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Information> data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;


}
