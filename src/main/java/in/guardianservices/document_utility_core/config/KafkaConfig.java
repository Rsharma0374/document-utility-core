package in.guardianservices.document_utility_core.config;

import in.guardianservices.document_utility_core.model.PdfRequest;
import in.guardianservices.document_utility_core.model.PdfResponse;
import org.springframework.context.annotation.Configuration;

//import org.apache.kafka.clients.admin.NewTopic;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.*;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
@Configuration
public class KafkaConfig {
//
//    private final String bootstrapServers = "localhost:9092";
//
//    // Kafka Producer Config
//    @Bean
//    public ProducerFactory<String, PdfRequest> pdfRequestProducerFactory() {
//        Map<String, Object> config = new HashMap<>();
//        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        return new DefaultKafkaProducerFactory<>(config);
//    }
//
//    @Bean
//    public KafkaTemplate<String, PdfRequest> pdfRequestKafkaTemplate() {
//        return new KafkaTemplate<>(pdfRequestProducerFactory());
//    }
//
//    @Bean
//    public ProducerFactory<String, PdfResponse> pdfResponseProducerFactory() {
//        Map<String, Object> config = new HashMap<>();
//        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        return new DefaultKafkaProducerFactory<>(config);
//    }
//
//    @Bean
//    public KafkaTemplate<String, PdfResponse> pdfResponseKafkaTemplate() {
//        return new KafkaTemplate<>(pdfResponseProducerFactory());
//    }
//
//    // Kafka Consumer Config for PdfRequest
//    @Bean
//    public ConsumerFactory<String, PdfRequest> pdfRequestConsumerFactory() {
//        JsonDeserializer<PdfRequest> deserializer = new JsonDeserializer<>(PdfRequest.class);
//        deserializer.addTrustedPackages("*");
//
//        Map<String, Object> config = new HashMap<>();
//        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        config.put(ConsumerConfig.GROUP_ID_CONFIG, "pdf-processor-group");
//        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//
//        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, PdfRequest> pdfRequestListenerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, PdfRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(pdfRequestConsumerFactory());
//        return factory;
//    }
//
//    // Kafka Consumer Config for PdfResponse (WebSocket push)
//    @Bean
//    public ConsumerFactory<String, PdfResponse> pdfResponseConsumerFactory() {
//        JsonDeserializer<PdfResponse> deserializer = new JsonDeserializer<>(PdfResponse.class);
//        deserializer.addTrustedPackages("*");
//
//        Map<String, Object> config = new HashMap<>();
//        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        config.put(ConsumerConfig.GROUP_ID_CONFIG, "pdf-websocket-group");
//        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//
//        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, PdfResponse> pdfResponseListenerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, PdfResponse> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(pdfResponseConsumerFactory());
//        return factory;
//    }
//
//    // Optional: Kafka topic definitions
//    @Bean
//    public NewTopic requestTopic() {
//        return new NewTopic("pdf-to-image-requests", 1, (short) 1);
//    }
//
//    @Bean
//    public NewTopic responseTopic() {
//        return new NewTopic("pdf-to-image-responses", 1, (short) 1);
//    }
}
