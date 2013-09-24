package metridoc.ezproxy.services

import groovy.util.logging.Slf4j
import metridoc.core.InjectArgBase
import metridoc.core.services.CamelService
import metridoc.core.services.DefaultService
import metridoc.core.services.HibernateService
import metridoc.writers.EntityIteratorWriter

import java.util.zip.GZIPInputStream

/**
 * Created with IntelliJ IDEA on 9/24/13
 * @author Tommy Barker
 */
@InjectArgBase("ezproxy")
@Slf4j
class EzproxyIngestService extends DefaultService {

    EzproxyService ezproxyService

    void ingestData() {
        def camelService = createCamelService()

        processFile {

            EzproxyIteratorService ezIterator = getEzproxyIterator()

            ezproxyService.with {
                if (preview) {
                    ezIterator.preview()
                    return
                }
                if (!writer) {
                    writer = new EntityIteratorWriter(recordEntityClass: entityClass)
                }

                if (writer instanceof EntityIteratorWriter) {
                    def hibernateService = includeService(HibernateService, entityClasses: [entityClass])
                    writer.sessionFactory = hibernateService.sessionFactory
                }

                writerResponse = writer.write(ezIterator)
                if (writerResponse.fatalErrors) {
                    throw writerResponse.fatalErrors[0]
                }
            }
        }
        camelService.close()
    }

    protected EzproxyIteratorService getEzproxyIterator() {
        def ezproxyIteratorService = getVariable("ezproxyIteratorService", EzproxyIteratorService)
        if (ezproxyIteratorService) return ezproxyIteratorService

        ezproxyService.with {
            def inputStream = file.newInputStream()
            def fileName = file.name
            if (fileName.endsWith(".gz")) {
                inputStream = new GZIPInputStream(inputStream)
            }
            return includeService(EzproxyIteratorService, inputStream: inputStream, file: file)
        }
    }

    protected processFile(Closure closure) {
        String fileUrl = createFileUrl()
        CamelService camelService = createCamelService()

        ezproxyService.with {
            def usedUrl = camelUrl ?: fileUrl
            //this creates a file transaction
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

    protected CamelService createCamelService() {
        def camelService = includeService(CamelService)
        def doesNotHaveFilter = !camelService.camelContext.registry.lookupByName("ezproxyFileFilter")
        if (doesNotHaveFilter) {
            ezproxyService.with {
                def fileFilter = includeService(EzproxyFileFilterService, entityClass: entityClass, preview: preview, file: file)
                camelService.bind("ezproxyFileFilter", fileFilter)
            }
        }
        camelService
    }
}
