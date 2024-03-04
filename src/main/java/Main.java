import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        String url = "https://lenta.ru/";
        String domen = (url.contains("www")) ?
                url.substring(12).split("/", 2)[0] : url.substring(8).split("/", 2)[0];
        System.out.println(domen);


//        LuceneMorphology luceneMorph =
//                new RussianLuceneMorphology();
//
//        List<String> wordBaseForms =
//                luceneMorph.getNormalForms("леса");
//        wordBaseForms.forEach(System.out::println);
//
//        String text = "Повторное появление леопарда в Осетии позволяет предположить,\n" +
//                "что леопард постоянно обитает в некоторых районах Северного\n" +
//                "Кавказа.";
//
//
//        LemmaFinder lemmaFinder = new LemmaFinder();
////        lemmaFinder.collectLemmas(text);
//        Document elements;
//        try {
////                System.out.println("SSS - " + s.getPath());
//            elements = Jsoup.connect("https://www.lenta.ru")   // Get data from DB
//                    .userAgent("Mozilla").get();        //.get().select("a");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        String html = elements.html();
//        String result = elements.text();
////        String result1 = html.replaceAll("<[^>]*>\\n", "");
//
//
////        System.out.println(result + "\n");
////        System.out.println(result);
//
//        lemmaFinder.collectLemmas(result);


    }

}
