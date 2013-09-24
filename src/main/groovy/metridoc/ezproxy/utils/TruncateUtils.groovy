package metridoc.ezproxy.utils

import metridoc.iterators.Record

/**
 * @author Tommy Barker
 */
class TruncateUtils {
    public static final DEFAULT_VARCHAR_LENGTH = 255

    @SuppressWarnings("GrMethodMayBeStatic")
    static void truncateProperties(Record record, String... propertyNames) {
        propertyNames.each { propertyName ->
            def propertyValue = record.body[propertyName]
            if (propertyValue && propertyValue instanceof String) {
                record.body[propertyName] = truncate(propertyValue)
            }
        }
    }

    static String truncate(String value) {
        if(value.size() > DEFAULT_VARCHAR_LENGTH) {
            return value.substring(0, DEFAULT_VARCHAR_LENGTH)
        }

        return value
    }
}
