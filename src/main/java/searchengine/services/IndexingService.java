package searchengine.services;

import searchengine.dto.statistics.Response;

public interface IndexingService {
    Response startIndexing();

    Response stopIndexing();

    Response indexPage(String url);

}
