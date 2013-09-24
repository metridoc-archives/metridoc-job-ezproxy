import metridoc.core.MetridocScript
import metridoc.core.tools.ParseArgsTool
import metridoc.ezproxy.services.EzproxyService
import metridoc.ezproxy.services.ResolveDoisService
import metridoc.ezproxy.entities.EzDoi
import metridoc.ezproxy.entities.EzproxyHosts

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
            includeService(entityClass: EzproxyHosts, EzproxyService).execute()
            return
        case "processDois":
            println "processing dois"
            includeService(entityClass: EzDoi, EzproxyService).execute()
            return
        case "resolveDois":
            println "resolving dois"
            includeService(ResolveDoisService).resolveDois()
            return
    }
}