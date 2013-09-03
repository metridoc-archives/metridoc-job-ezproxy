package metridoc.ezproxy

import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
import metridoc.core.tools.Tool
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
class EzproxyIterator extends FileIterator implements Tool {
    public static final transient APACHE_NULL = "-"
    @InjectArg(config = "ezproxy.patronId")
    int patronId = -1
    @InjectArg(config = "ezproxy.country")
    int country = -1
    @InjectArg(config = "ezproxy.ipAddress")
    int ipAddress = -1
    @InjectArg(config = "ezproxy.state")
    int state = -1
    @InjectArg(config = "ezproxy.city")
    int city = -1
    @InjectArg(config = "ezproxy.rank")
    int rank = -1
    @InjectArg(config = "ezproxy.department")
    int department = -1
    @InjectArg(config = "ezproxy.ezproxyId")
    int ezproxyId = -1
    @InjectArg(config = "ezproxy.url")
    int url = -1
    @InjectArg(config = "ezproxy.proxyDate")
    int proxyDate = -1
    @InjectArg(config = "ezproxy.apacheNull")
    String apacheNull = APACHE_NULL
    @InjectArg(config = "ezproxy.delimiter")
    String delimiter
    @InjectArg(config = "ezproxy.maxLines")
    int maxLines = 0

    @InjectArg(config = "ezproxy.parser")
    Closure parser = {String line ->
        String[] items = line.split(delimiter)
        def record = new Record()
        ["patronId", "country", "ipAddress", "state", "city", "rank", "department", "rank", "ezproxyId", "url", "proxyDate"].each {
            int position = this."$it"
            if (position > -1) {
                assert position < items.size() : "position $position is larger than the size ${items.size()} of the " +
                        "elements $items"
                record.body[it] = items[position]
            }
        }

        return record
    }
    @InjectArg(config = "ezproxy.encoding")
    String encoding = "utf-8"
    //so we can get the line if there is a failure
    @InjectArg(ignore = true)
    String currentLine
    //one based
    @InjectArg(ignore = true)
    int currentRow = 0

    @Lazy(soft = true)
    Reader reader = { new InputStreamReader(inputStream, encoding) }()

    @Lazy
    LineIterator lineIterator = { new LineIterator(getReader()) }()

    @SuppressWarnings("GroovyVariableNotAssigned")
    @Override
    protected Record computeNext() {
        currentRow++
        if(maxLines > 0 && currentRow > maxLines) {
            return endOfData()
        }
        validateInputs()
        if (lineIterator.hasNext()) {
            currentLine = lineIterator.next()
            Record record
            Map body
            try {
                record = parser.call(currentLine) as Record
                body = record.body
                assert record : "the parser must return a non null record"
                assert body: "the result should not be empty or null"
                convertApacheNullToNull(body)
                addUrlHosts(body)
                addProxyDate(body)
            }
            catch (Throwable throwable) {
                if(record == null) {
                    record = new Record()
                }
                record.throwable = throwable
                body = record.body
                if(body == null) {
                    body = [:]
                    record.body = body
                }
            }
            body.fileName = fileName
            body.lineNumber = currentRow
            body.originalLine = currentLine
            return record
        }

        return endOfData()
    }

    void validateInputs() {
        assert inputStream : "input stream has not been set"
        assert delimiter : "delimiter has not been set"
        assert ezproxyId > -1 : "no position for [ezproxyId] has been set"
        assert url > -1 : "no position for [url] has been set"
        assert proxyDate > -1 : "no position for [proxyDate] has been set"
    }

    protected addUrlHosts(Map result) {
        String url = result.url
        assert url : "url is null or empty"
        validateUrl(url)
        result.urlHost = new URL(result.url).host
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected addProxyDate(Map result) {
        def proxyDate = result.proxyDate
        assert proxyDate : "proxyDate is not in result or is null"
        if(proxyDate instanceof String) {
            result.proxyDate = ApacheLogParser.parseLogDate(proxyDate)
        }
    }

    protected void convertApacheNullToNull(Map map) {
        map.each {key, value ->
            if(value == apacheNull) {
                map[key] = null
            }
        }
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected void validateUrl(String url) {
        try {
            new URL(url)
        }
        catch (MalformedURLException ex) {
            throw new AssertionError(ex)
        }
    }

    @Override
    void setBinding(Binding binding) {
        //TODO: once setBinding becomes optional for tools, delete this
        //do nothing
    }

    void preview() {
        def maxLinesUsed = maxLines ?: 10
        (0..maxLinesUsed).each {
            if(this.hasNext()) {
                log.info this.next()
            }
        }
    }
}
