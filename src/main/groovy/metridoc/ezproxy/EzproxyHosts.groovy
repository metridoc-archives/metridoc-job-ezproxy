package metridoc.ezproxy

import metridoc.iterators.Record

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import static metridoc.ezproxy.TruncateUtils.*

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@Entity
@Table(name = "ez_hosts",
    uniqueConstraints = [
        @UniqueConstraint(name = "ez_hosts_ez_id_host", columnNames = ["ezproxy_id", "url_host"]),
    ]
)
class EzproxyHosts extends EzproxyBase {

    @Column(name = "patron_id")
    String patronId
    @Column(name = "ip_address")
    String ipAddress
    String department
    String organization
    String rank
    String country
    String state
    String city

    @Override
    void populate(Record record) {
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

        super.populate(record)
    }

    @Override
    String createNaturalKey() {
        "${urlHost}_#_${ezproxyId}"
    }

    @Override
    boolean alreadyExists() {
        def session = sessionFactory.currentSession
        def query = session.createQuery("from EzproxyHosts where urlHost = :urlHost and ezproxyId = :ezproxyId")
            .setParameter("urlHost", urlHost)
            .setParameter("ezproxyId", ezproxyId)
        query.list().size() > 0
    }
}
