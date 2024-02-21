import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.utils.LemmaFinder;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();

        List<String> wordBaseForms =
                luceneMorph.getMorphInfo("в");
        wordBaseForms.forEach(System.out::println);

        String text = "Повторное появление леопарда в Осетии позволяет предположить,\n" +
                "что леопард постоянно обитает в некоторых районах Северного\n" +
                "Кавказа.";


        LemmaFinder lemmaFinder = new LemmaFinder();
//        lemmaFinder.collectLemmas(text);
        Document elements;
        try {
//                System.out.println("SSS - " + s.getPath());
            elements = Jsoup.connect("https://www.lenta.ru")   // Get data from DB
                    .userAgent("Mozilla").get();        //.get().select("a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String html = elements.html();
        String result = elements.text();
//        String result1 = html.replaceAll("<[^>]*>\\n", "");


        System.out.println(result + "\n");
//        System.out.println(result);

    }

}
