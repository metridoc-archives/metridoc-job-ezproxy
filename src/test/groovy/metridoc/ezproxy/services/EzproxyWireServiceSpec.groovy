package metridoc.ezproxy.services

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 9/25/13
 * @author Tommy Barker
 */
class EzproxyWireServiceSpec extends Specification {

    void "test wiring together services"() {
        given:
        def wireService = new EzproxyWireService(preview: true)

        when:
        def ezproxyService = wireService.wireupNonConfigServices(null)

        then:
        noExceptionThrown()
        ezproxyService.ezproxyIngestService
    }
}
