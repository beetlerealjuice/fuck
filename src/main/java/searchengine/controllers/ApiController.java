package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.SearchedData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final IndexingService indexingService;


    public ApiController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(indexingService.getStatistic());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchedData> search(@RequestParam(value = "query", required = false) String query,
                                               @RequestParam(value = "site", required = false) String site,
                                               @RequestParam(value = "offset", required = false) Integer offset,
                                               @RequestParam(value = "limit", required = false) Integer limit

    ) {
        return ResponseEntity.ok(indexingService.search(query, site, offset, limit));
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());

    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingService.indexPage(url));
    }


}