package metridoc.ezproxy.services

import metridoc.iterators.Record
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
class EzproxyIteratorServiceSpec extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    void "valid urls with invalid syntax are invalid"() {
        given: "a valid url with bad uri syntax"
        def url = "http://google.com/blah%2"

        when:
        new URL(url)

        then:
        noExceptionThrown()

        when:
        new URL(url).toURI()

        then:
        thrown(URISyntaxException)

        when:
        new EzproxyIteratorService().validateUrl(url)

        then:
        thrown(AssertionError)
    }

    def "if result is empty or null an AssertionError will occur"() {
        given: "a file with a lot of lines"
        def file = folder.newFile("fileWithLotsOfLines")
        file.withPrintWriter {PrintWriter writer ->
            (0..10000).each {
                writer.println(it)
            }
        }

        and: "a parser that always returns null"
        def nullParser = {new Record(body: null)}

        and: "a parser that always returns an empty Map"
        def emptyParser = {new Record(body: [:])}

        when: "when next is called for null parser iterator"
        def record = new EzproxyIteratorService(
                inputStream: file.newInputStream(),
                parser: nullParser,
                delimiter: "\\|\\|",
                proxyDate: 1,
                url: 2,
                ezproxyId: 3
        ).next()

        then: "an AssertionError is thrown"
        record.throwable instanceof AssertionError

        when: "when next is called for empty parser iterator"
        record = new EzproxyIteratorService(
                inputStream: file.newInputStream(),
                parser: emptyParser,
                delimiter: "\\|\\|",
                proxyDate: 1,
                url: 2,
                ezproxyId: 3
        ).next()

        then: "an AssertionError is thrown"
        record.throwable instanceof AssertionError
    }

    def "test cases where target file makes no sense and we are doing a preview"() {
        given:
        def badFile = folder.newFile("badFile.log")
        badFile.withPrintWriter {writer ->
            writer.println("lkjhasdflkjh")
            writer.println("lkjhas||dflkjh")
        }

        when:
        new EzproxyIteratorService(
                inputStream: badFile.newInputStream()
        ).preview()

        then:
        thrown(AssertionError)
    }
}
