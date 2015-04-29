/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.  */

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
