package in.guardianservices.document_utility_core.controller;

import in.guardianservices.document_utility_core.messaging.KafkaProducer;
import in.guardianservices.document_utility_core.model.PdfRequest;
import in.guardianservices.document_utility_core.service.PdfStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/pdf")
public class PdfController {

//    @Autowired
//    private PdfStorageService storageService;
//
//    @Autowired
//    private KafkaProducer kafkaProducer;
//
//    @PostMapping("/to-images/async")
//    public ResponseEntity<?> upload(@RequestParam MultipartFile file) throws IOException {
//        String requestId = UUID.randomUUID().toString();
//        String path = storageService.save(file, requestId);
//        kafkaProducer.sendPdfRequest(new PdfRequest(requestId, path));
//        return ResponseEntity.ok(Map.of("requestId", requestId));
//    }
}
