package metridoc.ezproxy.services

import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
import metridoc.core.InjectArgBase
import metridoc.service.gorm.GormService
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter

/**
 * @author Tommy Barker
 */
@Slf4j
@InjectArgBase("ezproxy")
class EzproxyFileFilterService implements GenericFileFilter {

    File file
    boolean preview
    boolean stacktrace
    @InjectArg(ignore = true)
    Class entityClass

    GormService gormService

    @Override
    boolean accept(GenericFile file) {
        try {
            if(this.file) {
                return file.fileNameOnly == this.file.name
            }

            if(preview) return true

            assert entityClass && gormService : "entityClass and gormService must not be null"

            List result
            entityClass.withTransaction {
                result = entityClass.findAllByFileName(file.fileName)
            }

            return result ==  null || result.size() == 0
        }
        catch (Throwable throwable) {
            if(stacktrace) {
                throwable.printStackTrace()
            } else {
                log.error throwable.message
            }
            return false
        }
    }
}
