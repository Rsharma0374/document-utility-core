package in.guardianservices.document_utility_core.controller;

import in.guardianservices.document_utility_core.exception.InvalidPasswordException;
import in.guardianservices.document_utility_core.service.DocService;
import in.guardianservices.document_utility_core.service.PdfService;
import in.guardianservices.document_utility_core.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

}
