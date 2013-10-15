package metridoc.ezproxy.services

import metridoc.ezproxy.entities.EzHosts
import metridoc.service.gorm.GormService
import spock.lang.Specification

/**
 * @author Tommy Barker
 */
class EzproxyIngestServiceSpec extends Specification {

    void "if the gormService has already been setup, setup writer should not fail"() {
        given:
        GormService gormService = new GormService(embeddedDataSource: true)
        gormService.init()
        gormService.enableGormFor(EzHosts)
        EzproxyService ezService = new EzproxyService(entityClass: EzHosts)
        def ingestService = new EzproxyIngestService(ezproxyService: ezService)
        Binding binding = ingestService.binding
        binding.gormService = gormService

        when:
        ingestService.setupWriter()

        then:
        noExceptionThrown()
    }
}
