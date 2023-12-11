package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(indexes = @Index(columnList = "path")) // Установить индекс для path"
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)")
    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
}
