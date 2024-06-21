package searchengine.services;

import searchengine.dto.statistics.SearchedData;

public interface SearchService {
    SearchedData search(String query, String site, Integer offset, Integer limit);
}
