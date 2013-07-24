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

        then: "lineNumber cannot be null"
        def error = thrown(AssertionError)
        error.message.contains("lineNumber")

        when: "lineNumber is there"
        new EzproxyHosts(lineNumber: 1).validate()

        then: "fileName cannot be null"
        error = thrown(AssertionError)
        error.message.contains("fileName")
    }

    def "test truncation"() {
        given: "EzproxyHost with some data"
    }
}
