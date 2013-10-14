import metridoc.core.services.ConfigService
import metridoc.ezproxy.entities.EzDoi
import metridoc.ezproxy.entities.EzHosts
import metridoc.ezproxy.services.EzproxyDropTableService
import metridoc.ezproxy.services.EzproxyService
import metridoc.ezproxy.services.EzproxyWireService
import metridoc.ezproxy.services.ResolveDoisService

includeService(ConfigService)
assert argsMap: "no arguments were provided, run [mdoc help ezproxy]"
assert argsMap.params: "a command must be provided, run [mdoc help ezproxy]"

def command = argsMap.params[0]
def commands = ["processHosts", "processDois", "resolveDois", "dropTables"]

if (!commands.contains(command)) {
    println ""
    println "  $command is not one of $commands, run [mdoc help ezproxy]"
    println ""
    System.exit(1)
}

switch (command) {
    case "processHosts":
        println "processing hosts"
        ingestFor(EzHosts)
        return
    case "processDois":
        println "processing dois"
        ingestFor(EzDoi)
        return
    case "resolveDois":
        println "resolving dois"
        includeService(ResolveDoisService).execute()
        return
    case "dropTables":
        println "dropping tables"
        includeService(EzproxyDropTableService)
        runStep("dropTables")
        return
}

void ingestFor(Class ezproxyIngestClass) {
    wireupServices(ezproxyIngestClass)
    if (argsMap.preview) {
        runStep("preview")
    }
    else {
        runStep("processEzproxyFile")
    }
}

EzproxyService wireupServices(Class ezproxyIngestClass) {
    return includeService(EzproxyWireService).wireupServices(ezproxyIngestClass)
}