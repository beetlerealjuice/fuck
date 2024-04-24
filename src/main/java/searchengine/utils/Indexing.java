package searchengine.utils;


import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;

//@Component
public class Indexing extends RecursiveTask<ConcurrentSkipListSet<String>> {

    ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();
    private String link;

    public Indexing(String link) {
        this.link = link;
    }


    @SneakyThrows
    @Override
    protected ConcurrentSkipListSet<String> compute() {
        Thread.sleep(500);
        Set<Indexing> tasks = new HashSet<>();
        String regex = "https?://[^,\\s]+";

        Elements elements;
        try {
            elements = Jsoup.connect(link)
                    .userAgent("Mozilla").get().select("a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Element element : elements) {
            String link = element.absUrl("href");
            boolean checkLink = link.matches(regex) &&
                    !links.contains(link) && !link.contains(".pdf");

            if (checkLink) {
                links.add(link);
                continue;

            }

            if (checkLink) {
                Indexing indexing = new Indexing(link);
                indexing.fork();
                tasks.add(indexing);

            }

        }

        tasks.forEach(task -> {
            task.join();
        });
        return links;

    }

}
