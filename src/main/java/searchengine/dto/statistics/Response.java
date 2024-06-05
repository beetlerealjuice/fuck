package searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class Response {

    private Boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;
}
