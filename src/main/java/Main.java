import org.jsoup.Jsoup;
import searchengine.utils.LemmaFinder;

import java.io.IOException;
import java.util.List;
import java.util.Set;


public class Main {

    public static void main(String[] args) throws IOException {


        String link = "https://www.svetlovka.ru/#";

        String text = Jsoup.connect(link.trim())
                .userAgent("Mozilla").get().html();

        String findWord = "знакомый";
//        String text = "Знакомые книги «Волшебная пыльца», «первая любовь» и «лунная дорожка»: " +
//                "как через ароматы открыть для себя давно";

//                String text = "Повторное появление леопарда в Осетии позволяет предположить,\n" +
//                "что леопард постоянно обитает в некоторых районах Северного\n" +
//                "Кавказа.";

        LemmaFinder lemmaFinder = new LemmaFinder();
        Set<String> lemmas = lemmaFinder.splitter(text);

        for (String textWord : lemmas) {
            List<String> words = lemmaFinder.getNormalForms(textWord);
            for (String lemmaWord : words) {
                if (lemmaWord.contains(findWord)) {

                    int index = text.indexOf(textWord);
                    String word;
                    if (index == -1) {
                        word = Character.toUpperCase(textWord.charAt(0)) + textWord.substring(1);
                        index = text.indexOf(word);
                    } else {
                        word = textWord;
                    }

                    int sizeOfWord = word.length();

                    int textLength = text.length();
                    int start = 0;
                    int finish = 0;
                    int quantityOfSubstring = 100;

                    if (index - quantityOfSubstring <= 0) {
                        start = textLength - textLength;
                    } else {
                        start = index - quantityOfSubstring;
                    }
                    if (index + quantityOfSubstring >= textLength) {
                        finish = textLength;
                    } else {
                        finish = index + quantityOfSubstring;
                    }


                    String newText = text.substring(start, finish);
                    int newIndex = newText.indexOf(word);


                    StringBuilder sb = new StringBuilder(newText);
                    sb.insert(newIndex + sizeOfWord, "</b>");
                    sb.insert(newIndex, "<b>");
                    System.out.println(sb.toString());


//                    System.out.println(textWord);
                }
            }


        }

    }
}


