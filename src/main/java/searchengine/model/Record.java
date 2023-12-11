package searchengine.model;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import searchengine.config.SitesList;
import searchengine.controllers.ApiController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

public class Record {
    private String exception;
    private Site site;


    public synchronized Site getSiteIndexing(Site sites) {
        Site newSite = new Site();

        newSite.setName(sites.getName());
        newSite.setUrl(sites.getUrl());
        newSite.setStatusTime(LocalDateTime.now());
        newSite.setStatus(Status.INDEXING);
        site = newSite;

        return site;
    }

    public synchronized List<Page> getPages() {
        Set<String> links = getLinks();
//        links.forEach((s) -> System.out.println("getPages - links: " + s));

        List<Page> pages = new ArrayList<>();
        if (!links.isEmpty()) {
//            System.out.println("Size of getLinks - " + links.size());
            for (String link : links) {
                Page newPage = new Page();
                newPage.setSite(site); // Потоки подвисают
                newPage.setPath(link);
                try {
                    newPage.setContent(getHtml(link));
                    newPage.setCode(new ResponseEntity<>(HttpStatus.OK).getStatusCodeValue());
                } catch (Exception ex) {
                    newPage.setCode(new ResponseEntity<>(HttpStatus.NOT_FOUND).getStatusCodeValue());
                }
                pages.add(newPage);
            }
            return pages;
        }
        return null;
    }

    public synchronized Set<String> getLinks() {
        Set<String> links = new ConcurrentSkipListSet<>();
        try {
            ForkJoinPool pool = new ForkJoinPool();

            links = pool.invoke(new Indexing(site.getUrl(), getDomenNoWww(site)));
            pool.shutdown();

        } catch (Exception ex) {
            setException(ex.getMessage());
        }

//        links.forEach(System.out::println);

        return links;
    }


    public synchronized Site getSiteIndexed() {

        if (getPages() == null) {
            site.setLastError(getException());
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.FAILED);

        } else {
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.INDEXED);
        }
        return site;
    }

    public synchronized String getException() {
        return exception;
    }

    public synchronized void setException(String exception) {
        this.exception = exception;
    }


    // Код для домена, для проверки, чтобы небыло ссылок на стороннние ресурсы
    private String getDomenNoWww(Site site) {
        Site getSite = site;
        String url = getSite.getUrl();
        String domen = (url.contains("www")) ?
                url.substring(12) : url;
        return domen;
    }

    @SneakyThrows
    public String getHtml(String link) // throws IOException, InterruptedException
    {
        Thread.sleep(500);
        String html = Jsoup.connect(link.trim())
                .userAgent("Mozilla").get().html();
        return html;
    }

}
