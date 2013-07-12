package metridoc.ezproxy

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
class EzproxyIteratorSpec extends Specification {

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
        def nullParser = {null}

        and: "a parser that always returns an empty Map"
        def emptyParser = {[:]}

        when: "when next is called for null parser iterator"
        def record = new EzproxyIterator(
                inputStream: file.newInputStream(),
                ezParser: nullParser
        ).next()

        then: "an AssertionError is thrown"
        record.throwable instanceof AssertionError

        when: "when next is called for empty parser iterator"
        record = new EzproxyIterator(
                inputStream: file.newInputStream(),
                ezParser: emptyParser
        ).next()

        then: "an AssertionError is thrown"
        record.throwable instanceof AssertionError
    }
}
