package metridoc.ezproxy.services

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.core.Step

import javax.sql.DataSource

/**
 * Created with IntelliJ IDEA on 9/25/13
 * @author Tommy Barker
 */
@Slf4j
class EzproxyDropTableService {
    DataSource dataSource

    boolean stacktrace

    @Step(description = "drops all ezproxy tables")
    void dropTables() {
        assert dataSource : "A data source has not been set, cannot drop tables"
        def sql = new Sql(dataSource)
        ["ez_doi_journal", "ez_doi", "ez_hosts"].each {
            try {
                sql.execute("drop table $it" as String)
            }
            catch (Throwable throwable) {
                def baseMessage = "Could not drop table [$it], skipping this operation"
                if(!stacktrace) {
                    baseMessage += "\n  $throwable.message"
                    log.warn baseMessage
                    return
                }

                log.warn baseMessage, throwable
            }
        }
    }
}
