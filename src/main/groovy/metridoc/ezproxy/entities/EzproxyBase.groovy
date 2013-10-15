package metridoc.ezproxy.entities

import grails.persistence.Entity
import metridoc.iterators.Record
import metridoc.utils.ApacheLogParser
import org.slf4j.LoggerFactory

import static metridoc.ezproxy.utils.TruncateUtils.truncateProperties

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
abstract class EzproxyBase {
    public static final transient NATURAL_KEY_CACHE = "naturalKeyCache"
    Date proxyDate
    Integer proxyMonth
    Integer proxyYear
    Integer proxyDay
    String ezproxyId
    String fileName
    String urlHost
    Integer lineNumber
    Set<String> naturalKeyCache = []

    static transients = ['naturalKeyCache']

    static constraints = {
        ezproxyId(maxSize: 50)
    }

    static mapping = {
        fileName(index: "idx_file_name")
        ezproxyId(index: "idx_ezproxy_id")
        urlHost(index: "idx_url_host")
        version(false)
    }

    static runBaseConstraints(delegate, it) {
        runStaticClosure(constraints, delegate, it)
    }

    static runBaseMapping(delegate, it) {
        runStaticClosure(mapping, delegate, it)
    }

    static runStaticClosure(Closure closure, delegate, it) {
        def clone = closure.clone() as Closure
        clone.delegate = delegate
        clone.call(it)
    }

    boolean acceptRecord(Record record) {
        def cache = record.getHeader(NATURAL_KEY_CACHE, Set)
        if(cache) {
            naturalKeyCache = cache
        }
        else {
            cache = [] as Set<String>
            record.headers[NATURAL_KEY_CACHE] = cache
            naturalKeyCache = cache
        }

        truncateProperties(record, "ezproxyId", "fileName", "urlHost")
        addDateValues(record.body)

        record.body.ezproxyId &&
                record.body.urlHost
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
