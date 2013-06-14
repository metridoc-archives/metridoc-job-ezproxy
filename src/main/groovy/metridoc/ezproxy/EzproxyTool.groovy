package metridoc.ezproxy

import groovy.util.logging.Slf4j
import metridoc.core.tools.CamelTool
import metridoc.core.tools.RunnableTool
import metridoc.iterators.Iterators
import metridoc.writers.IteratorWriter
import metridoc.writers.TableIteratorWriter
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter

/**
 * Created with IntelliJ IDEA on 6/13/13
 * @author Tommy Barker
 */
@Slf4j
class EzproxyTool extends RunnableTool {
    String ezEncoding = "utf-8"

    Closure<Map> ezParser
    Closure<Map> ezTransformer
    String ezFileFilter = "ezproxy*"
    File ezDirectory
    File ezFile
    IteratorWriter writer = new TableIteratorWriter()

    @Override
    Object configure() {
        assert ezParser: "ezproxy parser cannot be null"

        if (!ezFile) {
            assert ezFileFilter: "ezproxy parser cannot be null"
            assert ezDirectory: 'ezproxy directory cannot be null'
        }

        target(processEzproxyFile: "default target for processing ezproxy file") {
            def ezIterator

            long readLockTimeout = 1000 * 60 * 60 //one hour

            if (!ezFile) {
                String fileUrl = "${ezDirectory.toURI().toURL()}?noop=true&readLockTimeout=${readLockTimeout}&antInclude=${ezFileFilter}&sendEmptyMessageWhenIdle=true"
                def camelTool = includeTool(CamelTool)
                camelTool.consume(fileUrl) { File file ->
                    ezFile = file
                }
            }

            if (ezFile == null) {
                log.info "no files were found in $ezDirectory"
                return
            }

            ezIterator = new EzproxyIterator(
                    inputStream: ezFile.newInputStream(),
                    fileName: ezFile.name,
                    ezParser: ezParser,
                    ezEncoding: ezEncoding
            )

            def filteredIterator = Iterators.toFilteredAndTransformedIterator(ezIterator) { Map record ->
                ezTransformer.call(record)
            }

            writer.rowIterator = filteredIterator
            writer.write()
            if (binding.hasVariable("camelTool")) {
                binding.camelTool.close()
            }
        }

        setDefaultTarget("processEzproxyFile")
    }

    boolean acceptFile(File file) {
        def fileName = file.name

        if (fileName.endsWith(".processed")) {
            return false
        }

        def processedFileName = "${fileName}.processed"
        def processedFile = new File(file.parent, processedFileName)

        if (processedFile.exists()) {
            return false
        }

        return true
    }

    protected File getFileToProcess() {
        long readLockTimeout = 1000 * 60 * 60 //one hour
        if (!ezFile) {
            String fileUrl = "${ezDirectory.toURI().toURL()}?noop=true&readLockTimeout=${readLockTimeout}&antInclude=${ezFileFilter}&sendEmptyMessageWhenIdle=true&filter=#ezproxyFileFilter"
            def camelTool = includeTool(CamelTool)
            camelTool.bind("ezproxyFileFilter",
                    [
                            accept: { GenericFile file ->
                                acceptFile(file.file)
                            }
                    ] as GenericFileFilter
            )
            camelTool.consume(fileUrl) { File file ->
                ezFile = file
            }
        }

        return ezFile
    }
}


