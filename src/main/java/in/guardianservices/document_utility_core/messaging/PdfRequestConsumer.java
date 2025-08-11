package in.guardianservices.document_utility_core.messaging;

import in.guardianservices.document_utility_core.model.PdfRequest;
import in.guardianservices.document_utility_core.model.PdfResponse;
import in.guardianservices.document_utility_core.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class PdfRequestConsumer {

//
//    @Autowired
//    private PdfService pdfService;
//
//    @Autowired
//    private PdfResponseProducer responseProducer;
//
//    @KafkaListener(topics = "pdf-to-image-requests")
//    public void consume(PdfRequest request) throws IOException {
//        List<String> imagePaths = pdfService.convert(request.getFilePath());
//        responseProducer.sendPdfResponse(new PdfResponse(request.getRequestId(), imagePaths));
//    }
}
