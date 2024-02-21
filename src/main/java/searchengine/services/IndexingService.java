package searchengine.services;

public interface IndexingService {
    Boolean startIndexing();

    Boolean stopIndexing();

    int getActiveTreads();

    Boolean getStopExecutor();

    Boolean indexPage(String url);
}
