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
public class Indexing extends RecursiveAction //<Set<String>>
{

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

        setPage(site, site.getUrl());
        Elements elements;
        try {
            elements = Jsoup.connect(site.getUrl())   // Get data from DB
                    .userAgent("Mozilla").get().select("a");        //.get().select("a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Element element : elements) {
            System.out.println(" --- " + element);
            System.out.println("Before: " + element.absUrl("href"));

            if (element.absUrl("href").contains(getDomen(site).trim())
//                        &&  !links.contains(element.absUrl("href"))
            ) {

                setPage(site, element.absUrl("href"));
                System.out.println("After: " + element.absUrl("href"));
//links.add(element.absUrl("href"));

//                    Indexing r = new Indexing(pageRepository);
//                    r.fork();
//                    tasks.add(r);
            } else break;
        }

//        });

        Indexing r = new Indexing(pageRepository, lemmaRepository, indexSearchRepository);
        r.fork().join();


//        tasks.forEach(s-> System.out.println("task: " + s.toString()));
//            for (Indexing task : tasks) {
//                links.addAll(task.join());
//                System.out.println("task: " + task);
//            }
//        links.forEach(s -> System.out.println("links - " + s));
//        return links;


    }

    public synchronized void setPage(Site site, String url) {
        int frequency = 0;

        try {
            Optional<Page> page = pageRepository.findByPath(url);
            System.out.println("Page - " + page.get().getPath());
            if (!page.isEmpty()) {

                System.out.println("delete from page where id= " + page.get().getId());
                Iterable<IndexSearch> indexSearches = indexSearchRepository.findByPageId(page.get().getId());
                pageRepository.deleteById(page.get().getId());


                for (IndexSearch indexSearch : indexSearches) {
                    System.out.println("IndexSearch - " + indexSearch);

                    int lemmaId = indexSearch.getLemma().getId();
                    Optional<Lemma> lemma = lemmaRepository.findById(lemmaId);
                    frequency = lemma.get().getFrequency();
                    if (frequency >= 2) {
                        frequency = frequency - 1;
                        Lemma newLemma = lemma.get();
                        newLemma.setFrequency(frequency);
                        lemmaRepository.save(newLemma);
                    } else {
                        lemmaRepository.deleteById(lemmaId);
                    }
                    System.out.println("indexSearchRepository.deleteById = " + indexSearch.getId());
                    indexSearchRepository.deleteById(indexSearch.getId());

                }
            }

        } catch (Exception ex) {
        }

        Page newPage = new Page();
        newPage.setSite(site);
        newPage.setPath(url);
        frequency = 1;
//        System.out.println("newPage - " + site.getUrl() + " page - " + newPage.getPath());

        LemmaFinder lemmaFinder = new LemmaFinder();
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(url);


        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            IndexSearch newIndex = new IndexSearch();
            Lemma newLemma = new Lemma();

//            System.out.println(entry.getKey() + " - " + entry.getValue());
            Optional<Lemma> lemma = lemmaRepository.findByLemma(entry.getKey());
//            System.out.println("Lemma in DB - " + lemma.get().getLemma());

            if (lemma.isEmpty()) {

//                System.out.println("Lemma is empty");
                newLemma.setLemma(entry.getKey());
                newLemma.setSite(site);
//               System.out.println("Frequency - " + frequency);
                newLemma.setFrequency(frequency);
                lemmaRepository.save(newLemma);

                newIndex.setPage(newPage);
                newIndex.setLemma(newLemma);
                newIndex.setRank(Float.valueOf(entry.getValue()));
//                System.out.println("NewIndex = " + newIndex.getRank());
                indexSearchRepository.save(newIndex);

            } else {

                frequency = lemma.get().getFrequency() + 1;
//               System.out.println("Frequency - " + frequency);

                newLemma.setFrequency(frequency);

                lemmaRepository.save(newLemma);

                newIndex.setPage(newPage);
                newIndex.setLemma(newLemma);
                newIndex.setRank(Float.valueOf(entry.getValue()));

                indexSearchRepository.save(newIndex);

            }
        }

        try {
            newPage.setContent(getHtml(url));
            newPage.setCode(new ResponseEntity<>(HttpStatus.OK).getStatusCodeValue());
        } catch (Exception ex) {
            newPage.setCode(new ResponseEntity<>(HttpStatus.NOT_FOUND).getStatusCodeValue());
        }

        pageRepository.save(newPage);

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
