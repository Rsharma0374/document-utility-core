package in.guardianservices.document_utility_core.messaging;

import in.guardianservices.document_utility_core.model.PdfResponse;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PdfResponseProducer {

//
//    @Autowired
//    private KafkaTemplate<String, PdfResponse> kafkaTemplate;
//
//    public void sendPdfResponse(PdfResponse response) {
//        kafkaTemplate.send("pdf-to-image-responses", response);
//    }
}
