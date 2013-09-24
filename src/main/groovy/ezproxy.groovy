import metridoc.core.MetridocScript
import metridoc.core.services.CamelService
import metridoc.core.services.HibernateService
import metridoc.core.tools.ParseArgsTool
import metridoc.ezproxy.entities.EzDoi
import metridoc.ezproxy.entities.EzproxyHosts
import metridoc.ezproxy.services.EzproxyFileFilterService
import metridoc.ezproxy.services.EzproxyIngestService
import metridoc.ezproxy.services.EzproxyService
import metridoc.ezproxy.services.ResolveDoisService

EzproxyService ezproxyService

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
        wireupServices(ezproxyIngestClass)
        ezproxyService.execute()
    }
}

void wireupServices(Class ezproxyIngestClass) {
    use(MetridocScript)  {
        if(!argsMap.containsKey("preview")) {
            includeService(HibernateService, entityClasses: [ezproxyIngestClass])
        }
        ezproxyService = includeService(EzproxyService, entityClass: ezproxyIngestClass)
        def camelService = includeService(CamelService)
        def ezproxyFileFilter = includeService(EzproxyFileFilterService, entityClass: ezproxyIngestClass)
        camelService.bind("ezproxyFileFilter", ezproxyFileFilter)

        def ingestService = includeService(EzproxyIngestService)

        //circular dependency
        ezproxyService.ezproxyIngestService = ingestService
    }
}