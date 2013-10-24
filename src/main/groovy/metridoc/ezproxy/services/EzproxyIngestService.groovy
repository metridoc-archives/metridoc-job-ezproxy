/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.  */

package metridoc.ezproxy.services

import groovy.util.logging.Slf4j
import metridoc.core.InjectArgBase
import metridoc.core.services.CamelService
import metridoc.core.services.DataSourceService
import metridoc.core.services.DefaultService
import metridoc.service.gorm.GormService
import metridoc.tool.gorm.GormIteratorWriter
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
    long waitForFile = 1000 * 60 * 3 //3 minutes
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
                writer = new GormIteratorWriter(gormClass: entityClass)
            }

            if (writer instanceof GormIteratorWriter && !preview) {
                DataSourceService gormService = includeService(GormService)

                try {
                    gormService.enableFor(entityClass)
                }
                catch (IllegalStateException ignored) {
                    //do nothing, already enabled
                }

                entityClass.withNewSession { Session session ->
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

    protected void processFile(Closure closure) {
        String fileUrl = createFileUrl()

        ezproxyService.with {
            def usedUrl = camelUrl ?: fileUrl
            //this creates a file transaction
            def sanitizedUrl = URISupport.sanitizeUri(usedUrl)
            log.info "consuming from [${sanitizedUrl}]"
            boolean atLeastOneFileProcessed = false
            camelService.consumeWait(usedUrl, waitForFile) { File file ->
                ezproxyService.file = file
                if (ezproxyService.file) {
                    atLeastOneFileProcessed = true
                    log.info "processing file $file"
                    closure.call(ezproxyService.file)
                }
            }

            if(!atLeastOneFileProcessed) {
                log.info "no files were processed, if this is unexpected, consider extending the wait time to retrieve the file\n" +
                        "  command line: use --waitForFile=<milliseconds>"
                        "  config file: use ezproxy.waitForFile=<milliseconds>"
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
