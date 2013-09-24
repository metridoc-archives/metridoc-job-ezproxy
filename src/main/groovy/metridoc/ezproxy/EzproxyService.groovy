package metridoc.ezproxy

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
import metridoc.core.InjectArgBase
import metridoc.core.services.CamelService
import metridoc.core.services.HibernateService
import metridoc.core.services.RunnableService
import metridoc.writers.EntityIteratorWriter
import metridoc.writers.IteratorWriter
import metridoc.writers.WriteResponse

import java.util.zip.GZIPInputStream

/**
 * Created with IntelliJ IDEA on 6/13/13
 * @author Tommy Barker
 */
@SuppressWarnings("GrMethodMayBeStatic")
@Slf4j
@ToString(includePackage = false, includeNames = true)
@InjectArgBase("ezproxy")
class EzproxyService extends RunnableService {
    public static final String FILE_FILTER_IS_NULL = "ezproxy file filter cannot be null"
    public static final String EZ_DIRECTORY_IS_NULL = 'ezproxy directory or camelUrl must not be null'
    public static final String DEFAULT_FILE_FILTER = "ezproxy*"
    public static final Closure<String> EZ_DIRECTORY_DOES_NOT_EXISTS = { "ezproxy directory ${it} does not exist" as String }
    public static final Closure<String> EZ_FILE_DOES_NOT_EXIST = { "ezproxy file $it does not exist" as String }

    String fileFilter = DEFAULT_FILE_FILTER
    File directory
    File file
    @InjectArg(ignore = true)
    IteratorWriter writer
    @InjectArg(ignore = true)
    WriteResponse writerResponse
    @InjectArg(ignore = true)
    def entityClass
    String camelUrl
    boolean preview
    HibernateService hibernateService

    @Override
    def configure() {
        target(validateInputs: "validates inputs to the job") {
            validateInputs()
        }

        target(processEzproxyFile: "default target for processing ezproxy file", depends:"validateInputs") {
            processEzproxyFile()
        }

        setDefaultTarget("processEzproxyFile")
    }

    EzproxyIteratorService createIterator() {

    }

    private void preview() {

    }

    private void processEzproxyFile() {
        def camelService = includeService(CamelService)
        processFile {
            def inputStream = file.newInputStream()
            def fileName = file.name
            if (fileName.endsWith(".gz")) {
                inputStream = new GZIPInputStream(inputStream)
            }

            def ezIterator = includeService(EzproxyIteratorService, inputStream: inputStream, file: file)

            if (preview) {
                ezIterator.preview()
                return
            }
            if (!writer) {
                writer = new EntityIteratorWriter(recordEntityClass: entityClass)
            }
            if (writer instanceof EntityIteratorWriter) {
                writer.sessionFactory = hibernateTool.sessionFactory
            }
            writerResponse = writer.write(ezIterator)
            if (writerResponse.fatalErrors) {
                throw writerResponse.fatalErrors[0]
            }
        }
        camelService.close()
    }

    protected void validateInputs() {
        assert entityClass : "entityClass cannot be null"
        if (!preview) {
            log.info "booting up hibernate with entity $entityClass"
            hibernateService = includeService(HibernateService, entityClasses: [getEntityClass()])
        }

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
        def camelService = includeService(CamelService)
        def doesNotHaveFilter = !camelService.camelContext.registry.lookupByName("ezproxyFileFilter")
        if (doesNotHaveFilter) {
            def fileFilter = includeService(EzproxyFileFilter, entityClass: entityClass, preview: preview, file: file)
            camelService.bind("ezproxyFileFilter", fileFilter)
        }

        def usedUrl = camelUrl ?: fileUrl
        //this creates a file transaction
        camelService.consume(usedUrl) { File file ->
            this.file = file
            if (this.file) {
                log.info "processing file $file"
                closure.call(this.file)
            }
        }
    }
}


