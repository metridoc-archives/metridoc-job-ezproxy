/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.  */

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

