package searchengine.services;

import lombok.Getter;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.*;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.Indexing;
import searchengine.utils.LemmaFinder;
import searchengine.utils.LemmaFinderEn;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;


@Service
public class IndexingServiceImpl implements IndexingService {
    @Autowired
    private SitesList list;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private IndexSearchRepository indexSearchRepository;
    @Autowired
    private LemmaRepository lemmaRepository;


    @Getter
    private volatile int activeTreads;
    private volatile boolean stopExecutor;
    private volatile ThreadPoolExecutor executor;
    private List<Information> searchDates = new ArrayList<>();
    private List<Information> newSearchData = new ArrayList<>();
    private List<String> queries = new ArrayList<>();


    public IndexingServiceImpl() {
        stopExecutor = true;
        executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }


    @Override
    public Response startIndexing() {

        if (getActiveTreads() != 0)
            return getFalseResponse("Индексация не запущена");

        List<SiteConfig> sitesList = list.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            int j = i;
            createNewSiteInDb(sitesList, j);
        }


        return getTrueResponse();
    }

    private void createNewSiteInDb(List<SiteConfig> sitesList, int j) {
        executor.execute(() -> {
            activeTreads = executor.getActiveCount();
            Site newSite = new Site();
            newSite.setName(sitesList.get(j).getName());
            newSite.setUrl(sitesList.get(j).getUrl());
            newSite.setStatusTime(LocalDateTime.now());
            newSite.setStatus(Status.INDEXING);
            siteRepository.save(newSite);

            try {
                ForkJoinPool pool = new ForkJoinPool();
                Indexing indexing = new Indexing(newSite.getUrl());
                Set<String> setLinks = new HashSet<>(pool.invoke(indexing));
                pool.shutdown();
                setLinks.forEach(link -> {
                    if (link.contains(getDomen(newSite.getUrl()))) setPage(newSite, link);

                });

            } catch (Exception ex) {

                newSite.setLastError(ex.getMessage());
                newSite.setStatusTime(LocalDateTime.now());
                newSite.setStatus(Status.FAILED);

                siteRepository.save(newSite);
            }
            if (!stopExecutor) {
                newSite.setLastError("Индексация остановлена пользователем");
                newSite.setStatusTime(LocalDateTime.now());
                newSite.setStatus(Status.FAILED);

            } else {
                newSite.setStatusTime(LocalDateTime.now());
                newSite.setStatus(Status.INDEXED);
            }
            siteRepository.save(newSite);

        });

    }

    private synchronized void setPage(Site site, String url) {
        int frequency = 0;

        Optional<Page> page = pageRepository.findByPath(url);

        if (page.isPresent()) return;

        Page newPage = new Page();
        newPage.setSite(site);
        newPage.setPath(url);
        frequency = 1;

        try {
            newPage.setContent(getHtml(url));
            newPage.setCode(new ResponseEntity<>(HttpStatus.OK).getStatusCodeValue());

        } catch (Exception ex) {
            newPage.setCode(new ResponseEntity<>(HttpStatus.NOT_FOUND).getStatusCodeValue());

        }

        pageRepository.save(newPage);

        if (newPage.getCode() != 200) return;

        LemmaFinder lemmaFinder = new LemmaFinder();
        LemmaFinderEn lemmaFinderEn = new LemmaFinderEn();
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(url);
        lemmas.putAll(lemmaFinderEn.collectLemmas(url));

        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {

            IndexSearch newIndex = new IndexSearch();
            Lemma newLemma = new Lemma();
            Optional<Lemma> lemma = lemmaRepository.findByLemma(entry.getKey());

            if (lemma.isEmpty()) {
                newLemma.setLemma(entry.getKey());
                newLemma.setSite(site);
                newLemma.setFrequency(frequency);
                newIndex.setPage(newPage);
                newIndex.setLemma(newLemma);
                newIndex.setRank(Float.valueOf(entry.getValue()));

                lemmaRepository.save(newLemma);
                indexSearchRepository.save(newIndex);
            } else {
                frequency = lemma.get().getFrequency() + 1;
                lemma.get().setFrequency(frequency);
                newIndex.setPage(newPage);
                newIndex.setLemma(lemma.get());
                newIndex.setRank(Float.valueOf(entry.getValue()));
                lemmaRepository.save(lemma.get());
                indexSearchRepository.save(newIndex);
            }
        }

    }

    @SneakyThrows
    public String getHtml(String link) {
        return Jsoup.connect(link.trim())
                .userAgent("Mozilla").get().html();
    }

    public String getDomen(String url) {
        return (url.contains("www")) ?
                url.substring(12).split("/", 2)[0] : url.substring(8).split("/", 2)[0];
    }


    @Override
    public Response stopIndexing() {
        if (getActiveTreads() == 0) {
            return getFalseResponse("Индексация не запущена");
        } else {

            executor.shutdown();
            stopExecutor = false;
        }
        return getTrueResponse();
    }

    @Override
    public Boolean getStopExecutor() {
        return stopExecutor;
    }

    @SneakyThrows
    @Override
    public Response indexPage(String url) {

        if (!urlIsUrl(url)) {
            return getFalseResponse("Данная страница не найдена");
        }

        String domen = getDomen(url);
        Iterable<Site> sites = siteRepository.findAll();

        int i = 0;
        for (Site site : sites) {
            if (site.getUrl().contains(domen)) {
                i++;
                Optional<Page> page = pageRepository.findByPath(url);
                page.ifPresent(this::findLemmaInDb);
                setPage(site, url);
            }
        }


        if (i == 0)
            return getFalseResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");

        return getTrueResponse();
    }

    private Response getTrueResponse() {
        return Response.builder()
                .result(true)
                .build();

    }

    private Response getFalseResponse(String error) {
        return Response.builder()
                .result(false)
                .error(error)
                .build();
    }


    private void findLemmaInDb(Page page) {
        Iterable<IndexSearch> indexSearches = indexSearchRepository.findByPageId(page.getId());
        pageRepository.deleteById(page.getId());

        for (IndexSearch indexSearch : indexSearches) {
            int lemmaId = indexSearch.getLemma().getId();
            Optional<Lemma> lemma = lemmaRepository.findById(lemmaId);
            int frequency = lemma.get().getFrequency();
            if (frequency >= 2) {
                frequency = frequency - 1;
                Lemma newLemma = lemma.get();
                newLemma.setFrequency(frequency);
                lemmaRepository.save(newLemma);
            } else {
                lemmaRepository.deleteById(lemmaId);
            }
            indexSearchRepository.deleteById(indexSearch.getId());
        }
    }


    private boolean urlIsUrl(String url) {
        String regex = "https?://[^,\\s]+";
        return url.matches(regex);
    }


    @Override
    public StatisticsResponse getStatistic() {

        StatisticsData statisticsData = new StatisticsData();
        TotalStatistics totalStatistics = new TotalStatistics();

        List<DetailedStatisticsItem> detailedStatisticsItems = new ArrayList<>();

        Iterable<Site> sites = siteRepository.findAll();
        Iterable<Page> pages = pageRepository.findAll();
        Iterable<Lemma> lemmas = lemmaRepository.findAll();


        totalStatistics.setSites((int) sites.spliterator().getExactSizeIfKnown());
        totalStatistics.setPages((int) pages.spliterator().getExactSizeIfKnown());
        totalStatistics.setLemmas((int) lemmas.spliterator().getExactSizeIfKnown());
        totalStatistics.setIndexing(true);
        statisticsData.setTotal(totalStatistics);

        StatisticsResponse statisticsResponse = new StatisticsResponse();

        if (!sites.iterator().hasNext()) {
            statisticsResponse.setResult(false);
            return statisticsResponse;
        }

        for (Site site : sites) {
            DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem();
            detailedStatisticsItem.setName(site.getName());
            detailedStatisticsItem.setUrl(site.getUrl());
            detailedStatisticsItem.setStatus(site.getStatus().toString());
            detailedStatisticsItem.setStatusTime(site.getStatusTime().getSecond());
            detailedStatisticsItem.setError(site.getLastError());


            int pageNumbers = 0;
            for (Page page : pages) {
                if (page.getSite().getId() == site.getId()) pageNumbers++;
            }

            detailedStatisticsItem.setPages(pageNumbers);

            int lemmaNumbers = 0;
            for (Lemma lemma : lemmas) {
                if (lemma.getSite().getId() == site.getId()) lemmaNumbers++;

            }
            detailedStatisticsItem.setLemmas(lemmaNumbers);
            detailedStatisticsItems.add(detailedStatisticsItem);
        }
        statisticsData.setDetailed(detailedStatisticsItems);
        statisticsResponse.setResult(true);
        statisticsResponse.setStatistics(statisticsData);

        return statisticsResponse;
    }


    @SneakyThrows
    @Override
    public SearchedData search(String query, String webSite, Integer offset, Integer limit) {

        boolean emptyQuery = query == null || query.isEmpty();
        queries.add(query);

        queries.forEach(queryList -> {
            if (!queryList.contains(query)) {
                queries = new ArrayList<>();
                searchDates = new ArrayList<>();
                newSearchData = new ArrayList<>();
            }
        });

        if (!newSearchData.isEmpty())
            return getSearchedData(searchDates.size(), getLimitInformation());

        if (emptyQuery)
            return getErrorSearchedData("Задан пустой поисковый запрос");

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


    private Information getDataSnippet(Information information, Lemma lemma) {

        String html = getHtml(information.getUri().trim());

        for (String snip : getSnippet(html, lemma.getLemma())) {
            if (information.getUri().contains(getDomen(lemma.getSite().getUrl()))) {
                information.setSnippet(snip);

            }
        }

        return information;

    }

    private boolean isEnglish(String query) {
        String regex = "[A-z, \\s]+";

        return query.matches(regex);

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
