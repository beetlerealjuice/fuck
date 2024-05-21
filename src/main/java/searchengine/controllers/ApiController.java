package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.Response;
import searchengine.dto.statistics.SearchedData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Information;
import searchengine.services.IndexingService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final IndexingService indexingService;


    public ApiController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        StatisticsResponse statisticsResponse = new StatisticsResponse();

        if (indexingService.getStatistic() == null) {
            statisticsResponse.setResult(false);
            return new ResponseEntity<>(statisticsResponse, HttpStatus.NOT_IMPLEMENTED);
        } else
            statisticsResponse.setResult(true);
        statisticsResponse.setStatistics(indexingService.getStatistic());
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
            searchedData = indexingService.search(query, site);
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
        if (indexingService.getActiveTreads() == 0) {
            response.setResult(indexingService.startIndexing());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else

            response.setResult(false);
        response.setError("Индексация уже запущена");
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        Response response = new Response();
        if (indexingService.getActiveTreads() == 0) {
            response.setResult(false);
            response.setError("Индексация не запущена");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else
            response.setResult(indexingService.stopIndexing());
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        Response response = new Response();

        if (indexingService.indexPage(url) == false) {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.setResult(indexingService.indexPage(url));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}