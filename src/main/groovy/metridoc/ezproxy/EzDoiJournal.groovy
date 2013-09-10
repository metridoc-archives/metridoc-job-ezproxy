package metridoc.ezproxy

import org.hibernate.annotations.Index

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version

/**
 * Created with IntelliJ IDEA on 7/12/13
 * @author Tommy Barker
 */
@Entity
@Table(name = "ez_doi_journal")
class EzDoiJournal {
    @Id
    @GeneratedValue
    Long id
    @Version
    Long version
    @Column(unique = true, nullable = false)
    String doi
    @Column(name="article_title")
    String articleTitle
    @Column(name="journal_title")
    String journalTitle
    @Column(name="given_name")
    String givenName
    @Column(name="sur_name")
    String surName
    String volume
    String issue
    @Column(name="first_page")
    String firstPage
    @Column(name="last_page")
    String lastPage
    @Column(name="print_year")
    Integer printYear
    @Column(name="null_year")
    Integer nullYear
    @Column(name="other_year")
    Integer otherYear
    @Column(name="electronic_year")
    Integer electronicYear
    @Column(name="online_year")
    Integer onlineYear
    @Column(name="print_issn")
    String printIssn
    @Column(name="electronic_issn")
    String electronicIssn
    @Column(name="print_isbn")
    String printIsbn
    @Column(name="electronic_isbn")
    String electronicIsbn
}
