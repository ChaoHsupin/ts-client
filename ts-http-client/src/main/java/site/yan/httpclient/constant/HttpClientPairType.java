package site.yan.httpclient.constant;

public enum HttpClientPairType {
    REMOTE_SERVER("remote server"),
    PATH("path"),
    STATUS_CODE("status code"),
    CONTENT_SIZE("content size"),
    EXCEPTION("exception");

    private final String value;

    HttpClientPairType(String s) {
        this.value = s;
    }

    public String text() {
        return this.value;
    }
}
