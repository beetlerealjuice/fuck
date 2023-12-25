package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {

    Optional<Page> findBySiteId(Site siteId);
    Optional<Page>findByPath(String path);
}
