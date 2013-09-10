package metridoc.ezproxy

import metridoc.entities.MetridocRecordEntity
import metridoc.iterators.Record
import metridoc.utils.ApacheLogParser
import org.hibernate.SessionFactory
import org.hibernate.annotations.Index
import org.slf4j.LoggerFactory

import javax.persistence.Column
import javax.persistence.MappedSuperclass
import static metridoc.ezproxy.TruncateUtils.*

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@MappedSuperclass
abstract class EzproxyBase extends MetridocRecordEntity {
    public static final transient DEFAULT_VARCHAR_LENGTH = 255
    public static final transient NATURAL_KEY_CACHE = "naturalKeyCache"
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
    @Column(name = "url_host", nullable = false)
    @Index(name = "idx_ezproxy_id_url_host")
    String urlHost
    @Column(name = "line_number", nullable = false)
    Integer lineNumber
    transient Set<String> naturalKeyCache = []
    transient SessionFactory sessionFactory

    @Override
    boolean acceptRecord(Record record) {
        record.body.ezproxyId &&
                record.body.urlHost
    }

    @Override
    void populate(Record record) {
        def cache = record.getHeader(NATURAL_KEY_CACHE, Set)
        if(cache) {
            naturalKeyCache = cache
        }
        else {
            cache = [] as Set<String>
            record.headers[NATURAL_KEY_CACHE] = cache
            naturalKeyCache = cache
        }
        sessionFactory = record.getHeader("sessionFactory", SessionFactory)
        addDateValues(record.body)
        truncateProperties(record, "ezproxyId", "fileName", "urlHost")
        super.populate(record)
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
                    return
                }
            }

            def calendar = new GregorianCalendar()
            calendar.setTime(record.proxyDate as Date)
            record.proxyYear = calendar.get(Calendar.YEAR)
            record.proxyMonth = calendar.get(Calendar.MONTH) + 1
            record.proxyDay = calendar.get(Calendar.DAY_OF_MONTH)
        }
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected boolean notNull(item) {
        if (item == null) {
            return false
        }

        if (item instanceof String) {
            return item.trim()
        }
        return true
    }

    @Override
    void validate() {
        ["lineNumber", "fileName", "urlHost", "proxyDate", "ezproxyId", "proxyMonth", "proxyDay", "proxyYear"].each { property ->
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

    @Override
    boolean shouldSave() {
        String naturalKey = createNaturalKey()
        if(naturalKeyCache.add(naturalKey)) {
            return !alreadyExists()
        }

        return false
    }

    abstract String createNaturalKey()
    abstract boolean alreadyExists()
}
