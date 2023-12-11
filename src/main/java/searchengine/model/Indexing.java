package searchengine.model;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveTask;


public class Indexing extends RecursiveTask<Set<String>> {

    private static Set<String> checkURL = new HashSet<>();
    private static String path;
    private static String domen;


    public Indexing(String path, String domen) {
        this.path = path;
        this.domen = domen;

    }


    @SneakyThrows
    @Override
    protected Set<String> compute() {

        Set<String> links = new HashSet<>();
        Set<Indexing> tasks = new HashSet<>();

        String regex = "https://[a-z.]{1,}[^, .]+";

        links.add(path);
        Thread.sleep(500);
        Elements elements = Jsoup.connect(path.trim())
                .userAgent("Mozilla").get().select("a");
        for (Element element : elements) {
            if (element.absUrl("href").contains(domen.trim()) // проверка ссылок не ведет ли на стороние ресурсы
                    && element.absUrl("href").matches(regex)
                    && !checkURL.contains(element.absUrl("href"))) {
                Indexing r = new Indexing(element.absUrl("href"), domen); //
                r.fork();
                tasks.add(r);
                checkURL.add(element.absUrl("href"));
            } else break;
        }

        for (Indexing task : tasks) {
            links.addAll(task.join());
        }

        return links;
    }

}
