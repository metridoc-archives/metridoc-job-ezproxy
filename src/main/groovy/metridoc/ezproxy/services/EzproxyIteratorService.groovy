/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.ezproxy.services

import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
import metridoc.core.InjectArgBase
import metridoc.iterators.FileIterator
import metridoc.iterators.Record
import metridoc.utils.ApacheLogParser
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.LineIterator

/**
 * Created with IntelliJ IDEA on 6/12/13
 * @author Tommy Barker
 *
 */
@Slf4j
@InjectArgBase("ezproxy")
class EzproxyIteratorService extends FileIterator {
    public static final transient APACHE_NULL = "-"
    boolean encryptPatronId = false
    boolean encryptIpAddress = false
    int patronId = -1
    int country = -1
    int ipAddress = -1
    int state = -1
    int city = -1
    int rank = -1
    int department = -1
    int ezproxyId = -1
    int url = -1
    int proxyDate = -1
    String apacheNull = APACHE_NULL
    String delimiter
    int maxLines = 0

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

        def body = record.body
        if(encryptIpAddress && body.ipAddress) {
            body.ipAddress = DigestUtils.sha256Hex(body.ipAddress as String)
        }

        if(encryptPatronId && body.patronId) {
            body.patronId = DigestUtils.sha256Hex(body.patronId as String)
        }
        return record
    }
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

    void preview() {
        def maxLinesUsed = maxLines ?: 10
        (0..maxLinesUsed).each {
            if(this.hasNext()) {
                def next = this.next()
                println ""
                println "Record {"

                println ""
                println "    originalLine -> $next.body.originalLine"
                println ""

                next.body.each {key, value ->
                    if(key != "originalLine") {
                        Integer position
                        try {
                            position = this."$key"
                        }
                        catch (Throwable ignored) {
                            //ignore
                        }
                        if(position > -1) {
                            println "    $key (pos $position) -> $value"
                        }
                        else {
                            println "    $key -> $value"
                        }
                    }
                }
                println "}"
                println ""
            }
        }
    }
}
