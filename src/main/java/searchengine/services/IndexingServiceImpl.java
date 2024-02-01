package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.utils.Indexing;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
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

    public static boolean stopExecutor = false;
    ThreadPoolExecutor executor;

    @Override
    public Boolean startIndexing() { // вместо Boolean вернуть TotalStatistic
        List<SiteConfig> sitesList = list.getSites();

        for (int i = 0; i < sitesList.size(); i++) {
//            Запуск в многопоточном режиме
            int j = i;
            executor =
                    (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            executor.execute(() -> {
                while (stopExecutor == false) {


                    // Останавливаем потоки
//                while (stopIndexing() == true){
//                    executor.shutdown();
//                }

                    Site newSite = new Site();
                    newSite.setName(sitesList.get(j).getName());
                    newSite.setUrl(sitesList.get(j).getUrl());
                    newSite.setStatusTime(LocalDateTime.now());
                    newSite.setStatus(Status.INDEXING);
                    siteRepository.save(newSite);

//                synchronized (newSite) {
                    Set<String> links = new ConcurrentSkipListSet<>();

                    try {
                        ForkJoinPool pool = new ForkJoinPool();
                        Indexing indexing = new Indexing(pageRepository);
                        indexing.setSite(newSite);
                        links = pool.invoke(indexing); // FJP
                        pool.shutdown();
                    } catch (Exception ex) {
                        newSite.setLastError(ex.getMessage());
                        newSite.setStatusTime(LocalDateTime.now());
                        newSite.setStatus(Status.FAILED);
                        siteRepository.save(newSite);
                    }
                    if (!links.isEmpty()) {

                        newSite.setStatusTime(LocalDateTime.now());
                        newSite.setStatus(Status.INDEXED);
                        siteRepository.save(newSite);
                    }
                    stopIndexing();

                }
            });

        }
        return true;
    }

    @Override
    public Boolean stopIndexing() {
        executor.shutdown();
        stopExecutor = true;
        return stopExecutor;
    }


}
