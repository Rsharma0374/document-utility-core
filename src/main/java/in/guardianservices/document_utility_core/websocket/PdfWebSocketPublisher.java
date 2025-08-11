package in.guardianservices.document_utility_core.websocket;

import in.guardianservices.document_utility_core.model.PdfResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class PdfWebSocketPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PdfWebSocketPublisher.class);
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

//    @KafkaListener(topics = "pdf-to-image-responses")
//    public void publish(PdfResponse response) {
//        logger.warn("Received pdf-to-image-responses. Message: " + response.toString());
//        messagingTemplate.convertAndSend("/topic/pdf-result/" + response.getRequestId(), response);
//    }
}
