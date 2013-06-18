package metridoc.ezproxy

import groovy.util.logging.Slf4j
import metridoc.iterators.FileIterator
import org.apache.commons.io.LineIterator

/**
 * Created with IntelliJ IDEA on 6/12/13
 * @author Tommy Barker
 *
 */
@Slf4j
class EzproxyIterator extends FileIterator {

    Closure ezParser
    String ezEncoding = "utf-8"
    String fileName
    //one based
    int currentRow = 0
    int assertionErrors = 0

    @Lazy(soft = true)
    Reader reader = { new InputStreamReader(inputStream, ezEncoding) }()

    @Lazy
    LineIterator lineIterator = { new LineIterator(getReader()) }()

    @Override
    protected Map computeNext() {
        currentRow++
        if (lineIterator.hasNext()) {
            def next = lineIterator.next()
            Map result
            try {
                result = ezParser.call(next) as Map
                assert result : "the result should not be empty or null"
            }
            catch (AssertionError error) {
                assertionErrors++
                log.warn("there was an assertion error at line $currentRow for file $fileName: $error.message")
                return computeNext()
            }
            result.fileName = fileName
            result.lineNumber = currentRow
            return result
        } else {
            endOfData()
        }
    }
}
