package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.Optional;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    Optional<Lemma> findByLemma(String lemma);

    Iterable<Lemma> findBySiteId(Integer id);
}
