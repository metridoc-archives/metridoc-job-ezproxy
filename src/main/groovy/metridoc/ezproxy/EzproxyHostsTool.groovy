package metridoc.ezproxy

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
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
    public static final String FILE_FILTER_IS_NULL = "ezproxy file filter cannot be null"
    public static final String EZ_DIRECTORY_IS_NULL = 'ezproxy directory or camelUrl must not be null'
    public static final String DEFAULT_FILE_FILTER = "ezproxy*"
    public static final Closure<String> EZ_DIRECTORY_DOES_NOT_EXISTS = { "ezproxy directory ${it} does not exist" as String }
    public static final Closure<String> EZ_FILE_DOES_NOT_EXIST = { "ezproxy file $it does not exist" as String }

    @InjectArg(config = "ezproxy.fileFilter")
    String fileFilter = DEFAULT_FILE_FILTER
    @InjectArg(config = "ezproxy.directory")
    File directory
    @InjectArg(config = "ezproxy.file")
    File file
    @InjectArg(ignore = true)
    IteratorWriter writer = new EntityIteratorWriter(recordEntityClass: EzproxyHosts)
    @InjectArg(ignore = true)
    WriteResponse writerResponse
    @InjectArg(ignore = true)
    List<Class> entityClasses = [EzproxyHosts]
    @InjectArg(config = "ezproxy.camelUrl")
    String camelUrl
    @InjectArg(config = "ezproxy.preview")
    boolean preview

    @Override
    def configure() {
        def hibernateTool
        if (!preview) {
            hibernateTool = includeTool(HibernateTool, entityClasses: entityClasses)
        }

        validateInputs()
        target(processEzproxyFile: "default target for processing ezproxy file") {
            def camelTool = includeTool(CamelTool)
            processFile {
                def inputStream = file.newInputStream()
                def fileName = file.name
                if(fileName.endsWith(".gz")) {
                    inputStream = new GZIPInputStream(inputStream)
                }

                def ezIterator = includeTool(EzproxyIterator, inputStream: inputStream, file: file)

                if(preview) {
                    ezIterator.preview()
                    return
                }

                if (writer instanceof EntityIteratorWriter) {
                    writer.sessionFactory = hibernateTool.sessionFactory
                }
                writerResponse = writer.write(ezIterator)
                if(writerResponse.fatalErrors) {
                    throw writerResponse.fatalErrors[0]
                }
            }
            camelTool.close()
        }

        setDefaultTarget("processEzproxyFile")
    }

    protected void validateInputs() {
        println this
        if (!file) {
            assert fileFilter: FILE_FILTER_IS_NULL
            assert directory || camelUrl: EZ_DIRECTORY_IS_NULL
            if (directory) {
                assert directory.exists(): EZ_DIRECTORY_DOES_NOT_EXISTS(directory)
            }
        } else {
            assert file.exists(): EZ_FILE_DOES_NOT_EXIST(file)
        }
    }

    boolean acceptFile(String fileName) {
        if(preview) return true
        def result
        def hibernateTool = getVariable("hibernateTool", HibernateTool)
        hibernateTool.withTransaction {Session session ->
            Query query = session.createQuery("from EzproxyHosts where fileName = :fileName")
                    .setParameter("fileName", fileName)
            result = query.list()
        }

        return result.size() ==  0
    }

    protected processFile(Closure closure) {
        if(file) {
            assert file.exists() : "$file does not exist"
            directory = new File(file.parent)
        }

        long readLockTimeout = 1000 * 60 * 60 * 24 //one day
        String fileUrl
        if (directory) {
            fileUrl = "${directory.toURI().toURL()}?noop=true&readLockTimeout=${readLockTimeout}&antInclude=${fileFilter}&sendEmptyMessageWhenIdle=true&filter=#ezproxyFileFilter"
        }
        def camelTool = includeTool(CamelTool)
        def doesNotHaveFilter = !camelTool.camelContext.registry.lookupByName("ezproxyFileFilter")
        if (doesNotHaveFilter) {
            camelTool.bind("ezproxyFileFilter",
                    [
                            accept: { GenericFile file ->
                                try {
                                    if(this.file) {
                                        return file.fileNameOnly == this.file.name
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

        def usedUrl = camelUrl ?: fileUrl
        //this creates a file transaction
        camelTool.consume(usedUrl) { File file ->
            this.file = file
            if (this.file) {
                log.info "processing file $file"
                closure.call(this.file)
            }
        }
    }
}


