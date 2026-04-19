package in.guardianservices.document_utility_core.model;

import java.util.List;

public class PdfResponse {

    private String status;
    private List<String> images;

    public PdfResponse() {
    }

    public PdfResponse(String status, List<String> images) {
        this.status = status;
        this.images = images;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}