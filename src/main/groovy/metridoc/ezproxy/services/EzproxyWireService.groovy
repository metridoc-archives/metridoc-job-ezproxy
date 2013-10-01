package metridoc.ezproxy.services

import metridoc.core.services.CamelService
import metridoc.core.services.ConfigService
import metridoc.core.services.DefaultService
import metridoc.core.services.HibernateService

/**
 * Created with IntelliJ IDEA on 9/24/13
 * @author Tommy Barker
 */
class EzproxyWireService extends DefaultService {

    boolean preview

    EzproxyService wireupServices() {
        preview = true
        wireupServices(null)
    }

    EzproxyService wireupServices(Class ezproxyIngestClass) {
        includeService(ConfigService)
        if (!preview) {
            includeService(HibernateService, entityClasses: [ezproxyIngestClass])
        }

        def camelService = includeService(CamelService)
        def ezproxyFileFilter = includeService(EzproxyFileFilterService, entityClass: ezproxyIngestClass)
        camelService.bind("ezproxyFileFilter", ezproxyFileFilter)

        includeService(EzproxyIngestService)
        includeService(EzproxyService, entityClass: ezproxyIngestClass)
    }
}
