package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    //@Query("SELECT p FROM Page p where p.path = :path")
    Optional<Page> findByPath(String path);
}
