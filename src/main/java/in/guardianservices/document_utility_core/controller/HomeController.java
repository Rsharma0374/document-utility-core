package in.guardianservices.document_utility_core.controller;

import in.guardianservices.document_utility_core.exception.InvalidPasswordException;
import in.guardianservices.document_utility_core.service.DocService;
import in.guardianservices.document_utility_core.service.PdfService;
import in.guardianservices.document_utility_core.utils.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/doc-service")
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private PdfService pdfService;

    @Autowired
    private DocService docService;

    @GetMapping("/welcome")
    public String welcome() {
        return "+-------------------------------------+\n" +
                "|  Welcome to Document Utility!       |\n" +
                "+-------------------------------------+\n";
    }

    @PostMapping("/pdf/unlock")
    public ResponseEntity<?> unlockPdf(@RequestParam("file") MultipartFile file,
                                       @RequestParam("password") String password) {

        logger.info("Unlocking PDF file started");

        try {

            byte[] unlockedPdf = pdfService.unlockPdf(file, password);
            String originalFilename = file.getOriginalFilename();
            String downloadFilename = originalFilename != null ?
                    "unlocked_" + originalFilename : "unlocked.pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadFilename + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(unlockedPdf);
        } catch (InvalidPasswordException e) {
            logger.error("Invalid password provided: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid PDF password"));
        } catch (Exception e) {
            logger.error("Error while unlocking pdf: ", e);
            String message = e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", null == message ? "Failed to unlock PDF" : message));
        }
    }

    @PostMapping("/pdf/lock")
    public ResponseEntity<?> lockUnlockedPdf(@RequestParam("file") MultipartFile file,
                                             @RequestParam("password") String password) {

        logger.debug("Attempting to lock unlocked PDF: {}", file.getOriginalFilename());

        try {
            // Check if PDF is already locked
            if (FileUtils.isPasswordProtected(file)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "PDF is already password protected"));
            }

            // Validate extension
            if (!FileUtils.isValidPdf(file)) {
                logger.error("Invalid PDF found for file: {}", file.getOriginalFilename());
                throw new IllegalArgumentException("Invalid PDF found for file: " + file.getOriginalFilename());
            }

            byte[] lockedPdf = pdfService.lockUnlockedPdfStandard(file, password);

            String originalFilename = file.getOriginalFilename();
            String downloadFilename = originalFilename != null ?
                    "locked_" + originalFilename : "locked.pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadFilename + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(lockedPdf.length)
                    .body(lockedPdf);

        } catch (IllegalStateException e) {
            logger.error("PDF is already locked: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "PDF is already password protected"));
        } catch (Exception e) {
            logger.error("Error while locking unlocked PDF: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to lock PDF"));
        }
    }

    @PostMapping("/pdf-to-base64")
    public ResponseEntity<?> convertPdfToBase64(@RequestParam("file") MultipartFile file) {

        logger.debug("Attempting to convert pdf to base64: {}", file.getOriginalFilename());

        try {
            // Check if PDF is already locked
            if (FileUtils.isPasswordProtected(file)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "PDF is password protected"));
            }

            // Validate extension
            if (!FileUtils.isValidPdf(file)) {
                logger.error("Invalid PDF found for file: {}", file.getOriginalFilename());
                throw new IllegalArgumentException("Invalid PDF found for file: " + file.getOriginalFilename());
            }

            // Check file size before conversion (example: 10MB limit)
            if (!FileUtils.isFileSizeAcceptable(file, 10)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File size exceeds 10MB limit"));
            }

            String base64 = pdfService.convertPdfToBase64(file);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("base64", base64));

        } catch (IllegalStateException e) {
            logger.error("PDF is invalid: ", e);
            String message = e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", StringUtils.isNotBlank(message) ? message
                            : "PDF is password protected"));
        } catch (Exception e) {
            logger.error("Error while converting PDF to base64: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to convert PDF"));
        }
    }

    @PostMapping("/base64-to-pdf")
    public ResponseEntity<?> convertBase64toPdf(@RequestParam("base64") String base64) {

        logger.debug("Attempting to convert base64 to PDF, base64 length: {}", base64.length());

        try {
            // Validate base64 string
            if (base64 == null || base64.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Base64 string cannot be null or empty"));
            }

            // Remove data URL prefix if present (data:application/pdf;base64,)
            String cleanBase64 = base64;
            if (base64.startsWith("data:")) {
                int commaIndex = base64.indexOf(',');
                if (commaIndex != -1) {
                    cleanBase64 = base64.substring(commaIndex + 1);
                    logger.debug("Removed data URL prefix from base64 string");
                }
            }

            // Convert base64 to PDF bytes
            byte[] pdfBytes = pdfService.convertBase64ToPdf(cleanBase64);

            // Validate if the generated bytes form a valid PDF
            if (!FileUtils.isValidPdfBytes(pdfBytes)) {
                logger.error("Generated bytes do not form a valid PDF");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Provided base64 is not valid"));
            }

            // Generate filename with timestamp
            String filename = "converted_" + System.currentTimeMillis() + ".pdf";

            // Set headers for PDF download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setAccessControlExposeHeaders(Collections.singletonList("Content-Disposition"));
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
            headers.setContentLength(pdfBytes.length);

            logger.debug("Successfully converted base64 to PDF. PDF size: {} bytes", pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid base64 format: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid base64 format: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error while converting base64 to PDF: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to convert base64 to PDF"));
        }
    }


}
