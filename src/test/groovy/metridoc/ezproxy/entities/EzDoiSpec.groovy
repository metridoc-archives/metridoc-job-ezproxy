package metridoc.ezproxy.entities

import metridoc.iterators.Record
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/8/13
 * @author Tommy Barker
 */
class EzDoiSpec extends Specification {

    void "when extractDoi throws error, populate will fail with assertion error saying doi is null"() {
        given: "a bad url"
        def badUrl = "http://foo?doi=10.%2"

        when: "extract doi is called"
        def doi = new EzDoi()
        doi.extractDoi(badUrl)

        then:
        thrown(Throwable)

        when: "populate is called"
        def record = new Record(
                body: [
                        url: badUrl,
                        ezproxyId: "asdasdf",
                        fileName: "kjahsdfkjahsdf",
                        urlHost: "foo"
                ]
        )
        doi.populate(record)

        then:
        def error = thrown(AssertionError)
        error.message.contains("doi cannot be null or empty")
    }

}
