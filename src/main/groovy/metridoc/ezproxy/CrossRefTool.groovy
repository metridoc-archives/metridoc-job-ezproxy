package metridoc.ezproxy

import groovy.util.logging.Slf4j
import groovy.xml.QName
import metridoc.core.tools.DefaultTool

/**
 * Created with IntelliJ IDEA on 6/18/13
 * @author Tommy Barker
 */
@Slf4j
class CrossRefTool extends DefaultTool {
    String crossRefUsername
    String crossRefPassword
    Closure<CrossRefResponse> searchCache
    static String ENCODING = "utf-8"
    static final String CROSS_REF_BASE_URL = "http://www.crossref.org/openurl/?noredirect=true&pid="

    CrossRefResponse resolveDoi(String doi) {
        CrossRefResponse response

        if(searchCache) {
            response = searchCache.call(doi)
            if(response) return response
        }

        assert crossRefUsername && crossRefPassword: "cross ref credentials need to be set to resolve doi $doi"
        def url = createCrossRefUrl(crossRefUsername, crossRefPassword, doi)
        response = new CrossRefResponse(doi: doi)
        def responseText
        responseText = url.getText(ENCODING)
        if (responseText.contains("The login you supplied is not recognized")) {
            response.loginFailure = true
            return response
        }

        if (responseText.contains("Malformed DOI")) {
            response.malformedDoi = true
            return response
        }

        processCrossRefXml(responseText, response)


        return response
    }

    protected void processCrossRefXml(String responseText, response) {
        def result = parseXml(responseText)
        def status = result.status
        if (status == 'resolved') {
            result.each { key, value ->
                response."${key}" = value
            }
        } else if (status == 'unresolved') {
            response.unresolved = true
        } else if (status == 'malformed') {
            response.malformedDoi = true
        } else {
            throw new RuntimeException("Unexpected response occurred from CrossRef, should have a status of resolved or unresolved")
        }
    }

    static URL createCrossRefUrl(String userName, String password, String doi) {
        def encodedDoi = URLEncoder.encode(doi, ENCODING)
        return new URL("$CROSS_REF_BASE_URL$userName:$password&id=$encodedDoi")
    }

    private static Map parseXml(String xml) {
        def result = [:]
        def node = new XmlParser().parseText(xml);
        def bodyQuery = node.query_result.body.query

        def contributor = bodyQuery.contributors.contributor.find {
            it["@sequence"] == "first"
        }
        if (contributor) {
            result.givenName = getItem(contributor.given_name)
            result.surName = getItem(contributor.surname)
        }

        result.status = bodyQuery["@status"].text()
        result.volume = getItem(bodyQuery.volume)
        result.issue = getItem(bodyQuery.issue)
        result.firstPage = getItem(bodyQuery.first_page)
        result.lastPage = getItem(bodyQuery.last_page)

        result.journalTitle = bodyQuery.journal_title.text()
        result.articleTitle = getItem(bodyQuery.article_title)

        multiValueSearch(bodyQuery.year as NodeList, "media_type", result) { Integer.valueOf(it) }
        multiValueSearch(bodyQuery.issn as NodeList, "type", result)
        multiValueSearch(bodyQuery.isbn as NodeList, "type", result)

        return result
    }

    private static void multiValueSearch(NodeList items, String typeAttribute, Map result) {
        multiValueSearch(items, typeAttribute, result, null)
    }

    private static void multiValueSearch(NodeList items, String typeAttribute, Map result, Closure closure) {
        items.each { Node it ->
            QName name = it.name() as QName
            def localName = name.localPart.capitalize()
            def usedAttribute = "@$typeAttribute"
            def lookup = "${it[usedAttribute]}$localName"
            def notInResult = result[lookup] == null
            if (notInResult) {
                if (closure) {
                    result[lookup] = closure.call(getItem(it))
                } else {
                    result[lookup] = getItem(it)
                }
            }
        }
    }

    private static String getItem(item) {
        item.text() ?: null
    }
}

class CrossRefResponse {
    boolean loginFailure = false
    boolean malformedDoi = false

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
    Integer electronicYear
    Integer onlineYear
    String printIssn
    String electronicIssn
    String printIsbn
    String electronicIsbn
}