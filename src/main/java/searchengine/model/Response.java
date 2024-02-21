package searchengine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

public class Response {


    @Setter
    @Getter
    private Boolean result;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;
}
