package searchengine.utils;


import lombok.Setter;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RecursiveAction;

@Component
public class Indexing extends RecursiveAction {

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexSearchRepository;

    @Setter
    private Site site;


    public Indexing(PageRepository pageRepository, LemmaRepository lemmaRepository,
                    IndexSearchRepository indexSearchRepository) {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexSearchRepository = indexSearchRepository;
    }


    @SneakyThrows
    @Override
    protected void compute() {
        Thread.sleep(500);

        Elements elements;
        try {
            elements = Jsoup.connect(site.getUrl())
                    .userAgent("Mozilla").get().select("a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Element element : elements) {
            if (element.absUrl("href").contains(getDomen(site).trim())) {

                setPage(site, element.absUrl("href"));

            } else break;
        }


        Indexing r = new Indexing(pageRepository, lemmaRepository, indexSearchRepository);
        r.fork().join();


    }


    public synchronized void setPage(Site site, String url) {
        int frequency = 0;

        Optional<Page> page = pageRepository.findByPath(url);

        if (page.isEmpty()) {
            Page newPage = new Page();
            newPage.setSite(site);
            newPage.setPath(url);
            frequency = 1;

            try {
                newPage.setContent(getHtml(url));
                newPage.setCode(new ResponseEntity<>(HttpStatus.OK).getStatusCodeValue());

            } catch (Exception ex) {
                newPage.setCode(new ResponseEntity<>(HttpStatus.NOT_FOUND).getStatusCodeValue());

            }

            pageRepository.save(newPage);

            if (newPage.getCode() == 200) {
                LemmaFinder lemmaFinder = new LemmaFinder();
                LemmaFinderEn lemmaFinderEn = new LemmaFinderEn();
                Map<String, Integer> lemmas = lemmaFinder.collectLemmas(url);
                lemmas.putAll(lemmaFinderEn.collectLemmas(url));

                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {

                    IndexSearch newIndex = new IndexSearch();
                    Lemma newLemma = new Lemma();
                    Optional<Lemma> lemma = lemmaRepository.findByLemma(entry.getKey());

                    if (lemma.isEmpty()) {
                        newLemma.setLemma(entry.getKey());
                        newLemma.setSite(site);
                        newLemma.setFrequency(frequency);
                        newIndex.setPage(newPage);
                        newIndex.setLemma(newLemma);
                        newIndex.setRank(Float.valueOf(entry.getValue()));

                        lemmaRepository.save(newLemma);
                        indexSearchRepository.save(newIndex);
                    } else {
                        frequency = lemma.get().getFrequency() + 1;
                        lemma.get().setFrequency(frequency);
                        newIndex.setPage(newPage);
                        newIndex.setLemma(lemma.get());
                        newIndex.setRank(Float.valueOf(entry.getValue()));
                        lemmaRepository.save(lemma.get());
                        indexSearchRepository.save(newIndex);
                    }
                }
            }
        }

    }

    @SneakyThrows
    public String getHtml(String link) {
        Thread.sleep(500);
        String html = Jsoup.connect(link.trim())
                .userAgent("Mozilla").get().html();
        return html;
    }

    public String getDomen(Site site) {
        String domen = (site.getUrl().contains("www")) ?
                site.getUrl().substring(12) : site.getUrl();
        return domen;
    }


}
