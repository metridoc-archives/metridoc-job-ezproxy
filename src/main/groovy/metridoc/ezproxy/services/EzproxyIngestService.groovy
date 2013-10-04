package metridoc.ezproxy.services

import groovy.util.logging.Slf4j
import metridoc.core.InjectArgBase
import metridoc.core.services.CamelService
import metridoc.core.services.DefaultService
import metridoc.core.services.HibernateService
import metridoc.writers.EntityIteratorWriter
import org.apache.camel.util.URISupport
import org.hibernate.Session

import java.util.zip.GZIPInputStream

/**
 * Created with IntelliJ IDEA on 9/24/13
 * @author Tommy Barker
 */
@InjectArgBase("ezproxy")
@Slf4j
class EzproxyIngestService extends DefaultService {

    EzproxyService ezproxyService
    CamelService camelService
    boolean preview

    void ingestData() {

        setupWriter()

        processFile {
            ezproxyService.with {
                ezproxyIterator.validateInputs()
                if (preview) {
                    ezproxyIterator.preview()
                    return
                }

                writerResponse = writer.write(ezproxyIterator)
                if (writerResponse.fatalErrors) {
                    throw writerResponse.fatalErrors[0]
                }
            }
        }
        camelService.close()
    }

    protected void setupWriter() {
        ezproxyService.with {
            if (!writer) {
                writer = new EntityIteratorWriter(recordEntityClass: entityClass)
            }

            if (writer instanceof EntityIteratorWriter && !preview) {
                def hibernateService = includeService(HibernateService)
                hibernateService.enableFor(entityClass)
                writer.sessionFactory = hibernateService.sessionFactory
                hibernateService.withTransaction { Session session ->
                    def url = session.connection().metaData.getURL()
                    log.info "connecting to ${url}"
                }
            }

        }
    }

    protected EzproxyIteratorService getEzproxyIterator() {
        def ezproxyIteratorService = getVariable("ezproxyIteratorService", EzproxyIteratorService)
        if (ezproxyIteratorService) return ezproxyIteratorService

        def inputStream = ezproxyService.file.newInputStream()
        def fileName = ezproxyService.file.name
        if (fileName.endsWith(".gz")) {
            inputStream = new GZIPInputStream(inputStream)
        }
        def service = includeService(EzproxyIteratorService, inputStream: inputStream, file: ezproxyService.file)
        return service
    }

    protected processFile(Closure closure) {
        String fileUrl = createFileUrl()

        ezproxyService.with {
            def usedUrl = camelUrl ?: fileUrl
            //this creates a file transaction
            log.info "consuming from [${URISupport.sanitizeUri(usedUrl)}]"
            camelService.consume(usedUrl) { File file ->
                ezproxyService.file = file
                if (ezproxyService.file) {
                    log.info "processing file $file"
                    closure.call(ezproxyService.file)
                }
            }
        }
    }

    protected String createFileUrl() {
        String fileUrl
        ezproxyService.with {
            if (file) {
                assert file.exists(): "$file does not exist"
                directory = new File(file.parent)
            }

            long readLockTimeout = 1000 * 60 * 60 * 24 //one day
            if (directory) {
                fileUrl = "${directory.toURI().toURL()}?noop=true&readLockTimeout=${readLockTimeout}&antInclude=${fileFilter}&sendEmptyMessageWhenIdle=true&filter=#ezproxyFileFilter"
            }
        }
        fileUrl
    }
}
