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
}
