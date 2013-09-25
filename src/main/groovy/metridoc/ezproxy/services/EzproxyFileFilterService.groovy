package metridoc.ezproxy.services

import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
import metridoc.core.InjectArgBase
import metridoc.core.services.HibernateService
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter
import org.hibernate.Query
import org.hibernate.Session

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

    HibernateService hibernateService

    @Override
    boolean accept(GenericFile file) {
        try {
            if(this.file) {
                return file.fileNameOnly == this.file.name
            }

            if(preview) return true

            assert entityClass && hibernateService : "entityClass and hibernateService must not be null"

            def result
            hibernateService.withTransaction {Session session ->
                Query query = session.createQuery("from ${entityClass.simpleName} where fileName = :fileName")
                        .setParameter("fileName", file.fileNameOnly)
                result = query.list()
            }

            return result.size() ==  0
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
