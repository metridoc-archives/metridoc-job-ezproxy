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



package metridoc.ezproxy.entities

import metridoc.iterators.Record
import metridoc.service.gorm.GormService
import metridoc.tool.gorm.MetridocRecordGorm
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
        def service = new GormService(embeddedDataSource: true)
        service.init()
        service.enableFor(EzDoi)
        EzDoi.withTransaction {
            def gormRecord = new MetridocRecordGorm(entityInstance: new EzDoi())
            gormRecord.populate (record)
        }

        then:
        def error = thrown(AssertionError)
        error.message.contains("error on field [doi] with error code [nullable]")
    }

    void "test alreadyExists"() {
        given:
        def gormService = new GormService(embeddedDataSource: true)
        gormService.init()
        gormService.enableFor(EzDoi)
        EzDoi.withTransaction {
            new EzDoi(
                    doi:"foo",
                    ezproxyId: "bar",
                    fileName: "foobar",
                    lineNumber: 1,
                    proxyDate: new Date(),
                    proxyDay: 1,
                    proxyMonth: 1,
                    proxyYear: 2012,
                    urlHost: "http://foo.com"
            ).save(failOnError: true)
        }

        when:
        boolean exists = new EzDoi(
                doi: "foo",
                ezproxyId: "bar"
        ).alreadyExists()

        then:
        noExceptionThrown()
        exists
    }
}
