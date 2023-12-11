package searchengine.controllers;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.*;
import searchengine.model.Record;
import searchengine.services.StatisticsService;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private SitesList list;

    private List<Site> sitesList = new CopyOnWriteArrayList<>();


    private final StatisticsService statisticsService;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;


    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() {
        pageRepository.deleteAll();
        siteRepository.deleteAll();

        sitesList = list.getSites();

        for (int i = 0; i < sitesList.size(); i++) {


//            Запуск в многопоточном режиме
//            int j = i;
//            ThreadPoolExecutor executor =
//                    (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//            executor.execute(() -> {
//
//                Record recordSitePage = new Record();
//                siteRepository.save(recordSitePage.getSiteIndexing(sitesList.get(j)));
//                pageRepository.saveAll(recordSitePage.getPages());
//                siteRepository.save(recordSitePage.getSiteIndexed());
//            });



//          Запуск в однопоточном режиме
            Record recordSitePage = new Record();
            siteRepository.save(recordSitePage.getSiteIndexing(sitesList.get(i)));
            pageRepository.saveAll(recordSitePage.getPages());
            siteRepository.save(recordSitePage.getSiteIndexed());

        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


}