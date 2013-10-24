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
