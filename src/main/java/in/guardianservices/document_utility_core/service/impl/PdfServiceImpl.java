package in.guardianservices.document_utility_core.service.impl;

import in.guardianservices.document_utility_core.exception.InvalidPasswordException;
import in.guardianservices.document_utility_core.service.PdfService;
import in.guardianservices.document_utility_core.utils.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class PdfServiceImpl implements PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    /**
     * Unlocks a password-protected PDF and returns the unlocked PDF as byte array
     * @param file MultipartFile containing the password-protected PDF
     * @param password Password to unlock the PDF
     * @return byte array of the unlocked PDF
     * @throws InvalidPasswordException if the password is incorrect
     */
    public byte[] unlockPdf(MultipartFile file, String password) throws InvalidPasswordException, IOException {

        logger.info("Starting PDF unlock process for file: {}", file.getOriginalFilename());

        // Validate extension
        if (!FileUtils.isValidPdf(file)) {
            logger.error("Invalid PDF found for file: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("Invalid PDF found for file: " + file.getOriginalFilename());
        }
        // Validate is pdf password protected
        if (!FileUtils.isPasswordProtected(file)) {
            logger.error("PDF is not password protected for file: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("PDF is not password protected for file: " + file.getOriginalFilename());
        }
        // Validate input
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        PDDocument document = null;
        ByteArrayOutputStream outputStream = null;

        try {
            // Load the PDF document with password
            document = PDDocument.load(file.getInputStream(), password);

            logger.info("PDF loaded successfully with provided password");

            // Check if document is actually encrypted
            if (!document.isEncrypted()) {
                logger.info("PDF is not encrypted, returning original file");
                return file.getBytes();
            }

            // Log current permissions for debugging
            if (document.getCurrentAccessPermission() != null) {
                boolean canModify = document.getCurrentAccessPermission().canModify();
                boolean canExtract = document.getCurrentAccessPermission().canExtractContent();
                logger.info("PDF permissions - canModify: {}, canExtract: {}", canModify, canExtract);
            }

            // Remove all security restrictions
            document.setAllSecurityToBeRemoved(true);

            // Save the unlocked PDF to byte array
            outputStream = new ByteArrayOutputStream();
            document.save(outputStream);

            byte[] unlockedPdfBytes = outputStream.toByteArray();
            logger.info("PDF successfully unlocked, output size: {} bytes", unlockedPdfBytes.length);

            return unlockedPdfBytes;

        } catch (IOException e) {
            // Check if the error is due to incorrect password
            if (e.getMessage() != null &&
                    (e.getMessage().contains("password") ||
                            e.getMessage().contains("Cannot decrypt PDF") ||
                            e.getMessage().contains("Bad user password"))) {
                logger.error("Invalid password provided for PDF: {}", file.getOriginalFilename());
                throw new InvalidPasswordException("Invalid password provided for PDF");
            }

            // Re-throw other IO exceptions
            logger.error("Error processing PDF file: {}", e.getMessage());
            throw new IOException("Error processing PDF file: " + e.getMessage(), e);

        } finally {
            // Clean up resources
            closeOutputStream(document, outputStream);
        }
    }

    /**
     * Locks an unlocked PDF with standard permissions (allow printing and copying)
     * @param file MultipartFile containing the unlocked PDF
     * @param password Password to open and modify the PDF
     * @return byte array of the locked PDF
     */
    public byte[] lockUnlockedPdfStandard(MultipartFile file, String password)
            throws IllegalStateException, IOException {

        AccessPermission permissions = new AccessPermission();
        permissions.setCanPrint(true);
        permissions.setCanExtractContent(true);
        permissions.setCanModify(false);
        permissions.setCanModifyAnnotations(false);
        permissions.setCanFillInForm(true);
        permissions.setCanExtractForAccessibility(true);

        return lockUnlockedPdf(file, password, permissions);
    }

    /**
     * Locks an unlocked PDF with password protection
     * @param file MultipartFile containing the unlocked PDF
     * @param password Password required to open the PDF (also used as owner password)
     * @param permissions AccessPermission object defining what users can do
     * @return byte array of the locked PDF
     * @throws IOException if there's an error processing the PDF
     * @throws IllegalStateException if the PDF is already password protected
     */
    public byte[] lockUnlockedPdf(MultipartFile file, String password,
                                  AccessPermission permissions) throws IOException, IllegalStateException {

        logger.debug("Starting PDF lock process for unlocked file: {}", file.getOriginalFilename());

        // Validate input
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        PDDocument document = null;
        ByteArrayOutputStream outputStream = null;

        try {
            // Load the unlocked PDF document
            document = PDDocument.load(file.getInputStream());

            logger.debug("Unlocked PDF loaded successfully, applying security settings");

            // Verify it's truly unlocked
            if (document.isEncrypted()) {
                throw new IllegalStateException("PDF appears to be encrypted despite initial check");
            }

            // Create protection policy (use same password for both user and owner)
            StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, permissions);

            // Set encryption key length (128-bit AES)
            policy.setEncryptionKeyLength(128);

            // Set additional security options
            policy.setPreferAES(true); // Use AES encryption

            // Apply the protection policy
            document.protect(policy);

            // Save the locked PDF to byte array
            outputStream = new ByteArrayOutputStream();
            document.save(outputStream);

            byte[] lockedPdfBytes = outputStream.toByteArray();
            logger.debug("PDF successfully locked, output size: {} bytes", lockedPdfBytes.length);

            return lockedPdfBytes;

        } finally {
            // Clean up resources
            closeOutputStream(document, outputStream);
        }
    }

    private void closeOutputStream(PDDocument document, ByteArrayOutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                logger.error("Error closing output stream: {}", e.getMessage());
            }
        }

        if (document != null) {
            try {
                document.close();
            } catch (IOException e) {
                logger.error("Error closing PDF document: {}", e.getMessage());
            }
        }
    }

    /**
     * Converts a MultipartFile PDF to Base64 string
     * @param file the MultipartFile containing PDF data
     * @return Base64 encoded string of the PDF
     * @throws IOException if file reading fails
     * @throws IllegalArgumentException if file is null or empty
     */
    @Override

    public String convertPdfToBase64(MultipartFile file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            logger.debug("Converting PDF file '{}' to Base64, size: {} bytes",
                    file.getOriginalFilename(), file.getSize());

            // Get the file bytes
            byte[] fileBytes = file.getBytes();

            // Encode to Base64
            String base64String = Base64.getEncoder().encodeToString(fileBytes);

            logger.debug("Successfully converted PDF to Base64. Original size: {} bytes, Base64 length: {} characters",
                    fileBytes.length, base64String.length());

            return base64String;

        } catch (IOException e) {
            logger.error("Failed to read file '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw new IOException("Failed to read PDF file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while converting PDF '{}' to Base64: {}",
                    file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Unexpected error during PDF conversion", e);
        }
    }

    /**
     * Converts Base64 string to PDF bytes
     * @param base64String the Base64 encoded PDF string
     * @return byte array containing PDF data
     * @throws IllegalArgumentException if base64String is null, empty, or invalid
     */
    public byte[] convertBase64ToPdf(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            throw new IllegalArgumentException("Base64 string cannot be null or empty");
        }

        try {
            logger.debug("Converting Base64 string to PDF bytes. Base64 length: {} characters", base64String.length());

            // Decode Base64 string to bytes
            byte[] pdfBytes = Base64.getDecoder().decode(base64String.trim());

            logger.debug("Successfully converted Base64 to PDF bytes. PDF size: {} bytes", pdfBytes.length);

            return pdfBytes;

        } catch (IllegalArgumentException e) {
            logger.error("Invalid Base64 string format: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Base64 format: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while converting Base64 to PDF: {}", e.getMessage());
            throw new RuntimeException("Unexpected error during Base64 to PDF conversion", e);
        }
    }


}

