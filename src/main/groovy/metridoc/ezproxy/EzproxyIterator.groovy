package metridoc.ezproxy

import groovy.util.logging.Slf4j
import metridoc.iterators.FileIterator
import metridoc.iterators.Record
import metridoc.utils.ApacheLogParser
import org.apache.commons.io.LineIterator

/**
 * Created with IntelliJ IDEA on 6/12/13
 * @author Tommy Barker
 *
 */
@Slf4j
class EzproxyIterator extends FileIterator {
    public static final transient APACHE_NULL = "-"

    Closure ezParser
    String ezEncoding = "utf-8"
    //so we can get the line if there is a failure
    String currentLine
    //one based
    int currentRow = 0

    @Lazy(soft = true)
    Reader reader = { new InputStreamReader(inputStream, ezEncoding) }()

    @Lazy
    LineIterator lineIterator = { new LineIterator(getReader()) }()

    @SuppressWarnings("GroovyVariableNotAssigned")
    @Override
    protected Record computeNext() {
        currentRow++

        if (lineIterator.hasNext()) {
            currentLine = lineIterator.next()
            Map result
            def record = new Record()
            try {
                result = ezParser.call(currentLine) as Map
                assert result: "the result should not be empty or null"
                convertApacheNullToNull(result)
                addUrlHosts(result)
                addProxyDate(result)
            }
            catch (Throwable throwable) {
                record.throwable = throwable
            }
            result = result ?: [:]
            result.fileName = fileName
            result.lineNumber = currentRow
            result.originalLine = currentLine
            record.body = result
            return record
        }

        return endOfData()
    }

    protected addUrlHosts(Map result) {
        String url = result.url
        assert url : "url is null or empty"
        validateUrl(url)
        result.urlHost = new URL(result.url).host
    }

    protected addProxyDate(Map result) {
        def proxyDate = result.proxyDate
        assert proxyDate : "proxyDate is not in result or is null"
        if(proxyDate instanceof String) {
            result.proxyDate = ApacheLogParser.parseLogDate(proxyDate)
        }
    }

    protected void convertApacheNullToNull(Map map) {
        map.each {key, value ->
            if(value == APACHE_NULL) {
                map[key] = null
            }
        }
    }

    protected void validateUrl(String url) {
        try {
            new URL(url)
        }
        catch (MalformedURLException ex) {
            throw new AssertionError(ex)
        }
    }
}
