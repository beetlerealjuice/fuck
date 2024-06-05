package searchengine.services;

import searchengine.dto.statistics.Response;
import searchengine.dto.statistics.SearchedData;
import searchengine.dto.statistics.StatisticsResponse;

public interface IndexingService {
    Response startIndexing();

    Response stopIndexing();

    int getActiveTreads();

    Boolean getStopExecutor();

    Response indexPage(String url);

    StatisticsResponse getStatistic();

    SearchedData search(String query, String site, Integer offset, Integer limit);
}
