package metridoc.ezproxy

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Timeout

/**
 * Created with IntelliJ IDEA on 6/14/13
 * @author Tommy Barker
 */
class EzproxyToolSpec extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    EzproxyTool getTool() {
        new EzproxyTool(ezDirectory: folder.root, ezParser: {})
    }

    @Timeout(5)
    def "if no file exists the tool should not choke"() {
        given: "an empty folder"

        when: "I call the tool on the empty folder"
        tool.execute()

        then: "it should not fail or hang"
        notThrown(Throwable)
    }

    def "a file should only be accepted if not already processed"() {
        given: "a folder with a file to process"
        File testFile = folder.newFile("ezproxy.test")

        when: "when not marked as processed"

        then: "file will be accepted for processing"
        tool.acceptFile(testFile)

        when: "file IS marked as processed"
        folder.newFile("ezproxy.test.processed")

        then: "file is will NOT be accepted"
        !tool.acceptFile(testFile)
    }

    def "getFileToProcess will return null if there are no files to process or they dont match the pattern"() {
        given: "a folder that has no files"

        when: "getFileToProcess is called"
        def response = tool.getFileToProcess()

        then: "it will return null"
        null == response

        when: "a non ezproxy file is added"
        folder.newFile("foobar")

        and: "getFileToProcess is called"
        response = tool.getFileToProcess()

        then: "it will return null"
        null == response
    }

    def "getFileToProcess will return null if a valid file has already been processed"() {
        given: "a folder with a processed ezproxy file"
        folder.newFile("ezproxy.testFile")
        folder.newFile("ezproxy.testFile.processed")

        when: "getFileToProcess is called"
        def response = tool.getFileToProcess()

        then: "the response should be null"
        null == response
    }

}
