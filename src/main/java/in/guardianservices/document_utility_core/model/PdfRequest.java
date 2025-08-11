package in.guardianservices.document_utility_core.model;

import lombok.*;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfRequest {

    private String requestId;
    private String filePath;
}
