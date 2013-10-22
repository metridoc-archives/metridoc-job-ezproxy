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
import metridoc.iterators.Record
import org.slf4j.LoggerFactory

import java.util.regex.Pattern

import static metridoc.ezproxy.utils.TruncateUtils.truncateProperties

/**
 * Created with IntelliJ IDEA on 7/12/13
 * @author Tommy Barker
 */
@Entity
class EzDoi extends EzproxyBase {

    String doi
    Boolean processedDoi = false
    Boolean resolvableDoi = false

    public static final transient DOI_PREFIX_PATTERN = "10."
    public static final transient DOI_PROPERTY_PATTERN = "doi=10."
    public static final transient  DOI_FULL_PATTERN = Pattern.compile(/10\.\d+\//)

    static mapping = {
        runBaseMapping(delegate, it)
        doi(index: true)
    }

    boolean acceptRecord(Record record) {
        boolean hasEzproxyIdAndHost = super.acceptRecord(record)

        if(!hasEzproxyIdAndHost) {
            return false
        }

        def body = record.body
        try {
            if (hasDoi(body)) {
                body.doi = extractDoi(body.url)
            }
        }
        catch (Throwable throwable) {
            /*
                any number of issues could cause an exception.  We don't want to cause a failure though.  The doi will
                just be null and the record considered invalid
            */
            def log = LoggerFactory.getLogger(EzDoi)
            log.warn "Could not extract doi from $body.url", throwable
        }
        truncateProperties(record, "doi")
        return body.doi != null
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

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    boolean alreadyExists() {
        def answer
        withTransaction {
            answer = EzDoi.findByEzproxyIdAndDoi(ezproxyId, doi)
        }

        return answer != null
    }
}
