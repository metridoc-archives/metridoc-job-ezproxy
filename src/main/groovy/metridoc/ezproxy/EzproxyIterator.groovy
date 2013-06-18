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
            if (ezParser instanceof Closure) {
                result = ezParser.call(next) as Map
            }
            else {
                result = ezParser.parse(next) as Map
            }
            result.fileName = fileName
            result.lineNumber = currentRow
            return result
        }
        else {
            endOfData()
        }
    }
}
