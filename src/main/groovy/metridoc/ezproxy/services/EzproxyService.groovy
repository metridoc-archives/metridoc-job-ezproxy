package metridoc.ezproxy.services

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
import metridoc.core.InjectArgBase
import metridoc.core.services.HibernateService
import metridoc.core.services.RunnableService
import metridoc.writers.IteratorWriter
import metridoc.writers.WriteResponse

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

    @Override
    def configure() {
        step(validateInputs: "validates inputs to the job")
        step(preview: "previews data", depends: "validateInputs")
        step(processEzproxyFile: "default target for processing ezproxy file", depends:"validateInputs")

        if(preview) {
            setDefaultTarget("preview")
        }
        else {
            setDefaultTarget("processEzproxyFile")
        }
    }

    private void preview() {
        processEzproxyFile()
    }

    private void processEzproxyFile() {
        includeService(EzproxyIngestService).ingestData()
    }

    protected void validateInputs() {
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
}


