package in.guardianservices.document_utility_core.model;

import lombok.*;

import java.util.List;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PdfResponse {

    private String requestId;
    private List<String> imagePaths;
}
