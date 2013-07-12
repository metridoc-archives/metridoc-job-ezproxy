package metridoc.ezproxy

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
class EzproxyHostsSpec extends Specification {

    def "test basic validation"() {
        when: "validate empty payload"
        new EzproxyHosts().validate()

        then: "fileName cannot be null"
        def error = thrown(AssertionError)
        error.message.contains("fileName")

        when: "fileName is there"
        new EzproxyHosts(fileName: "foo").validate()

        then: "urlHost cannot be null"
        error = thrown(AssertionError)
        error.message.contains("urlHost")
    }

    def "test truncation"() {
        given: "EzproxyHost with some data"
    }
}
