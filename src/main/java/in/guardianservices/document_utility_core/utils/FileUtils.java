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

    /**
     * Validates if the file size is within acceptable limits for Base64 conversion
     * @param file the MultipartFile
     * @param maxSizeInMB maximum allowed size in MB
     * @return true if file size is acceptable
     */
    public static boolean isFileSizeAcceptable(MultipartFile file, int maxSizeInMB) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        long maxSizeInBytes = (long) maxSizeInMB * 1024 * 1024;
        return file.getSize() <= maxSizeInBytes;
    }

    /**
     * Validates if byte array contains valid PDF data
     * @param bytes the byte array to validate
     * @return true if bytes form a valid PDF
     */
    public static boolean isValidPdfBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }

        // Check PDF header signature (%PDF-)
        String header = new String(bytes, 0, 8);
        if (!header.startsWith("%PDF-")) {
            return false;
        }

        // Check for PDF footer (%%EOF)
        String content = new String(bytes);
        return content.contains("%%EOF");
    }
}
