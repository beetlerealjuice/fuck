package searchengine.services;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.utils.Indexing;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
//    @Autowired
//    private PageRepository pageRepository;

    @Override
    public Boolean startIndexing() {
        List<SiteConfig> sitesList = list.getSites();

        for (int i = 0; i < sitesList.size(); i++) {
//            Запуск в многопоточном режиме
            int j = i;
            ThreadPoolExecutor executor =
                    (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            executor.execute(() -> {


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
                    Indexing indexing = new Indexing();
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
//                    for (String link : links) {
//                        Page newPage = new Page();
//                        newPage.setSite(newSite); // Потоки подвисают
//                        newPage.setPath(newSite.getUrl());
//                        try {
//                            newPage.setContent(getHtml(link));
//                            newPage.setCode(new ResponseEntity<>(HttpStatus.OK).getStatusCodeValue());
//                        } catch (Exception ex) {
//                            newPage.setCode(new ResponseEntity<>(HttpStatus.NOT_FOUND).getStatusCodeValue());
//                        }
//                        pageRepository.save(newPage);
//                    }
                    newSite.setStatusTime(LocalDateTime.now());
                    newSite.setStatus(Status.INDEXED);
                    siteRepository.save(newSite);
                }

            });

        }
        return true;
    }

//    @SneakyThrows
//    public String getHtml(String link) {
//        Thread.sleep(500);
//        String html = Jsoup.connect(link.trim())
//                .userAgent("Mozilla").get().html();
//        return html;
//    }
}
