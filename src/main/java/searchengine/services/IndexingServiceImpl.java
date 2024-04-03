package searchengine.services;

import lombok.Getter;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.*;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.Indexing;
import searchengine.utils.LemmaFinder;

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


    public IndexingServiceImpl() {
        stopExecutor = true;
        executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }


    @Override
    public Boolean startIndexing() {
        List<SiteConfig> sitesList = list.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
//            Запуск в многопоточном режиме@
            int j = i;

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
                    Indexing indexing = new Indexing(pageRepository, lemmaRepository, indexSearchRepository);
                    indexing.setSite(newSite);
                    pool.invoke(indexing); // FJP
                    pool.shutdown();
                } catch (Exception ex) {

                    newSite.setLastError(ex.getMessage());
                    newSite.setStatusTime(LocalDateTime.now());
                    newSite.setStatus(Status.FAILED);

                    siteRepository.save(newSite);
                }
                if (stopExecutor == false) {
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
        return true;
    }

    @Override
    public Boolean stopIndexing() {

        executor.shutdown();
        stopExecutor = false;
        activeTreads = executor.getActiveCount();
//        System.out.println("Количество потоков в stopIndexing - " + activeTreads);
        return true;
    }

    @Override
    public Boolean getStopExecutor() {
        return stopExecutor;
    }

    @Override
    public Boolean indexPage(String url) {

        String domen = (url.contains("www")) ?
                url.substring(12).split("/", 2)[0] : url.substring(8).split("/", 2)[0];


        Iterable<Site> sites = siteRepository.findAll();
        int i = 0;
        for (Site site : sites) {
            if (site.getUrl().contains(domen)) {
                i++;

                try {
                    Optional<Page> page = pageRepository.findByPath(url);

                    if (!page.isEmpty()) {
                        Iterable<IndexSearch> indexSearches = indexSearchRepository.findByPageId(page.get().getId());
                        pageRepository.deleteById(page.get().getId());

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

                    Indexing indexing = new Indexing(pageRepository, lemmaRepository, indexSearchRepository);
                    indexing.setPage(site, url);


                } catch (Exception ex) {
                }
            }
        }
        if (i == 0) return false;

        return true;
    }

    @Override
    public StatisticsData getStatistic() {

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

        for (Site site : sites) {
            DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem();
            detailedStatisticsItem.setName(site.getName());
            detailedStatisticsItem.setUrl(site.getUrl());
            detailedStatisticsItem.setStatus(site.getStatus().toString());
            detailedStatisticsItem.setStatusTime(site.getStatusTime().getSecond());
            detailedStatisticsItem.setError(site.getLastError());


            int pageNumbers = 0;
            for (Page page : pages) {
                if (page.getSite().getId() == site.getId())
                    pageNumbers++;
            }

            detailedStatisticsItem.setPages(pageNumbers);

            int lemmaNumbers = 0;
            for (Lemma lemma : lemmas) {
                if (lemma.getSite().getId() == site.getId()) {
                    lemmaNumbers++;
                }
            }
            detailedStatisticsItem.setLemmas(lemmaNumbers);
            detailedStatisticsItems.add(detailedStatisticsItem);
        }
        statisticsData.setDetailed(detailedStatisticsItems);

        return statisticsData;
    }

    @SneakyThrows
    @Override
    public SearchedData search(String query) {


        LemmaFinder lemmaFinder = new LemmaFinder();
        Set<String> getLemmasFromQuery = lemmaFinder.lemmaSearcher(query);
        Iterable<Lemma> lemmas = lemmaRepository.findAll();

        List<Integer> frequencies = new ArrayList<>();
        List<Lemma> foundLemmas = new ArrayList<>();

        // Нашли соответствие лемм и поискового запроса
        for (String getLemma : getLemmasFromQuery) {
            for (Lemma lemma : lemmas) {
                if (lemma.getLemma().equals(getLemma)) {
                    frequencies.add(lemma.getFrequency());
                    foundLemmas.add(lemma);
                }

            }
        }

        SearchedData searchedData = new SearchedData();
        List<Information> dates = new ArrayList<>();


        if (!foundLemmas.isEmpty()) {

            int max = Collections.max(frequencies);
            int min = Collections.min(frequencies);
            int percent;
            if (max != min) {
                percent = max / 2;
            } else {
                percent = max;
            }

            //Удаляю леммы у которых частота больше 50%
            Iterator<Lemma> lemmaIterator = foundLemmas.iterator();
            while (lemmaIterator.hasNext()) {
                Lemma nextLemma = lemmaIterator.next();
                if (nextLemma.getFrequency() > percent) {
                    lemmaIterator.remove();
                }
            }

            Iterable<IndexSearch> indexSearches = indexSearchRepository.findAll();

            Map<Integer, Float> pageRank = new HashMap<>();


            for (Lemma lemma : foundLemmas) {

                for (IndexSearch indexSearch : indexSearches) {
                    if (indexSearch.getLemma().getId() == lemma.getId()) {


                        //Page Id
                        int idPage = indexSearch.getPage().getId();

                        //Lemma
                        String lemma1 = lemma.getLemma();

                        //Rank
                        float rank1 = indexSearch.getRank();


                        // Добавляем в мапу
                        if (pageRank.containsKey(idPage)) {
                            float newRank = pageRank.get(idPage) + rank1;
                            pageRank.replace(idPage, newRank);
                        } else {
                            pageRank.put(idPage, rank1);
                        }

                    }

                }
            }

            Map<Integer, Float> relativeRelevant = new HashMap<>();
            float maxValue = Collections.max(pageRank.values());

            for (Map.Entry<Integer, Float> entry : pageRank.entrySet()) {
                float relevantValue = entry.getValue() / maxValue;
                relativeRelevant.put(entry.getKey(), relevantValue);

            }


            Iterable<Site> sites = siteRepository.findAll();
            Iterable<Page> pages = pageRepository.findAll();


            searchedData.setCount(relativeRelevant.size());
            searchedData.setResult(true);

            for (Map.Entry<Integer, Float> entry : relativeRelevant.entrySet()) {
                Information data = new Information();

                for (Page page : pages) {
                    if (page.getId() == entry.getKey()) {
                        for (Site site : sites) {
                            if (page.getSite().getId() == site.getId()) {
                                Document document = Jsoup.parse(page.getContent());
                                String title = document.title();

                                data.setSite(site.getUrl());
                                data.setSiteName(site.getName());
                                data.setUri(page.getPath());
                                data.setTitle(title);

                                // Находим лемму для поиска ее в тексте и выделения
                                for (Lemma lemma : foundLemmas) {
                                    if (lemma.getSite().getId() == site.getId()) {

                                        String snippet = getSnippet(page.getContent(), lemma.getLemma());

                                        data.setSnippet(snippet);
                                    }
                                }

                                data.setRelevance(entry.getValue());

                            }

                        }

                    }
                }
                dates.add(data);

            }

            searchedData.setData(dates);
            return searchedData;
        }

        searchedData.setResult(true);
        searchedData.setCount(0);

        return searchedData;
    }

    @SneakyThrows
    public String getSnippet(String text, String findWord) {
        LemmaFinder lemmaFinder = new LemmaFinder();
        Set<String> lemmas = lemmaFinder.splitter(text);
        String snippet = "";
        for (String textWord : lemmas) {
            List<String> words = lemmaFinder.getNormalForms(textWord);
            for (String lemmaWord : words) {
                if (lemmaWord.contains(findWord)) {

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

                    if (index - quantityOfSubstring <= 0) {
                        start = textLength - textLength;
                    } else {
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
                    snippet = sb.toString();
                }
            }

        }

        return snippet;
    }

}
