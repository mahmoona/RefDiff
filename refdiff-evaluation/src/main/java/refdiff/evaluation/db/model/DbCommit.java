package refdiff.evaluation.db.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "commit", uniqueConstraints = { @UniqueConstraint(columnNames = { "repository", "sha1" }) })
public class DbCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_commit")
    @SequenceGenerator(name = "seq_commit", initialValue = 1)
    private Integer id;

    private String sha1;

    @ManyToOne
    @JoinColumn(name = "repository")
    private DbRepository repository;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public DbRepository getRepository() {
        return repository;
    }

    public void setRepository(DbRepository repository) {
        this.repository = repository;
    }

}
