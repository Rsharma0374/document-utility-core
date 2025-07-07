package in.guardianservices.document_utility_core.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Checks if a PDF file is password protected
     * @param file MultipartFile containing the PDF
     * @return true if password protected, false otherwise
     */
    public static boolean isPasswordProtected(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return document.isEncrypted();
        } catch (IOException e) {
            // If we can't load without password, it's likely password protected
            return e.getMessage() != null && e.getMessage().contains("password");
        }
    }

    /**
     * Validates PDF file format
     * @param file MultipartFile to validate
     * @return true if valid PDF, false otherwise
     */
    public static boolean isValidPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("application/pdf")) {
            logger.error("Invalid content type: {}", contentType);
            return false;
        }

        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".pdf")) {
            logger.error("Invalid file extension: {}", filename);
            return false;
        }

        // Try to load the file to verify it's a valid PDF
        try (PDDocument ignored = PDDocument.load(file.getInputStream())) {
            return true;
        } catch (IOException e) {
            // If it fails due to password, it's still a valid PDF
            if (e.getMessage() != null && e.getMessage().contains("password")) {
                return true;
            }
            logger.error("Invalid PDF file: {}", e.getMessage());
            return false;
        }
    }
}
