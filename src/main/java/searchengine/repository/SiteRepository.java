package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);
}
