package searchengine.utils;


import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import searchengine.model.Page;
import searchengine.model.Status;
import searchengine.repository.PageRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.RecursiveTask;


public class Indexing extends RecursiveTask<Set<String>> {

    @Autowired
    private PageRepository pageRepository;

    private String path;

    private String domen;

    public Indexing(String path, String domen) {
        this.path = path;
        this.domen = domen;

    }


    @SneakyThrows
    @Override
    protected Set<String> compute() {
//    protected void compute() {
        Set<String> links = new HashSet<>();
        Set<Indexing> tasks = new HashSet<>();

        String regex = "https://[a-z.]{1,}[^, .]+";

        links.add(path);
        Thread.sleep(500);
        Elements elements = Jsoup.connect(path.trim())
                .userAgent("Mozilla").get().select("a");
        for (Element element : elements) {
//            System.out.println("Before: " + element.absUrl("href"));

            if (element.absUrl("href").contains(domen.trim())
                    && !links.contains(element.absUrl("href"))) {

//                System.out.println("After: " + element.absUrl("href"));

                //TODO Сделать обращение к БД для проверки была ли такая ссылка заптсана в БД
                Optional<Page> pageOptional = pageRepository.findByPath(element.absUrl("href"));
                System.out.println("PageOptional - " + pageOptional.get());
                if (pageOptional.isEmpty()) {

                    setPage(element.absUrl("href"));
                }

                Indexing r = new Indexing(element.absUrl("href"), domen); //
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

    public void setPage(String link) {
        Page newPage = new Page();
//        newPage.setSite(newSite); // Потоки подвисают
        newPage.setPath(link);
        try {
            newPage.setContent(link);
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

    @Override
    public String toString() {
        return "Indexing{" +
                "path='" + path + '\'' +
                ", domen='" + domen + '\'' +
                '}';
    }


}
