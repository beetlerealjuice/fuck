package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Response;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final IndexingService indexingServiceService;
    private final StatisticsService statisticsService;


    public ApiController(StatisticsService statisticsService,
                         IndexingService indexingServiceService) {
        this.statisticsService = statisticsService;
        this.indexingServiceService = indexingServiceService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        Response response = new Response();
        if (indexingServiceService.getActiveTreads() == 0) {
            response.setResult(indexingServiceService.startIndexing());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else

            response.setResult(false);
        response.setError("Индексация уже запущена");
        return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        Response response = new Response();
        if (indexingServiceService.getActiveTreads() == 0) {
            response.setResult(false);
            response.setError("Индексация не запущена");
            return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
        } else
            response.setResult(indexingServiceService.stopIndexing());
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        Response response = new Response();

        if (indexingServiceService.indexPage(url) == false) {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
        }
        response.setResult(indexingServiceService.indexPage(url));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}