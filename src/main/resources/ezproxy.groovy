import metridoc.core.MetridocScript
import metridoc.core.tools.ParseArgsTool
import metridoc.ezproxy.EzDoi

import metridoc.ezproxy.EzproxyHosts
import metridoc.ezproxy.EzproxyTool

use(MetridocScript) {
    includeTool(ParseArgsTool)
    def command = argsMap.params[0]
    def commands = ["processHosts", "processDois", "resolveDois"]

    if(!commands.contains(command)) {
        println ""
        println "  $command is not one of $commands, run [mdoc help ezproxy]"
        println ""
    }

    switch(command) {
        case "processHosts":
            includeTool(entityClass: EzproxyHosts, EzproxyTool).execute()
            break
        case "processDois":
            println "processing dois"
            includeTool(entityClass: EzDoi, EzproxyTool).execute()
            break
    }
}

