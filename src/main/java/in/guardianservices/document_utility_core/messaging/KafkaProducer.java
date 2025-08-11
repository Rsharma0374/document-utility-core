package in.guardianservices.document_utility_core.messaging;

import in.guardianservices.document_utility_core.model.PdfRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {


    @Autowired
    private KafkaTemplate<String, PdfRequest> kafkaTemplate;

    public void sendPdfRequest(PdfRequest request) {
        kafkaTemplate.send("pdf-to-image-requests", request);
    }
}
