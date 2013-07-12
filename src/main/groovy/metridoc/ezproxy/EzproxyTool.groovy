package metridoc.ezproxy

import groovy.util.logging.Slf4j
import metridoc.core.tools.CamelTool
import metridoc.core.tools.RunnableTool
import metridoc.utils.ApacheLogParser
import metridoc.writers.IteratorWriter
import metridoc.writers.TableIteratorWriter
import metridoc.writers.WriteResponse
import metridoc.writers.WrittenRecordStat
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter

import java.util.regex.Pattern
import java.util.zip.GZIPInputStream

/**
 * Created with IntelliJ IDEA on 6/13/13
 * @author Tommy Barker
 */
@SuppressWarnings("GrMethodMayBeStatic")
@Slf4j
class EzproxyTool extends RunnableTool {
    public static final String EZPROXY_PARSER_IS_NULL = "ezproxy parser cannot be null"
    public static final String FILE_FILTER_IS_NULL = "ezproxy file filter cannot be null"
    public static final String EZ_DIRECTORY_IS_NULL = 'ezproxy directory cannot be null'
    public static final String DEFAULT_FILE_FILTER = "ezproxy*"
    public static final Closure<String> EZ_DIRECTORY_DOES_NOT_EXISTS = { "ezproxy directory ${it} does not exist" as String }
    public static final Closure<String> EZ_FILE_DOES_NOT_EXIST = { "ezproxy file $it does not exist" as String }
    public static final APACHE_NULL = "-"
    public static final DOI_PREFIX_PATTERN = "10."
    public static final DOI_PROPERTY_PATTERN = "doi=10."
    public static final DOI_FULL_PATTERN = Pattern.compile(/10\.\d+\//)
    public static final Closure<Map> DEFAULT_EZ_PARSER = { String line ->
        def data = line.split(/\|\|/)
        assert data.size() >= 14: "there should be at least 14 data fields but was only ${data.size()}"
        def result = [:]
        result.ipAddress = data[0]
        result.city = data[1]
        result.state = data[2]
        result.country = data[3]
        result.patronId = data[5]
        result.proxyDate = data[6]
        result.url = data[8]
        result.ezproxyId = data[13]

        return result as Map
    }

    String ezEncoding = "utf-8"

    Closure<Map> ezParser = DEFAULT_EZ_PARSER

    Closure<Map> ezTransformer
    String ezFileFilter = DEFAULT_FILE_FILTER
    File ezDirectory
    File ezFile
    String processedExtension = "processed"
    IteratorWriter ezWriter = new TableIteratorWriter()
    int assertionErrors = 0
    int successfulRecords = 0
    WriteResponse writerResponse

    @Override
    def configure() {
        validateInputs()

        target(processEzproxyFile: "default target for processing ezproxy file") {
            def camelTool = includeTool(CamelTool)
            processFile {
                def inputStream = ezFile.newInputStream()
                if(ezFile.name.endsWith(".gz")) {
                    inputStream = new GZIPInputStream(inputStream)
                }
                def ezIterator = new EzproxyIterator(
                        inputStream: inputStream,
                        file: ezFile,
                        ezParser: ezParser,
                        ezEncoding: ezEncoding
                )

                writerResponse = ezWriter.write(ezIterator)
                int total = 0
                def stats = writerResponse.aggregateStats
                stats.values().each {
                    total += it
                }
                assertionErrors = stats[WrittenRecordStat.Status.INVALID]
                successfulRecords = stats[WrittenRecordStat.Status.WRITTEN]
                if(assertionErrors) {
                    log.warn "while iterating over ${total} records there were ${stats[WrittenRecordStat.Status.INVALID]} errors"
                }
                camelTool.close()
            }
        }

        setDefaultTarget("processEzproxyFile")
    }

    protected void convertApacheNullToNull(Map map) {
        map.each {key, value ->
            if(value == APACHE_NULL) {
                map[key] = null
            }
        }
    }

    protected void addDateValues(Map record) {
        def proxyDate = record.proxyDate
        if (notNull(proxyDate)) {
            if (proxyDate instanceof String) {
                try {
                    record.proxyDate = ApacheLogParser.parseLogDate(proxyDate)
                }
                catch (Throwable e) {
                    //since during parsing assertion errors are considered 'invalid'
                    throw new AssertionError("$proxyDate is not a parsable date", e)
                }
            }

            def calendar = new GregorianCalendar()
            calendar.setTime(record.proxyDate as Date)
            record.proxyYear = calendar.get(Calendar.YEAR)
            record.proxyMonth = calendar.get(Calendar.MONTH)
            record.proxyDay = calendar.get(Calendar.DAY_OF_MONTH)
        }
    }

    protected void addDoi(Map record) {
        if(hasDoi(record)) {
            record.doi = extractDoi(record.url)
        }
    }

    protected void addHosts(Map record) {
        def url = record.url as String
        if (notNull(url)) {
            validateUrl(url)
            record.urlHost = new URL(url).getHost()
        }
    }

    protected boolean notNull(item) {
        if(item == null) {
            return false
        }

        if(item instanceof String) {
            item.trim() && item.trim() != APACHE_NULL
        }

        return true
    }

    protected void validateUrl(String url) {
        try {
            new URL(url).toURI()
        }
        catch (URISyntaxException | MalformedURLException ex) {
            throw new AssertionError(ex)
        }
    }

    protected void validateInputs() {
        assert ezParser: EZPROXY_PARSER_IS_NULL

        if (!ezFile) {
            assert ezFileFilter: FILE_FILTER_IS_NULL
            assert ezDirectory: EZ_DIRECTORY_IS_NULL
            assert ezDirectory.exists(): EZ_DIRECTORY_DOES_NOT_EXISTS(ezDirectory)
        } else {
            assert ezFile.exists(): EZ_FILE_DOES_NOT_EXIST(ezFile)
        }
    }

    boolean acceptFile(File file) {
        def fileName = file.name

        if (fileName.endsWith(".$processedExtension")) {
            return false
        }

        def processedFileName = "${fileName}.$processedExtension"
        def processedFile = new File(file.parent, processedFileName)

        if (processedFile.exists()) {
            return false
        }

        return true
    }

    protected File finishProcessingFile(File file) {
        def fileName = file.name
        def result = new File(file.parent, "${fileName}.$processedExtension")
        boolean created = result.createNewFile()
        if (!created) {
            log.warn "Could not create processed file $result, it was probably already there"
        }

        return result
    }

    protected boolean processFile(Closure closure) {
        if(ezFile) {
            ezDirectory = new File(ezFile.parent)
        }

        long readLockTimeout = 1000 * 60 * 60 //one hour
        String fileUrl = "${ezDirectory.toURI().toURL()}?noop=true&readLockTimeout=${readLockTimeout}&antInclude=${ezFileFilter}&sendEmptyMessageWhenIdle=true&filter=#ezproxyFileFilter"
        def camelTool = includeTool(CamelTool)
        def doesNotHaveFilter = !camelTool.camelContext.registry.lookupByName("ezproxyFileFilter")
        if (doesNotHaveFilter) {
            camelTool.bind("ezproxyFileFilter",
                    [
                            accept: { GenericFile file ->
                                if(ezFile) {
                                    return file.fileNameOnly == ezFile.name
                                }
                                acceptFile(file.file as File)
                            }
                    ] as GenericFileFilter
            )
        }

        //this creates a file transaction
        camelTool.consume(fileUrl) { File file ->
            ezFile = file
            if (ezFile) {
                closure.call(ezFile)
                finishProcessingFile(file)
            }
        }
        return ezFile
    }

    protected String extractDoi(String url) {
        String result = null
        int idxBegin = url.indexOf(DOI_PROPERTY_PATTERN)
        boolean
        if (idxBegin > -1) {
            String doiBegin = url.substring(idxBegin + 4)
            int idxEnd = doiBegin.indexOf('&') > 0 ? doiBegin.indexOf('&') : doiBegin.size()
            result = URLDecoder.decode(URLDecoder.decode(doiBegin.substring(0, idxEnd), "utf-8"), "utf-8") //double encoding
        } else {
            idxBegin = url.indexOf(DOI_PREFIX_PATTERN)
            if (idxBegin > -1) {
                String doiBegin = url.substring(idxBegin)
                //find index of 2nd slash
                int slashInd = doiBegin.indexOf("/");
                slashInd = slashInd > -1 ? doiBegin.indexOf("/", slashInd + 1) : -1;
                int idxEnd = doiBegin.indexOf('?')
                if (idxEnd == -1) {
                    //case where doi is buried in embedded url
                    doiBegin = URLDecoder.decode(doiBegin, "utf-8")
                    idxEnd = doiBegin.indexOf('&')
                    slashInd = slashInd > -1 ? doiBegin.indexOf("/", slashInd + 1) : -1; // compute again in case of encoding
                }
                if (idxEnd > -1) {
                    if (slashInd > -1) {
                        idxEnd = [slashInd, idxEnd].min()
                    }
                } else if (slashInd > -1) {
                    idxEnd = slashInd
                } else {
                    idxEnd = doiBegin.size()
                }
                result = doiBegin.substring(0, idxEnd)
            }
        }

        if (result && result.contains("/")) {
            int startIndex = result.indexOf("/")
            String suffix = result.substring(startIndex + 1, result.length())
            int nextSlash = suffix.indexOf("/")
            if (nextSlash > -1) {
                result = result.substring(0, startIndex + nextSlash + 1)
            }
        } else {
            result = null //must be garbage
        }
        return result
    }

    protected boolean hasDoi(Map record) {
        String url = record.url
        int indexOfDoiPrefix = url.indexOf(DOI_PREFIX_PATTERN)
        if (indexOfDoiPrefix > -1) {
            def doiAtStart = url.substring(indexOfDoiPrefix)
            //noinspection GroovyUnusedCatchParameter
            try {
                doiAtStart = URLDecoder.decode(doiAtStart, "utf-8")
            } catch (IllegalArgumentException ex) {

            }
            def doiMatcher = DOI_FULL_PATTERN.matcher(doiAtStart)
            return doiMatcher.lookingAt()
        }

        return false
    }
}


