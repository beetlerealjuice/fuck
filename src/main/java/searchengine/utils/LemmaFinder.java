package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LemmaFinder {
    private static final LuceneMorphology luceneMorphology;

    static {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public Map<String, Integer> collectLemmas(String url) {
        Document elements;
        try {
            elements = Jsoup.connect(url)   // Get data from DB
                    .userAgent("Mozilla").get();        //.get().select("a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String text = elements.text();

        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }


            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
//        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
//            System.out.println(entry.getKey() + " - " + entry.getValue());
//        }
        return lemmas;
    }


    //    /**
//     * @param text текст из которого собираем все леммы
//     * @return набор уникальных лемм найденных в тексте
//     */
//    public Set<String> getLemmaSet(String text) {
//        String[] textArray = arrayContainsRussianWords(text);
//        Set<String> lemmaSet = new HashSet<>();
//        for (String word : textArray) {
//            if (!word.isEmpty() && isCorrectWordForm(word)) {
//                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
//                if (anyWordBaseBelongToParticle(wordBaseForms)) {
//                    continue;
//                }
//                lemmaSet.addAll(luceneMorphology.getNormalForms(word));
//            }
//        }
//        return lemmaSet;
//    }
//
    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
//
//    private boolean isCorrectWordForm(String word) {
//        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
//        for (String morphInfo : wordInfo) {
//            if (morphInfo.matches(WORD_TYPE_REGEX)) {
//                return false;
//            }
//        }
//        return true;
//    }

    public static String html2text(String html) {
        System.out.println(Jsoup.parse(html).text());
        return Jsoup.parse(html).text();
    }

}



