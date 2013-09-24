import metridoc.core.MetridocScript
import metridoc.core.tools.HibernateTool
import metridoc.core.tools.ParseArgsTool
import metridoc.ezproxy.CrossRefTool
import metridoc.ezproxy.entities.EzDoi
import metridoc.ezproxy.entities.EzDoiJournal
import metridoc.ezproxy.entities.EzproxyHosts
import metridoc.ezproxy.EzproxyService
import metridoc.ezproxy.TruncateUtils
import org.hibernate.Session

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
            resolveDois()
            return
    }
}

void resolveDois() {
    use(MetridocScript) {
        def hibernateTool = includeService(entityClasses: [EzDoiJournal, EzDoi], HibernateTool)
        def count = argsMap.doiResolutionCount ?: 2000

        hibernateTool.withTransaction { Session session ->

            def q = session.createQuery("from EzDoi where processedDoi = false")
            q.setMaxResults(count)
            def ezDois = q.list()
            if (ezDois) {
                println "processing ${ezDois.size()} dois"
            }
            else {
                println "there are no dois to process"
            }

            CrossRefTool crossRefTool = includeService(CrossRefTool)
            int counter = 0
            ezDois.each { EzDoi ezDoi ->
                counter++
                if (counter % 100 == 0) {
                    println "processed $counter records"
                }
                def response = crossRefTool.resolveDoi(ezDoi.doi)
                assert !response.loginFailure: "Could not login into cross ref"
                if (response.malformedDoi || response.unresolved) {
                    ezDoi.resolvableDoi = false
                    println "Could not resolve doi $ezDoi.doi, it was either malformed or unresolvable"
                }

                else {
                    q = session.createQuery("from EzDoiJournal where doi = '${response.doi}'")
                    if(q.list()) {
                        println "doi ${response.doi} has already been processed"
                        return
                    }
                    def ezJournal = new EzDoiJournal()
                    ezJournal.properties.findAll {
                        it.key != "id" &&
                                it.key != "version" &&
                                it.key != "class"
                    }.each { key, value ->

                        def chosenValue = response."$key"
                        if (chosenValue instanceof String) {
                            chosenValue = TruncateUtils.truncate(chosenValue)
                        }

                        ezJournal."$key" = chosenValue
                    }
                    session.save(ezJournal)
                }

                ezDoi.processedDoi = true
                session.save(ezDoi)
            }
        }
    }
}