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
//    private static final ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();

    public static void main(String[] args) throws IOException {

        String link = "https://www.playback.ru/";


        ForkJoinPool pool = new ForkJoinPool();

        MyFork myFork = new MyFork(link);
        Set<String> result = new HashSet<>(pool.invoke(myFork));

        pool.shutdown();
//        result.forEach(System.out::println);


    }

}


