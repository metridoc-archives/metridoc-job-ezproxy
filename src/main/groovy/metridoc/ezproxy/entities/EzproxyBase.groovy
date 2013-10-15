package metridoc.ezproxy.entities

import com.sun.xml.internal.ws.util.StringUtils
import groovy.util.logging.Slf4j
import metridoc.iterators.Record
import metridoc.utils.ApacheLogParser
import org.slf4j.LoggerFactory

import static metridoc.ezproxy.utils.TruncateUtils.truncateProperties

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@Slf4j
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

    static transients = ['naturalKeyCache', 'fieldsToLoad']

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
        log.debug  "checking to accept record {}", record
        def cache = record.getHeader(NATURAL_KEY_CACHE, Set)
        if (cache) {
            naturalKeyCache = cache
        }
        else {
            cache = [] as Set<String>
            record.headers[NATURAL_KEY_CACHE] = cache
            naturalKeyCache = cache
        }

        boolean result = record.body.ezproxyId &&
                record.body.urlHost

        if (!result) {
            log.debug "record {} was rejected", record
            return false
        }

        truncateProperties(record, "ezproxyId", "fileName", "urlHost")
        addDateValues(record.body)

        log.debug "record {} was accepted", record
        return result
    }

    void populate(Record record) {
        log.debug "populating {}", record

        def dataOfInterest = record.body.findAll {
            String propertyName = StringUtils.capitalize(it.key)
            this.metaClass.respondsTo(this, "set${propertyName}", [it.value.getClass()] as Object[])
        }

        dataOfInterest.each {
            log.debug "updating property [$it.key] with [$it.value]"
            this."$it.key" = it.value
        }
        log.debug "finished populating {}", record
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
        if (naturalKeyCache.add(naturalKey)) {
            def doesNotExist = !alreadyExists()
            if (doesNotExist) {
                log.debug "validating [{}]", this
                if (!this.validate()) {
                    log.debug "[$this] is invalid"
                    if (this.errors.fieldErrorCount) {
                        def message = "error on field [${this.errors.fieldError.field}] with error code [${this.errors.fieldError.code}]"
                        throw new AssertionError(message)
                    }
                    else {
                        throw new RuntimeException("unknown error occurred \n ${this.errors}")
                    }
                }
                log.debug "[{}] will be saved", naturalKey
            } else {
                log.debug "[{}] will not be saved", naturalKey
            }

            return doesNotExist
        }

        log.debug "[{}] is already in the cache, will not save", naturalKey
        return false
    }

    abstract String createNaturalKey()

    abstract boolean alreadyExists()
}
