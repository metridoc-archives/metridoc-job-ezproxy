package metridoc.ezproxy.services

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/18/13
 * @author Tommy Barker
 */
class CrossRefServiceSpec extends Specification {

    void "test exceptions"() {
        when:
        CrossRefServiceHelper helper = new CrossRefServiceHelper()
        def response = helper.getResponse("foo", new URL("http://foobar.com"))

        then:
        response.statusException
        helper.disconnectCalled
    }
}

class CrossRefServiceHelper extends CrossRefService {
    boolean disconnectCalled

    @Override
    protected HttpURLConnection getConnection(URL url) {
        return new HttpURLConnection(url){
            @Override
            void disconnect() {
                disconnectCalled = true
            }

            @Override
            boolean usingProxy() {
                return false
            }

            @Override
            void connect() throws IOException {
                //do nothing
            }

            @Override
            int getResponseCode() throws IOException {
                400
            }
        }
    }
}

