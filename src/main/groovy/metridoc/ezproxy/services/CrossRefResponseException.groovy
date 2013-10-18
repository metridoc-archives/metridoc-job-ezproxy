package metridoc.ezproxy.services

/**
 * Created with IntelliJ IDEA on 10/18/13
 * @author Tommy Barker
 */
class CrossRefResponseException extends Exception {
    int statusCode
    URL url

    CrossRefResponseException(int statusCode) {
        this.statusCode = statusCode
    }

    @Override
    String getMessage() {
        "When processing crossRef url $url, a 2xx response was expected, but got $statusCode instead"
    }
}
