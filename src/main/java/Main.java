import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;


public class Main {


    public static void main(String[] args) throws IOException {

        String link = "https://www.playback.ru/";


        ForkJoinPool pool = new ForkJoinPool();

        MyFork myFork = new MyFork(link);
        Set<String> result = new HashSet<>(pool.invoke(myFork));

        pool.shutdown();
        result.forEach(System.out::println);


    }


    static class MyFork extends RecursiveTask<ConcurrentSkipListSet<String>> {
        ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();

        private String link;


        public MyFork(String link) {
            this.link = link;
        }

        @SneakyThrows
        @Override
        protected ConcurrentSkipListSet<String> compute() {
            Thread.sleep(500);
            Set<MyFork> tasks = new HashSet<>();

            String regex = "https?://[^,\\s]+";

            Document doc = Jsoup.connect(link)
                    .userAgent("Mozilla").get();     // .timeout(3000)
            Elements elements = doc.select("a");

            for (Element element : elements) {
                String newLink = element.attr("abs:href");
                boolean checkLink = newLink.matches(regex) &&
                        newLink.contains(getDomen(link)) &&
                        !links.contains(newLink);

                if (checkLink) {
                    links.add(newLink);
//                    System.out.println(newLink);
                    continue;
                }
                if (checkLink) {
                    MyFork myFork = new MyFork(newLink);
                    myFork.fork();
                    tasks.add(myFork);

                }

            }

            tasks.forEach(task -> {
                task.join();
            });
            return links;

        }
    }

    public static String getDomen(String url) {
        String domen = (url.contains("www")) ?
                url.substring(12).split("/", 2)[0] : url.substring(8).split("/", 2)[0];


        return domen;
    }


}


