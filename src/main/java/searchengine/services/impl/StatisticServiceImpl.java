package searchengine.services.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.StatisticService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class StatisticServiceImpl implements StatisticService {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;


    @Override
    public StatisticsResponse getStatistic() {

        StatisticsData statisticsData = new StatisticsData();
        TotalStatistics totalStatistics = new TotalStatistics();

        List<DetailedStatisticsItem> detailedStatisticsItems = new ArrayList<>();

        Iterable<Site> sites = siteRepository.findAll();
        Iterable<Page> pages = pageRepository.findAll();
        Iterable<Lemma> lemmas = lemmaRepository.findAll();


        totalStatistics.setSites((int) sites.spliterator().getExactSizeIfKnown());
        totalStatistics.setPages((int) pages.spliterator().getExactSizeIfKnown());
        totalStatistics.setLemmas((int) lemmas.spliterator().getExactSizeIfKnown());
        totalStatistics.setIndexing(true);
        statisticsData.setTotal(totalStatistics);

        StatisticsResponse statisticsResponse = new StatisticsResponse();


        if (!sites.iterator().hasNext()) {
            statisticsResponse.setResult(false);
            return statisticsResponse;
        }

        for (Site site : sites) {
            DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem();
            detailedStatisticsItem.setName(site.getName());
            detailedStatisticsItem.setUrl(site.getUrl());
            detailedStatisticsItem.setStatus(site.getStatus().toString());
            detailedStatisticsItem.setStatusTime(site.getStatusTime().getSecond());
            detailedStatisticsItem.setError(site.getLastError());


            int pageNumbers = 0;
            for (Page page : pages) {
                if (page.getSite().getId() == site.getId()) pageNumbers++;
            }

            detailedStatisticsItem.setPages(pageNumbers);

            int lemmaNumbers = 0;
            for (Lemma lemma : lemmas) {
                if (lemma.getSite().getId() == site.getId()) lemmaNumbers++;

            }
            detailedStatisticsItem.setLemmas(lemmaNumbers);
            detailedStatisticsItems.add(detailedStatisticsItem);
        }
        statisticsData.setDetailed(detailedStatisticsItems);
        statisticsResponse.setResult(true);
        statisticsResponse.setStatistics(statisticsData);

        return statisticsResponse;
    }
}
