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

import metridoc.ezproxy.entities.EzDoi
import metridoc.ezproxy.entities.EzDoiJournal
import org.slf4j.impl.SimpleLogger
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/15/13
 * @author Tommy Barker
 */
class ResolveDoisServiceSpec extends Specification{

    void "test ingest from CrossRefObject"() {
        given:
        def response = new CrossRefResponse(
                printYear: 1,
                issue: "foo",
                onlineYear: 50
        )

        def instance = new EzDoiJournal()

        when:
        new ResolveDoisService().ingestResponse(instance, response)

        then:
        1 == instance.printYear
        "foo" == instance.issue
        50 == instance.onlineYear
    }

    void "test process response on CrossRefResponseException"() {
        when:
        ResolveDoisService service = new ResolveDoisService()
        EzDoi ezDoi = new EzDoi(doi: "foo")
        service.processResponse(
                new CrossRefResponse(
                        statusException: new CrossRefResponseException(400, new URL("http://foo.bar"))
                ),
                ezDoi
        )

        then:
        !ezDoi.resolvableDoi
    }

    void "test convert to BMP (ie all 4byte unicode to 3byte unicode"() {
        given:
        def nonBMPText = "Whole mouse blood microRNA as biomarkers for exposure to 𝛄-rays and56Fe ions"

        when:
        def result = new ResolveDoisService().convertToBMP(nonBMPText)

        then:
        result.contains("_?_-rays")

        when:
        def response = new CrossRefResponse(articleTitle: nonBMPText)
        def journal = new EzDoiJournal()
        new ResolveDoisService().ingestResponse(journal, response)

        then:
        journal.articleTitle.contains("_?_-rays")

        when:
        response = new CrossRefResponse(articleTitle: nonBMPText)
        journal = new EzDoiJournal()
        new ResolveDoisService(use4byte: true).ingestResponse(journal, response)

        then:
        !journal.articleTitle.contains("_?_-rays")
    }
}
