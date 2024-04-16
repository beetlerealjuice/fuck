package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Information;
import searchengine.model.Response;
import searchengine.model.SearchedData;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.util.ArrayList;
import java.util.List;

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
        StatisticsResponse statisticsResponse = new StatisticsResponse();

        if (indexingServiceService.getStatistic() == null) {
            statisticsResponse.setResult(false);
            return new ResponseEntity<>(statisticsResponse, HttpStatus.NOT_IMPLEMENTED);
        } else
            statisticsResponse.setResult(true);
        statisticsResponse.setStatistics(indexingServiceService.getStatistic());
        return new ResponseEntity<>(statisticsResponse, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchedData> search(@RequestParam(value = "query", required = false) String query,
                                               @RequestParam(value = "site", required = false) String site,
                                               @RequestParam(value = "offset", required = false) Integer offset,
                                               @RequestParam(value = "limit", required = false) Integer limit

    ) {
        SearchedData searchedData = new SearchedData();

        if (query != null && !query.isEmpty()) {
            searchedData = indexingServiceService.search(query, site);
            List<Information> data = searchedData.getData();
            List<Information> newData = new ArrayList<>();
            int start = 0;
            int finish = 20;

            if (data == null || data.isEmpty()) {
                searchedData.setResult(false);
                searchedData.setCount(null);
                searchedData.setError("Запрашиваемые данные не найдены в базе данных");
                return new ResponseEntity<>(searchedData, HttpStatus.OK);

            }

            if (offset != null && offset != 0 && offset < data.size()) {
                start = offset;
            }
            if (limit != null && limit != 0 && limit <= data.size()) {
                finish = limit;
            } else if (finish > data.size()) {
                finish = data.size();
            }

            for (int i = start; i < finish; i++) {
                newData.add(data.get(i));
            }
            searchedData.setData(newData);


            return new ResponseEntity<>(searchedData, HttpStatus.OK);
        }
        searchedData.setResult(false);
        searchedData.setCount(null);
        searchedData.setError("Задан пустой поисковый запрос");

        return new ResponseEntity<>(searchedData, HttpStatus.OK);
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
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        Response response = new Response();
        if (indexingServiceService.getActiveTreads() == 0) {
            response.setResult(false);
            response.setError("Индексация не запущена");
            return new ResponseEntity<>(response, HttpStatus.OK);
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
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.setResult(indexingServiceService.indexPage(url));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}