package metridoc.ezproxy

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import metridoc.core.tools.CamelTool
import metridoc.core.tools.HibernateTool
import metridoc.core.tools.RunnableTool
import metridoc.writers.EntityIteratorWriter
import metridoc.writers.IteratorWriter
import metridoc.writers.WriteResponse
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter
import org.hibernate.Query
import org.hibernate.Session

import java.util.zip.GZIPInputStream

/**
 * Created with IntelliJ IDEA on 6/13/13
 * @author Tommy Barker
 */
@SuppressWarnings("GrMethodMayBeStatic")
@Slf4j
@ToString(includePackage = false, includeNames = true)
class EzproxyHostsTool extends RunnableTool {
    public static final String EZPROXY_PARSER_IS_NULL = "ezproxy parser cannot be null"
    public static final String FILE_FILTER_IS_NULL = "ezproxy file filter cannot be null"
    public static final String EZ_DIRECTORY_IS_NULL = 'ezproxy directory cannot be null'
    public static final String DEFAULT_FILE_FILTER = "ezproxy*"
    public static final Closure<String> EZ_DIRECTORY_DOES_NOT_EXISTS = { "ezproxy directory ${it} does not exist" as String }
    public static final Closure<String> EZ_FILE_DOES_NOT_EXIST = { "ezproxy file $it does not exist" as String }

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
    String ezFileFilter = DEFAULT_FILE_FILTER
    File ezDirectory
    File ezFile
    IteratorWriter ezWriter = new EntityIteratorWriter(recordEntityClass: EzproxyHosts)
    WriteResponse writerResponse
    HibernateTool hibernateTool
    List<Class> entityClasses = [EzproxyHosts]
    String ezFromUrl

    @Override
    def configure() {
        hibernateTool = includeTool(HibernateTool, entityClasses: entityClasses)
        if (ezWriter instanceof EntityIteratorWriter) {
            ezWriter.sessionFactory = hibernateTool.sessionFactory
        }
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
                if(writerResponse.fatalErrors) {
                    throw writerResponse.fatalErrors[0]
                }
            }
            camelTool.close()
        }

        setDefaultTarget("processEzproxyFile")
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

    boolean acceptFile(String fileName) {
        def result

        hibernateTool.withTransaction {Session session ->
            Query query = session.createQuery("from EzproxyHosts where fileName = :fileName")
                    .setParameter("fileName", fileName)
            result = query.list()
        }

        return result.size() ==  0
    }

    protected processFile(Closure closure) {
        if(ezFile) {
            assert ezFile.exists() : "$ezFile does not exist"
            ezDirectory = new File(ezFile.parent)
        }

        long readLockTimeout = 1000 * 60 * 60 * 24 //one day
        String fileUrl = "${ezDirectory.toURI().toURL()}?noop=true&readLockTimeout=${readLockTimeout}&antInclude=${ezFileFilter}&sendEmptyMessageWhenIdle=true&filter=#ezproxyFileFilter"
        def camelTool = includeTool(CamelTool)
        def doesNotHaveFilter = !camelTool.camelContext.registry.lookupByName("ezproxyFileFilter")
        if (doesNotHaveFilter) {
            camelTool.bind("ezproxyFileFilter",
                    [
                            accept: { GenericFile file ->
                                try {
                                    if(ezFile) {
                                        return file.fileNameOnly == ezFile.name
                                    }
                                    acceptFile(file.fileName)
                                }
                                catch (Throwable throwable) {
                                    log.error "problem filtering file", throwable
                                    return false
                                }
                            }
                    ] as GenericFileFilter
            )
        }

        def usedUrl = ezFromUrl ?: fileUrl
        //this creates a file transaction
        camelTool.consume(usedUrl) { File file ->
            ezFile = file
            log.info "processing file $file"
            if (ezFile) {
                closure.call(ezFile)
            }
        }
        return ezFile
    }
}


