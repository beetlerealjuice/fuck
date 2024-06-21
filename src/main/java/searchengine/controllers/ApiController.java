package searchengine.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.SearchedData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticService;

@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final IndexingService indexingService;
    private final StatisticService statisticService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticService.getStatistic());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchedData> search(@RequestParam(value = "query", required = false) String query,
                                               @RequestParam(value = "site", required = false) String site,
                                               @RequestParam(value = "offset", required = false) Integer offset,
                                               @RequestParam(value = "limit", required = false) Integer limit

    ) {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
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