package metridoc.ezproxy

import javax.persistence.Column

/**
 * Created with IntelliJ IDEA on 7/12/13
 * @author Tommy Barker
 */
class EzDoiJournal {
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
