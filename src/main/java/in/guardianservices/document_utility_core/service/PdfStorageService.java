package in.guardianservices.document_utility_core.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PdfStorageService {

    public String save(MultipartFile file, String requestId) throws IOException {
        Path dir = Paths.get("uploads");
        Files.createDirectories(dir);
        Path filePath = dir.resolve(requestId + ".pdf");
        file.transferTo(filePath);
        return filePath.toString();
    }
}
