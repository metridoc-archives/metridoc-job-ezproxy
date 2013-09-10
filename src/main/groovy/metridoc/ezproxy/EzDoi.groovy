package metridoc.ezproxy

import metridoc.iterators.Record
import org.hibernate.annotations.Index

import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import java.util.regex.Pattern
import static metridoc.ezproxy.TruncateUtils.*

/**
 * Created with IntelliJ IDEA on 7/12/13
 * @author Tommy Barker
 */
@Entity
@Table(name = "ez_doi",
        uniqueConstraints = [
            @UniqueConstraint(name = "ez_doi_ez_id_doi", columnNames = ["ezproxy_id", "doi"]),
        ]
)
class EzDoi extends EzproxyBase {

    @Index(name = "idx_doi")
    String doi
    Boolean processedDoi = false
    Boolean resolvableDoi = false

    public static final transient DOI_PREFIX_PATTERN = "10."
    public static final transient DOI_PROPERTY_PATTERN = "doi=10."
    public static final transient  DOI_FULL_PATTERN = Pattern.compile(/10\.\d+\//)

    @Override
    void populate(Record record) {
        def body = record.body
        doi = extractDoi(body.url)
        truncateProperties(record, "doi")
        super.populate(record)
    }

    @Override
    void validate() {
        ["doi"].each { property ->
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
    boolean acceptRecord(Record record) {
        boolean hasEzproxyIdAndHost = super.acceptRecord(record)

        if(!hasEzproxyIdAndHost) {
            return false
        }

        return hasDoi(record.body)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected String extractDoi(String url) {
        String result = null
        int idxBegin = url.indexOf(DOI_PROPERTY_PATTERN)
        boolean
        if (idxBegin > -1) {
            String doiBegin = url.substring(idxBegin + 4)
            int idxEnd = doiBegin.indexOf('&') > 0 ? doiBegin.indexOf('&') : doiBegin.size()
            result = URLDecoder.decode(URLDecoder.decode(doiBegin.substring(0, idxEnd), "utf-8"), "utf-8") //double encoding
        } else {
            idxBegin = url.indexOf(DOI_PREFIX_PATTERN)
            if (idxBegin > -1) {
                String doiBegin = url.substring(idxBegin)
                //find index of 2nd slash
                int slashInd = doiBegin.indexOf("/");
                slashInd = slashInd > -1 ? doiBegin.indexOf("/", slashInd + 1) : -1;
                int idxEnd = doiBegin.indexOf('?')
                if (idxEnd == -1) {
                    //case where doi is buried in embedded camelUrl
                    doiBegin = URLDecoder.decode(doiBegin, "utf-8")
                    idxEnd = doiBegin.indexOf('&')
                    slashInd = slashInd > -1 ? doiBegin.indexOf("/", slashInd + 1) : -1; // compute again in case of encoding
                }
                if (idxEnd > -1) {
                    if (slashInd > -1) {
                        idxEnd = [slashInd, idxEnd].min()
                    }
                } else if (slashInd > -1) {
                    idxEnd = slashInd
                } else {
                    idxEnd = doiBegin.size()
                }
                result = doiBegin.substring(0, idxEnd)
            }
        }

        if (result && result.contains("/")) {
            int startIndex = result.indexOf("/")
            String suffix = result.substring(startIndex + 1, result.length())
            int nextSlash = suffix.indexOf("/")
            if (nextSlash > -1) {
                result = result.substring(0, startIndex + nextSlash + 1)
            }
        } else {
            result = null //must be garbage
        }
        return result
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected boolean hasDoi(Map record) {
        String url = record.url
        int indexOfDoiPrefix = url.indexOf(DOI_PREFIX_PATTERN)
        if (indexOfDoiPrefix > -1) {
            def doiAtStart = url.substring(indexOfDoiPrefix)
            //noinspection GroovyUnusedCatchParameter
            try {
                doiAtStart = URLDecoder.decode(doiAtStart, "utf-8")
            } catch (IllegalArgumentException ex) {

            }
            def doiMatcher = DOI_FULL_PATTERN.matcher(doiAtStart)
            return doiMatcher.lookingAt()
        }

        return false
    }

    @Override
    String createNaturalKey() {
        return "${ezproxyId}_#_${doi}"
    }

    @Override
    boolean alreadyExists() {
        def session = sessionFactory.currentSession
        def query = session.createQuery("from EzDoi where doi = :doi and ezproxyId = :ezproxyId")
                .setParameter("doi", doi)
                .setParameter("ezproxyId", ezproxyId)
        query.list().size() > 0
    }
}
