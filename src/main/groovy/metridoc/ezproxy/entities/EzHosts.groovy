/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.ezproxy.entities

import grails.persistence.Entity
import groovy.util.logging.Slf4j
import metridoc.iterators.Record

import static metridoc.ezproxy.utils.TruncateUtils.truncateProperties

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@Entity
@Slf4j
class EzHosts extends EzproxyBase {

    String patronId
    String ipAddress
    String department
    String organization
    String rank
    String country
    String state
    String city


    static constraints = {
        runBaseConstraints(delegate, it)
        urlHost (unique: "ezproxyId")
        patronId(nullable: true)
        ipAddress(nullable: true)
        department(nullable: true)
        rank(nullable: true)
        country(nullable: true)
        state(nullable: true)
        city(nullable: true)
        organization(nullable: true)
    }

    @Override
    boolean acceptRecord(Record record) {
        truncateProperties(record,
                "patronId",
                "ipAddress",
                "department",
                "organization",
                "rank",
                "country",
                "state",
                "city"
        )

        super.acceptRecord(record)
    }

    @Override
    String createNaturalKey() {
        "${urlHost}_#_${ezproxyId}"
    }

    @Override
    boolean alreadyExists() {
        def answer
        withTransaction {
            log.debug "checking for {} and {} for EzHosts", ezproxyId, urlHost
            answer = EzHosts.findByEzproxyIdAndUrlHost(ezproxyId, urlHost) != null
        }

        return answer
    }
}
