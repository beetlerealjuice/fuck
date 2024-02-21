package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import searchengine.model.IndexSearch;

@Repository
public interface IndexSearchRepository extends CrudRepository<IndexSearch, Integer> {
}
