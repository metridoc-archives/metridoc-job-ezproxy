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

import metridoc.iterators.Record
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
class EzproxyIteratorServiceSpec extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    def "if result is empty or null an AssertionError will occur"() {
        given: "a file with a lot of lines"
        def file = folder.newFile("fileWithLotsOfLines")
        file.withPrintWriter {PrintWriter writer ->
            (0..10000).each {
                writer.println(it)
            }
        }

        and: "a parser that always returns null"
        def nullParser = {new Record(body: null)}

        and: "a parser that always returns an empty Map"
        def emptyParser = {new Record(body: [:])}

        when: "when next is called for null parser iterator"
        def record = new EzproxyIteratorService(
                inputStream: file.newInputStream(),
                parser: nullParser,
                delimiter: "\\|\\|",
                proxyDate: 1,
                url: 2,
                ezproxyId: 3
        ).next()

        then: "an AssertionError is thrown"
        record.throwable instanceof AssertionError

        when: "when next is called for empty parser iterator"
        record = new EzproxyIteratorService(
                inputStream: file.newInputStream(),
                parser: emptyParser,
                delimiter: "\\|\\|",
                proxyDate: 1,
                url: 2,
                ezproxyId: 3
        ).next()

        then: "an AssertionError is thrown"
        record.throwable instanceof AssertionError
    }

    def "test cases where target file makes no sense and we are doing a preview"() {
        given:
        def badFile = folder.newFile("badFile.log")
        badFile.withPrintWriter {writer ->
            writer.println("lkjhasdflkjh")
            writer.println("lkjhas||dflkjh")
        }

        when:
        new EzproxyIteratorService(
                inputStream: badFile.newInputStream()
        ).preview()

        then:
        thrown(AssertionError)
    }
}
