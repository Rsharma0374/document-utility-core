package in.guardianservices.document_utility_core.model;

public class PdfRequest {

    private String requestId;
    private String path;

    public PdfRequest() {
    }

    public PdfRequest(String requestId, String path) {
        this.requestId = requestId;
        this.path = path;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}