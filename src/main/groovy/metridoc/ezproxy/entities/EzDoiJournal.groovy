package metridoc.ezproxy.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 7/12/13
 * @author Tommy Barker
 */
@Entity
class EzDoiJournal {
    String doi
    String articleTitle
    String journalTitle
    String givenName
    String surName
    String volume
    String issue
    String firstPage
    String lastPage
    Integer printYear
    Integer nullYear
    Integer otherYear
    Integer electronicYear
    Integer onlineYear
    String printIssn
    String electronicIssn
    String printIsbn
    String electronicIsbn

    static constraints = {
        doi(unique: true)
        articleTitle(nullable: true)
        journalTitle(nullable: true)
        givenName(nullable: true)
        surName(nullable: true)
        volume(nullable: true)
        issue(nullable: true)
        firstPage(nullable: true)
        lastPage(nullable: true)
        printYear(nullable: true)
        nullYear(nullable: true)
        otherYear(nullable: true)
        electronicYear(nullable: true)
        onlineYear(nullable: true)
        printIssn(nullable: true)
        electronicIssn(nullable: true)
        printIsbn(nullable: true)
        electronicIsbn(nullable: true)
    }
}
