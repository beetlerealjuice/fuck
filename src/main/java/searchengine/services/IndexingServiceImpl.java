package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;

import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.Indexing;

import java.time.LocalDateTime;
import java.util.List;
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
//    @Autowired
//    private Index1Repository indexRepository;

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
    public Boolean startIndexing() { // вместо Boolean вернуть TotalStatistic
        List<SiteConfig> sitesList = list.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
//            Запуск в многопоточном режиме@
            int j = i;

            executor.execute(() -> {
                activeTreads = executor.getActiveCount();
//                while (stopExecutor) {
                Site newSite = new Site();
                newSite.setName(sitesList.get(j).getName());
                newSite.setUrl(sitesList.get(j).getUrl());
                newSite.setStatusTime(LocalDateTime.now());
                newSite.setStatus(Status.INDEXING);
                siteRepository.save(newSite);

//                    System.out.println("Потоки до - " + activeTreads);
//                synchronized (newSite) {
//                    Set<String> links = new ConcurrentSkipListSet<>();

                try {
                    ForkJoinPool pool = new ForkJoinPool();
                    Indexing indexing = new Indexing(pageRepository);
                    indexing.setSite(newSite);
//                        links =
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
//        String regex = "https://[a-z.]{1,}[^, .]+";
//        Optional<Page> page = pageRepository.findByPath(url);
//
//
//        String domen = (url.contains("www")) ?
//                url.substring(12) : url;
//
//
//        Optional<Site> site = siteRepository.findByUrl(domen);
//
//        if (page.isEmpty()) {
//
//            Page newPage = new Page();
////            newPage.setSite(site);
//            newPage.setPath(url);
//        }
//        Index newIndex = new Index();


        return true;
    }


}
