package in.guardianservices.document_utility_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class DocumentUtilityCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentUtilityCoreApplication.class, args);
	}

}
