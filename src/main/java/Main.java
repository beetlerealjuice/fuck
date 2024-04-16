import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;


public class Main {

    public static void main(String[] args) throws IOException {

        String link = "https://www.playback.ru";

        ForkJoinPool pool = new ForkJoinPool();
        MyFork myFork = new MyFork(link);
//        pool.invoke(myFork);

        List<String> result = new ArrayList<>(pool.invoke(myFork));

        pool.shutdown();
    }


    static class MyFork extends RecursiveTask<List<String>> {

        private String link;


        public MyFork(String link) {
            this.link = link;
        }

        @SneakyThrows
        @Override
        protected List<String> compute() {
            Thread.sleep(500);
            List<String> links = new ArrayList<>();

            String regex = "https?://[^,\\s]+";

            Document doc = Jsoup.connect(link)
                    .userAgent("Mozilla").get();     // .timeout(3000)
            Elements elements = doc.select("a");

            List<MyFork> tasks = new ArrayList<>();

            for (Element element : elements) {
                if (element.attr("abs:href").matches(regex)) {
                    String newLink = element.attr("abs:href");


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
}


