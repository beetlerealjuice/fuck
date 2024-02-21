package searchengine.utils;


import lombok.Setter;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.RecursiveAction;

@Component
public class Indexing extends RecursiveAction //<Set<String>>
{

    private final PageRepository pageRepository;


    @Setter
    private Site site;


    public Indexing(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }


    @SneakyThrows
    @Override
    protected void compute() {   //Set<String>
//        Set<String> links = new HashSet<>();
//        Set<Indexing> tasks = new HashSet<>();
//        String regex = "https://[a-z.]{1,}[^, .]+";
//        links.add(site.getUrl());


        Thread.sleep(500);

        setPage(site, site.getUrl());
//        Iterable<Page> pages = pageRepository.findAll();
//        pages.forEach(s -> {
        Elements elements;
        try {
//                System.out.println("SSS - " + s.getPath());
            elements = Jsoup.connect(site.getUrl())   // Get data from DB
                    .userAgent("Mozilla").get().select("a");        //.get().select("a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Element element : elements) {
            System.out.println(" --- " + element);
            System.out.println("Before: " + element.absUrl("href"));

            if (element.absUrl("href").contains(getDomen(site).trim())
//                        &&  !links.contains(element.absUrl("href"))
            ) {

                setPage(site, element.absUrl("href"));
                System.out.println("After: " + element.absUrl("href"));
//links.add(element.absUrl("href"));

//                    Indexing r = new Indexing(pageRepository);
//                    r.fork();
//                    tasks.add(r);
            } else break;
        }

//        });

        Indexing r = new Indexing(pageRepository);
        r.fork().join();


//        tasks.forEach(s-> System.out.println("task: " + s.toString()));
//            for (Indexing task : tasks) {
//                links.addAll(task.join());
//                System.out.println("task: " + task);
//            }
//        links.forEach(s -> System.out.println("links - " + s));
//        return links;


    }

    private void setPage(Site site, String url) {
        Optional<Page> page = pageRepository.findByPath(url);
        if (page.isEmpty()) {

            Page newPage = new Page();
            newPage.setSite(site);
            newPage.setPath(url);

            try {
                newPage.setContent(getHtml(url));
                newPage.setCode(new ResponseEntity<>(HttpStatus.OK).getStatusCodeValue());
            } catch (Exception ex) {
                newPage.setCode(new ResponseEntity<>(HttpStatus.NOT_FOUND).getStatusCodeValue());
            }

            pageRepository.save(newPage);
        } else return;

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
