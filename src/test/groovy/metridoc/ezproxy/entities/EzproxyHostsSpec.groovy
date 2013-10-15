package metridoc.ezproxy.entities

import metridoc.service.gorm.GormService
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
class EzproxyHostsSpec extends Specification {

    GormService service

    def setup() {
        service = new GormService(embeddedDataSource: true)
        service.init()
        service.enableFor(EzHosts)
    }

    def "test basic validation"() {
        when: "validate empty payload"
        EzHosts hosts
        boolean valid
        EzHosts.withTransaction {
            hosts = new EzHosts()
            valid = hosts.validate()
        }

        then: "lineNumber cannot be null"
        !valid
        "nullable" == hosts.errors.getFieldError("lineNumber").code

        when: "lineNumber is there"
        EzHosts.withTransaction {
            hosts = new EzHosts(lineNumber: 1)
            valid = hosts.validate()
        }

        then: "fileName cannot be null"
        !valid
        "nullable" == hosts.errors.getFieldError("fileName").code
    }
}
