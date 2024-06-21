package searchengine.services.impl;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchedData;
import searchengine.model.*;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchService;
import searchengine.utils.LemmaFinder;
import searchengine.utils.LemmaFinderEn;
import searchengine.utils.QueryRequest;

import java.util.*;

import static searchengine.services.impl.IndexingServiceImpl.*;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexSearchRepository indexSearchRepository;
    @Autowired
    private PageRepository pageRepository;

    private List<Information> searchDates = new ArrayList<>();
    private List<Information> newSearchData = new ArrayList<>();
    private List<QueryRequest> queries = new ArrayList<>();

    @SneakyThrows
    @Override
    public SearchedData search(String query, String webSite, Integer offset, Integer limit) {

        String site = webSite != null ? webSite : "";

        boolean emptyQuery = query == null || query.isEmpty();
        if (emptyQuery)
            return getErrorSearchedData("Задан пустой поисковый запрос");

        if (queries.isEmpty()) {
            queries.add(setQueryRequest(site, query));
        }

        queries.forEach(querySite -> {
            if (!querySite.getQuery().equals(query) || !querySite.getWebSite().equals(site)) {
                queries = new ArrayList<>();
                searchDates = new ArrayList<>();
                newSearchData = new ArrayList<>();
                queries.add(setQueryRequest(site, query));
            }
        });


        if (!newSearchData.isEmpty()) {
            return getSearchedData(searchDates.size(), searchDates);
        }


        LemmaFinder lemmaFinder = new LemmaFinder();
        LemmaFinderEn lemmaFinderEn = new LemmaFinderEn();
        Set<String> getLemmasFromQuery;

        if (isEnglish(query)) {
            getLemmasFromQuery = lemmaFinderEn.lemmaSearcher(query);
        } else {

            getLemmasFromQuery = lemmaFinder.lemmaSearcher(query);
        }

        List<Lemma> foundLemmas = findLemmasWithLessThen50Percent(getLemmasFromQuery, webSite);

        if (foundLemmas.isEmpty())
            return getSearchedData(0, new ArrayList<>());

        Map<Integer, Float> relativeRelevant = getRelativeRelevant(foundLemmas);
        List<Information> dataList = getDataWithoutSnippet(relativeRelevant);

        for (Information information : dataList) {
            for (Lemma lemma : foundLemmas) {
                if (lemma.getSite().getUrl().equals(information.getSite())) {
                    searchDates.add(getDataSnippet(information, lemma));
                }
            }
        }

        boolean offsetTrue = offset != null && offset != 0;
        boolean limitTrue = limit != null && limit != 0;

        int start = 0;
        int finish = 20;

        if (searchDates.isEmpty())
            return getErrorSearchedData("Запрашиваемые данные не найдены в базе данных");

        if (offsetTrue && offset < searchDates.size()) {
            start = offset;
        }
        if (limitTrue && limit <= searchDates.size()) {
            finish = limit;

        } else if (finish > searchDates.size()) {
            finish = searchDates.size();
        }
        for (int i = start; i < finish; i++) {
            newSearchData.add(searchDates.get(i));

        }

        return getSearchedData(searchDates.size(), newSearchData);
    }

    private List<Information> getLimitInformation() {

        Information lastElement = newSearchData.get(newSearchData.size() - 1);
        Information lastElementDates = searchDates.get(searchDates.size() - 1);

        if (lastElementDates.equals(lastElement)) {
            return new ArrayList<>();
        }

        int start = searchDates.indexOf(lastElement) + 1;
        int finish = start + 20;
        if (searchDates.size() < finish) {
            finish = searchDates.size();
        }
        for (int i = start; i < finish; i++) {
            newSearchData.add(searchDates.get(i));
        }

        return newSearchData;
    }

    private SearchedData getSearchedData(Integer count, List<Information> data) {
        return SearchedData.builder()
                .result(true)
                .count(count)
                .data(data)
                .build();

    }

    private SearchedData getErrorSearchedData(String error) {
        return SearchedData.builder()
                .result(false)
                .count(null)
                .error(error)
                .build();

    }

    private QueryRequest setQueryRequest(String webSite, String query) {
        return QueryRequest.builder()
                .webSite(webSite)
                .query(query)
                .build();
    }


    private Information getDataSnippet(Information information, Lemma lemma) {

        String html = getHtml(information.getUri().trim());

        for (String snip : getSnippet(html, lemma.getLemma())) {
            if (information.getUri().contains(getDomen(lemma.getSite().getUrl()))) {
                information.setSnippet(snip);

            }
        }

        return information;

    }

    private List<Lemma> findLemmasWithLessThen50Percent(Set<String> getLemmasFromQuery, String webSite) {
        boolean siteExist = webSite != null && !webSite.isEmpty();


        Iterable<Site> sites = siteRepository.findAll();
        Iterable<Lemma> lemmas = null;

        if (siteExist) {
            int siteId = 0;
            for (Site site : sites) {
                if (site.getUrl().contains(webSite)) siteId = site.getId();

            }
            if (siteId != 0) lemmas = lemmaRepository.findBySiteId(siteId);
        } else {

            lemmas = lemmaRepository.findAll();
        }


        List<Integer> frequencies = new ArrayList<>();
        List<Lemma> foundLemmas = new ArrayList<>();

        for (String getLemma : getLemmasFromQuery) {
            for (Lemma lemma : lemmas) {
                if (lemma.getLemma().equals(getLemma)) {
                    frequencies.add(lemma.getFrequency());
                    foundLemmas.add(lemma);
                }
            }
        }


        if (!foundLemmas.isEmpty()) {
            int max = Collections.max(frequencies);
            int min = Collections.min(frequencies);
            int percent;
            if (max != min) {
                percent = ((max - min) / 2) + min;
            } else {
                percent = max;
            }

            foundLemmas.removeIf(nextLemma -> nextLemma.getFrequency() > percent);
            return foundLemmas;

        }

        return foundLemmas;
    }

    private Map<Integer, Float> getRelativeRelevant(List<Lemma> foundLemmas) {
        Map<Integer, Float> pageRank = new HashMap<>();

        Iterable<IndexSearch> indexSearches = indexSearchRepository.findAll();
        for (Lemma lemma : foundLemmas) {
            for (IndexSearch indexSearch : indexSearches) {
                if (indexSearch.getLemma().getId() != lemma.getId()) continue;

                int idPage = indexSearch.getPage().getId();
                String lemma1 = lemma.getLemma();
                float rank1 = indexSearch.getRank();

                if (pageRank.containsKey(idPage)) {
                    float newRank = pageRank.get(idPage) + rank1;
                    pageRank.replace(idPage, newRank);
                } else {
                    pageRank.put(idPage, rank1);
                }
            }
        }

        Map<Integer, Float> relativeRelevant = new HashMap<>();
        float maxValue = Collections.max(pageRank.values());

        for (Map.Entry<Integer, Float> entry : pageRank.entrySet()) {
            float relevantValue = entry.getValue() / maxValue;
            relativeRelevant.put(entry.getKey(), relevantValue);
        }

        return relativeRelevant;
    }

    private List<Information> getDataWithoutSnippet(Map<Integer, Float> relativeRelevant) {
        List<Information> dataList = new ArrayList<>();
        Iterable<Page> pages = pageRepository.findAll();
        for (Map.Entry<Integer, Float> entry : relativeRelevant.entrySet()) {
            Information data = new Information();
            for (Page page : pages) {
                if (page.getId() == entry.getKey()) {
                    Document document = Jsoup.parse(page.getContent());
                    String title = document.title();
                    data.setRelevance(entry.getValue());
                    data.setUri(page.getPath());
                    data.setTitle(title);
                    data.setSite(page.getSite().getUrl());
                    data.setSiteName(page.getSite().getName());
                }

            }
            dataList.add(data);
        }

        return dataList;
    }

    @SneakyThrows
    public List<String> getSnippet(String text, String findWord) {

        LemmaFinder lemmaFinder = new LemmaFinder();
        LemmaFinderEn lemmaFinderEn = new LemmaFinderEn();
        Set<String> lemmas;

        if (isEnglish(findWord)) {
            lemmas = lemmaFinderEn.splitter(text);
        } else {
            lemmas = lemmaFinder.splitter(text);
        }

        List<String> snippets = new ArrayList<>();
        String snippet = "";
        List<String> words;
        for (String textWord : lemmas) {
            if (isEnglish(textWord)) {
                words = lemmaFinderEn.getNormalForms(textWord);
            } else {
                words = lemmaFinder.getNormalForms(textWord);
            }

            for (String lemmaWord : words) {
                if (!lemmaWord.contains(findWord)) {
                    continue;
                }

                int index = text.indexOf(textWord);
                String word;
                if (index == -1) {
                    word = Character.toUpperCase(textWord.charAt(0)) + textWord.substring(1);
                    index = text.indexOf(word);
                } else {
                    word = textWord;
                }
                int sizeOfWord = word.length();

                int textLength = text.length();
                int start = 0;
                int finish = 0;
                int quantityOfSubstring = 100;

                if (index - quantityOfSubstring > 0) {
                    start = index - quantityOfSubstring;
                }

                if (index + quantityOfSubstring >= textLength) {
                    finish = textLength;
                } else {
                    finish = index + quantityOfSubstring;
                }

                String newText = text.substring(start, finish);
                int newIndex = newText.indexOf(word);

                StringBuilder sb = new StringBuilder(newText);
                sb.insert(newIndex + sizeOfWord, "</b>");
                sb.insert(newIndex, "<b>");
                snippets.add(sb.toString());

            }

        }

        return snippets;
    }
}
