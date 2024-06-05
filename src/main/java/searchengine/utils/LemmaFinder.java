package searchengine.utils;

import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

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
            elements = Jsoup.connect(url)
                    .userAgent("Mozilla").get();
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

        return lemmas;
    }

    @SneakyThrows
    public Set<String> lemmaSearcher(String query) {
        String[] textArray = arrayContainsRussianWords(query);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {


            if (!word.isEmpty() && isCorrectWordForm(word)
            ) {

                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                lemmaSet.addAll(luceneMorphology.getNormalForms(word));
            }
        }

        return lemmaSet;
    }


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

    @SneakyThrows
    private boolean isCorrectWordForm(String word) {
        String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }

    @SneakyThrows
    public Set<String> splitter(String query) {
        String[] textArray = arrayContainsRussianWords(query);
        Set<String> lemmaSet = new HashSet<>();
        lemmaSet.addAll(Arrays.asList(textArray));
        return lemmaSet;
    }

    @SneakyThrows
    public List<String> getNormalForms(String word) {
        List<String> wordBaseForms = luceneMorphology.getNormalForms(word);
        return wordBaseForms;
    }

}



