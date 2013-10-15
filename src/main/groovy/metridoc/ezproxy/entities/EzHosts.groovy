package metridoc.ezproxy.entities

import grails.persistence.Entity
import metridoc.iterators.Record

import static metridoc.ezproxy.utils.TruncateUtils.truncateProperties

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@Entity
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
            answer = EzHosts.findByEzproxyIdAndUrlHost(ezproxyId, urlHost) != null
        }

        return answer
    }
}
