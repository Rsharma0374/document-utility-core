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

import java.util.*;

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
        return """
                +-------------------------------------+
                |  Welcome to Document Utility!       |
                +-------------------------------------+
                """;
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

        logger.info("Attempting to lock unlocked PDF: {}", file.getOriginalFilename());

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

        logger.info("Attempting to convert pdf to base64: {}", file.getOriginalFilename());

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

        logger.info("Attempting to convert base64 to PDF, base64 length: {}", base64.length());

        try {
            // Validate base64 string
            if (base64.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Base64 string cannot be null or empty"));
            }

            // Remove data URL prefix if present (data:application/pdf;base64,)
            String cleanBase64 = base64;
            if (base64.startsWith("data:")) {
                int commaIndex = base64.indexOf(',');
                if (commaIndex != -1) {
                    cleanBase64 = base64.substring(commaIndex + 1);
                    logger.info("Removed data URL prefix from base64 string");
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

            logger.info("Successfully converted base64 to PDF. PDF size: {} bytes", pdfBytes.length);

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


    // PDF COMPRESSION
    @PostMapping("/pdf/compress")
    public ResponseEntity<?> compressPdf(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "quality", defaultValue = "0.8") float quality) {

        logger.info("Attempting to compress PDF: {}, quality: {}", file.getOriginalFilename(), quality);

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File is empty"));
            }

            if (!FileUtils.isValidPdf(file)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid PDF file"));
            }

            // Validate quality parameter
            if (quality < 0.1f || quality > 1.0f) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Quality must be between 0.1 and 1.0"));
            }

            byte[] compressedPdf = pdfService.compressPdf(file, quality);

            // Calculate compression ratio
            long originalSize = file.getSize();
            long compressedSize = compressedPdf.length;
            double compressionRatio = ((double)(originalSize - compressedSize) / originalSize) * 100;

            logger.info("PDF compression successful. Compression ratio: {}", compressionRatio);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=compressed_" + file.getOriginalFilename())
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .header("X-Original-Size", String.valueOf(originalSize))
                    .header("X-Compressed-Size", String.valueOf(compressedSize))
                    .header("X-Compression-Ratio", String.format("%.2f", compressionRatio))
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(compressedPdf);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for PDF compression: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error while compressing PDF: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to compress PDF"));
        }
    }

    // PDF MERGE
    @PostMapping("/pdf/merge")
    public ResponseEntity<?> mergePdfs(@RequestParam("files") List<MultipartFile> files) {

        logger.info("Attempting to merge {} PDF files", files.size());

        try {
            // Validate files
            if (files.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No files provided"));
            }

            if (files.size() < 2) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "At least 2 files required for merging"));
            }

            // Validate each file
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "One or more files are empty"));
                }

                if (!FileUtils.isValidPdf(file)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid PDF file: " + file.getOriginalFilename()));
                }
            }

            byte[] mergedPdf = pdfService.mergePdfs(files);

            logger.info("PDF merge successful. Total merged files: {}", files.size());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=merged_" + System.currentTimeMillis() + ".pdf")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .header("X-Merged-Files-Count", String.valueOf(files.size()))
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(mergedPdf);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for PDF merge: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error while merging PDFs: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to merge PDFs"));
        }
    }

    // PDF SPLIT
    @PostMapping("/pdf/split")
    public ResponseEntity<?> splitPdf(@RequestParam("file") MultipartFile file,
                                      @RequestParam("pages") String pageRanges) {

        logger.info("Attempting to split PDF: {}, page ranges: {}", file.getOriginalFilename(), pageRanges);

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File is empty"));
            }

            if (!FileUtils.isValidPdf(file)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid PDF file"));
            }

            // Validate page ranges
            if (pageRanges == null || pageRanges.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Page ranges cannot be empty"));
            }

            List<byte[]> splitPdfs = pdfService.splitPdf(file, pageRanges);

            if (splitPdfs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No pages found for specified ranges"));
            }

            // Return as ZIP file containing multiple PDFs
            byte[] zipFile = pdfService.createZipFromPdfs(splitPdfs);

            logger.info("PDF split successful. Generated {} split files", splitPdfs.size());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=split_" +
                            Objects.requireNonNull(file.getOriginalFilename()).replace(".pdf", "") + "_pages.zip")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .header("X-Split-Files-Count", String.valueOf(splitPdfs.size()))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipFile);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for PDF split: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error while splitting PDF: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to split PDF"));
        }
    }

    // PDF TO IMAGE CONVERSION
    @PostMapping("/pdf/to-images")
    public ResponseEntity<?> convertPdfToImages(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "format", defaultValue = "PNG") String format,
                                                @RequestParam(value = "dpi", defaultValue = "300") int dpi) {

        logger.info("Attempting to convert PDF to images: {}, format: {}, DPI: {}",
                file.getOriginalFilename(), format, dpi);

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File is empty"));
            }

            if (!FileUtils.isValidPdf(file)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid PDF file"));
            }

            // Validate format
            if (!Arrays.asList("PNG", "JPEG", "JPG", "GIF", "BMP").contains(format.toUpperCase())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Unsupported image format: " + format));
            }

            // Validate DPI
            if (dpi < 72 || dpi > 600) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "DPI must be between 72 and 600"));
            }

            List<byte[]> images = pdfService.convertPdfToImages(file, format, dpi);

            if (images.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No pages found in PDF"));
            }

            byte[] zipFile = pdfService.createZipFromImages(images, format);

            logger.info("PDF to image conversion successful. Generated {} images", images.size());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" +
                            Objects.requireNonNull(file.getOriginalFilename()).replace(".pdf", "") + "_images.zip")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .header("X-Images-Count", String.valueOf(images.size()))
                    .header("X-Image-Format", format.toUpperCase())
                    .header("X-Image-DPI", String.valueOf(dpi))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipFile);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for PDF to image conversion: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error while converting PDF to images: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to convert PDF to images"));
        }
    }

}
