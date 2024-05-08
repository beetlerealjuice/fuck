import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;


public class Main {
//    private static final ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();

    public static void main(String[] args) throws IOException {

        String link = "https://sendel.ru/";


        ForkJoinPool pool = new ForkJoinPool();

        MyFork myFork = new MyFork(link);
        Set<String> result = new HashSet<>(pool.invoke(myFork));

        pool.shutdown();
//        result.forEach(System.out::println);
        try {
            Files.write(Paths.get("file.txt"), result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}


