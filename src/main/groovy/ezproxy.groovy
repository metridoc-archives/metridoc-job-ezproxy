import metridoc.core.MetridocScript
import metridoc.core.tools.ParseArgsTool
import metridoc.ezproxy.entities.EzDoi
import metridoc.ezproxy.entities.EzproxyHosts
import metridoc.ezproxy.services.EzproxyService
import metridoc.ezproxy.services.EzproxyWireService
import metridoc.ezproxy.services.ResolveDoisService

use(MetridocScript) {
    includeService(ParseArgsTool)
    assert argsMap: "no arguments were provided, run mdoc help ezproxy"
    def command = argsMap.params[0]
    def commands = ["processHosts", "processDois", "resolveDois"]

    if (!commands.contains(command)) {
        println ""
        println "  $command is not one of $commands, run [mdoc help ezproxy]"
        println ""
    }

    switch (command) {
        case "processHosts":
            println "processing hosts"
            ingestFor(EzproxyHosts)
            return
        case "processDois":
            println "processing dois"
            ingestFor(EzDoi)
            return
        case "resolveDois":
            println "resolving dois"
            includeService(ResolveDoisService).execute()
            return
    }
}

void ingestFor(Class ezproxyIngestClass) {
    use(MetridocScript)  {
        def ezproxyService = wireupServices(ezproxyIngestClass)
        ezproxyService.execute()
    }
}

EzproxyService wireupServices(Class ezproxyIngestClass) {
    use(MetridocScript)  {
        return includeService(EzproxyWireService).wireupServices(ezproxyIngestClass)
    }
}