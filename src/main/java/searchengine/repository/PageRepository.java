package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {

    Optional<Page> findByPath(String path);
}
