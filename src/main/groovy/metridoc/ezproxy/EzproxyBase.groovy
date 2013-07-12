package metridoc.ezproxy

import metridoc.entities.MetridocRecordEntity
import metridoc.iterators.Record
import metridoc.utils.ApacheLogParser
import org.hibernate.annotations.Index
import org.slf4j.LoggerFactory

import javax.persistence.Column
import javax.persistence.MappedSuperclass

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@MappedSuperclass
abstract class EzproxyBase extends MetridocRecordEntity {
    public static final transient APACHE_NULL = "-"
    public static final transient DEFAULT_VARCHAR_LENGTH = 255
    @Column(name = "proxy_date", nullable = false)
    Date proxyDate
    @Column(name = "proxy_month", nullable = false)
    Integer proxyMonth
    @Column(name = "proxy_year", nullable = false)
    Integer proxyYear
    @Column(name = "proxy_day", nullable = false)
    Integer proxyDay
    @Column(name = "ezproxy_id", nullable = false)
    @Index(name = "idx_ezproxy_id_url_host")
    String ezproxyId
    @Column(name = "file_name", nullable = false)
    @Index(name = "idx_file_name")
    String fileName
    @Column(name = "file_name", nullable = false)
    @Index(name = "idx_ezproxy_id_url_host")
    String urlHost

    @Override
    boolean acceptRecord(Record record) {
        record.body.containsKey("ezproxy_id")
    }

    @Override
    void populate(Record record) {
        addDateValues(record.body)
        truncateProperties(record, )
        super.populate(record)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected void truncateProperties(Record record, String... propertyNames) {
        propertyNames.each { propertyName ->
            def propertyValue = record.body[propertyName]
            if (propertyValue && propertyValue instanceof String) {
                record.body[propertyName] = propertyValue.substring(0, DEFAULT_VARCHAR_LENGTH)
            }
        }
    }

    protected void addDateValues(Map record) {
        def proxyDate = record.proxyDate
        if (notNull(proxyDate)) {
            if (proxyDate instanceof String) {
                try {
                    //transform it to a date type
                    record.proxyDate = ApacheLogParser.parseLogDate(proxyDate)
                }
                catch (Throwable ignored) {
                    def log = LoggerFactory.getLogger(this.getClass())
                    log.warn("Could not parse date $proxyDate")
                    //if this data point is important validation will fail
                }
            }

            def calendar = new GregorianCalendar()
            calendar.setTime(record.proxyDate as Date)
            record.proxyYear = calendar.get(Calendar.YEAR)
            record.proxyMonth = calendar.get(Calendar.MONTH)
            record.proxyDay = calendar.get(Calendar.DAY_OF_MONTH)
        }
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected boolean notNull(item) {
        if (item == null) {
            return false
        }

        if (item instanceof String) {
            return item.trim() && item.trim() != APACHE_NULL
        }
        return true
    }

    @Override
    void validate() {
        ["fileName", "urlHost", "proxyDate", "ezproxyId", "proxyMonth", "proxyDay", "proxyYear"].each { property ->
            def value = this."$property"
            if (value instanceof Integer) {
                assert value != null: "$property cannot be null"
            }
            else if (value instanceof String) {
                assert notNull(value) : "$property cannot be null or empty"
            }
            else {
                assert value: "$property cannot be null or empty"
            }
        }
    }
}
