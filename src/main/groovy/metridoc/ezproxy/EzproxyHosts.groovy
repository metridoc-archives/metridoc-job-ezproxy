package metridoc.ezproxy

import javax.persistence.Column
import javax.persistence.Entity

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@Entity
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
}
