package metridoc.ezproxy.utils

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/15/13
 * @author Tommy Barker
 */
class TruncateUtilsSpec extends Specification {

    void "if the value is null, then the value is returned"() {
        when:
        def result = TruncateUtils.truncate(null, 5)

        then:
        noExceptionThrown()
        null == result
    }
}
