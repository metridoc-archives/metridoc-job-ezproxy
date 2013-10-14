package metridoc.ezproxy.services

import metridoc.core.services.CamelService
import metridoc.core.services.ConfigService
import metridoc.core.services.DefaultService
import metridoc.core.services.ParseArgsService
import metridoc.service.gorm.GormService

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
        includeService(ParseArgsService)
        boolean mergeConfig = binding.argsMap.mergeMetridocConfig ? Boolean.valueOf(binding.argsMap.mergeMetridocConfig) : true

        includeService(ConfigService, mergeMetridocConfig:mergeConfig)

        wireupNonConfigServices(ezproxyIngestClass)
    }

    protected EzproxyService wireupNonConfigServices(Class ezproxyIngestClass) {
        if (!preview) {
            includeService(GormService, entityClasses: [ezproxyIngestClass])
        }

        def camelService = includeService(CamelService)
        def ezproxyFileFilter = includeService(EzproxyFileFilterService, entityClass: ezproxyIngestClass)
        camelService.bind("ezproxyFileFilter", ezproxyFileFilter)

        includeService(EzproxyIngestService)
        includeService(EzproxyService, entityClass: ezproxyIngestClass)
    }


}
