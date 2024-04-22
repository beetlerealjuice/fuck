import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;


public class Main {


    public static void main(String[] args) throws IOException {

        String link = "https://www.playback.ru/";

        ForkJoinPool pool = new ForkJoinPool();

//        pool.invoke(myFork);

        MyFork myFork = new MyFork(link);
        Set<String> result = new HashSet<>(pool.invoke(myFork));


        pool.shutdown();


    }


    static class MyFork extends RecursiveTask<Set<String>> {


        private String link;


        public MyFork(String link) {
            this.link = link;
        }

        @SneakyThrows
        @Override
        protected Set<String> compute() {
            Thread.sleep(500);
            Set<String> links = new HashSet<>();

            String regex = "https?://[^,\\s]+";

            Document doc = Jsoup.connect(link)
                    .userAgent("Mozilla").get();     // .timeout(3000)
            Elements elements = doc.select("a");

            Set<MyFork> tasks = new HashSet<>();

            for (Element element : elements) {
                String newLink = element.attr("abs:href");
                if (newLink.matches(regex) &&
                        newLink.contains(getDomen(link)) &&
                        !links.contains(newLink)
                ) {
                    links.add(newLink);
                    MyFork myFork = new MyFork(newLink);
                    myFork.fork();
                    tasks.add(myFork);
                    System.out.println(newLink);

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
                url.substring(12) : url;
        return domen;
    }


}


