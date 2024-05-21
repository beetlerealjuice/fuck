package searchengine.services;

import searchengine.dto.statistics.SearchedData;
import searchengine.dto.statistics.StatisticsData;

public interface IndexingService {
    Boolean startIndexing();

    Boolean stopIndexing();

    int getActiveTreads();

    Boolean getStopExecutor();

    Boolean indexPage(String url);

    StatisticsData getStatistic();

    SearchedData search(String query, String site);
}
