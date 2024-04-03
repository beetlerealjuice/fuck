package searchengine.services;

import searchengine.dto.statistics.StatisticsData;
import searchengine.model.SearchedData;

public interface IndexingService {
    Boolean startIndexing();

    Boolean stopIndexing();

    int getActiveTreads();

    Boolean getStopExecutor();

    Boolean indexPage(String url);

    StatisticsData getStatistic();

    SearchedData search(String query);
}
