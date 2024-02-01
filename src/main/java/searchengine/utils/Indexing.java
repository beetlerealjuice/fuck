package searchengine.utils;


import lombok.Setter;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import searchengine.controllers.ApiController;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.services.IndexingServiceImpl;

import java.util.*;
import java.util.concurrent.RecursiveTask;

@Component
public class Indexing extends RecursiveTask<Set<String>> {



//    @Autowired
    private final PageRepository pageRepository;


    @Setter
    private Site site;


    public Indexing(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }


    @SneakyThrows
    @Override
    protected Set<String> compute() {
//    protected void compute() {
        Set<String> links = new HashSet<>();
        Set<Indexing> tasks = new HashSet<>();


            String regex = "https://[a-z.]{1,}[^, .]+";

            links.add(site.getUrl());
            Thread.sleep(500);
            Elements elements = Jsoup.connect(site.getUrl().trim())
                    .userAgent("Mozilla").get().select("a");
            for (Element element : elements) {
                System.out.println("Before: " + element.absUrl("href"));
                if (element.absUrl("href").contains(getDomen(site).trim())
                        && !links.contains(element.absUrl("href"))) {

                    setPage(site, element.absUrl("href"));
                    System.out.println("After: " + element.absUrl("href"));

                    Indexing r = new Indexing(pageRepository);
                    r.fork();
                    tasks.add(r);

                } else break;
            }
//        tasks.forEach(s-> System.out.println("task: " + s.toString()));

            for (Indexing task : tasks) {
                links.addAll(task.join());
            }


            links.forEach(s -> System.out.println("links - " + s));


            return links;


    }

    public void setPage(Site site, String url) {
        Page newPage = new Page();
        newPage.setSite(site); // Потоки подвисают

        newPage.setPath(url);

        try {
            newPage.setContent(getHtml(url));
            newPage.setCode(new ResponseEntity<>(HttpStatus.OK).getStatusCodeValue());
        } catch (Exception ex) {
            newPage.setCode(new ResponseEntity<>(HttpStatus.NOT_FOUND).getStatusCodeValue());
        }

        pageRepository.save(newPage);

    }

    @SneakyThrows
    public String getHtml(String link) {
        Thread.sleep(500);
        String html = Jsoup.connect(link.trim())
                .userAgent("Mozilla").get().html();
        return html;
    }

    public String getDomen(Site site) {
        String domen = (site.getUrl().contains("www")) ?
                site.getUrl().substring(12) : site.getUrl();
        return domen;
    }


}
