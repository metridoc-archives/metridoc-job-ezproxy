/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



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
