package in.guardianservices.document_utility_core.service.impl;

import in.guardianservices.document_utility_core.exception.InvalidPasswordException;
import in.guardianservices.document_utility_core.service.PdfService;
import in.guardianservices.document_utility_core.utils.FileUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    /**
     * Compresses PDF by reducing image quality and removing unnecessary elements
     * @param file the PDF file to compress
     * @param quality compression quality (0.1 to 1.0)
     * @return compressed PDF as byte array
     */
    @Override
    public byte[] compressPdf(MultipartFile file, float quality) throws IOException {
        logger.debug("Starting PDF compression for file: {}, quality: {}", file.getOriginalFilename(), quality);

        if (quality < 0.1f || quality > 1.0f) {
            throw new IllegalArgumentException("Quality must be between 0.1 and 1.0");
        }

        try (PDDocument document = PDDocument.load(file.getInputStream());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            long originalSize = file.getSize();

            // Compress images in the PDF
            compressImagesInDocument(document, quality);

            // Remove unused resources
            document.getDocumentCatalog().getPages().forEach(page -> {
                try {
                    // Remove annotations if they exist
                    if (page.getAnnotations() != null) {
                        page.getAnnotations().clear();
                    }
                } catch (Exception e) {
                    logger.warn("Failed to remove annotations from page: {}", e.getMessage());
                }
            });

            document.save(outputStream);
            byte[] compressedBytes = outputStream.toByteArray();

            logger.debug("PDF compression completed. Original size: {} bytes, Compressed size: {} bytes, Reduction: {}%",
                    originalSize, compressedBytes.length,
                    ((originalSize - compressedBytes.length) * 100.0 / originalSize));

            return compressedBytes;
        }
    }

    private void compressImagesInDocument(PDDocument document, float quality) throws IOException {
        logger.debug("Compressing images in document with quality: {}", quality);

        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) continue;

            for (COSName name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);

                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;

                    // Get original image
                    BufferedImage bufferedImage = image.getImage();
                    if (bufferedImage == null) continue;

                    // Compress and replace
                    byte[] compressedBytes = compressImage(bufferedImage, quality);
                    PDImageXObject newImage = PDImageXObject.createFromByteArray(
                            document, compressedBytes, name.getName());
                    resources.put(name, newImage);
                }
            }
        }
    }

    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        // Convert to RGB if needed (JPEG only supports RGB and Grayscale)
        BufferedImage rgbImage = convertToRGB(image);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("JPEG").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        }
        writer.dispose();

        return output.toByteArray();
    }

    private BufferedImage convertToRGB(BufferedImage original) {
        if (original.getType() == BufferedImage.TYPE_INT_RGB) {
            return original;
        }

        BufferedImage rgbImage = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = rgbImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, original.getWidth(), original.getHeight());
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        return rgbImage;
    }

    /**
     * Merges multiple PDF files into a single PDF
     * @param files list of PDF files to merge
     * @return merged PDF as byte array
     */
    @Override
    public byte[] mergePdfs(List<MultipartFile> files) throws IOException {
        logger.debug("Starting PDF merge operation for {} files", files.size());

        if (files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for merging");
        }

        if (files.size() < 2) {
            throw new IllegalArgumentException("At least 2 files required for merging");
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        List<PDDocument> documents = new ArrayList<>();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Load all documents
            for (MultipartFile file : files) {
                if (!FileUtils.isValidPdf(file)) {
                    throw new IllegalArgumentException("Invalid PDF file: " + file.getOriginalFilename());
                }

                PDDocument document = PDDocument.load(file.getInputStream());
                documents.add(document);
                merger.addSource(file.getInputStream());
            }

            merger.setDestinationStream(outputStream);
            merger.mergeDocuments(null);

            byte[] mergedBytes = outputStream.toByteArray();
            logger.debug("PDF merge completed. Total pages in merged PDF: estimated from {} files", files.size());

            return mergedBytes;

        } finally {
            // Close all documents
            for (PDDocument doc : documents) {
                if (doc != null) {
                    try {
                        doc.close();
                    } catch (IOException e) {
                        logger.warn("Failed to close document: {}", e.getMessage());
                    }
                }
            }
        }
    }

    // PDF SPLIT
    /**
     * Splits PDF into multiple documents based on page ranges
     * @param file the PDF file to split
     * @param pageRanges comma-separated page ranges (e.g., "1-3,5,7-9")
     * @return list of split PDF byte arrays
     */
    public List<byte[]> splitPdf(MultipartFile file, String pageRanges) throws IOException {
        logger.debug("Starting PDF split operation for file: {}, page ranges: {}", file.getOriginalFilename(), pageRanges);

        List<byte[]> splitPdfs = new ArrayList<>();

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            int totalPages = document.getNumberOfPages();
            logger.debug("Total pages in document: {}", totalPages);

            List<PageRange> ranges = parsePageRanges(pageRanges, totalPages);

            for (PageRange range : ranges) {
                try (PDDocument splitDocument = new PDDocument();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    for (int i = range.start; i <= range.end; i++) {
                        PDPage page = document.getPage(i - 1); // PDFBox uses 0-based indexing
                        splitDocument.addPage(page);
                    }

                    splitDocument.save(outputStream);
                    splitPdfs.add(outputStream.toByteArray());

                    logger.debug("Created split PDF for pages {}-{}", range.start, range.end);
                }
            }
        }

        logger.debug("PDF split completed. Created {} split files", splitPdfs.size());
        return splitPdfs;
    }

    private List<PageRange> parsePageRanges(String pageRanges, int totalPages) {
        List<PageRange> ranges = new ArrayList<>();
        String[] rangeParts = pageRanges.split(",");

        for (String part : rangeParts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());

                if (start < 1 || end > totalPages || start > end) {
                    throw new IllegalArgumentException("Invalid page range: " + part);
                }

                ranges.add(new PageRange(start, end));
            } else {
                int page = Integer.parseInt(part);
                if (page < 1 || page > totalPages) {
                    throw new IllegalArgumentException("Invalid page number: " + page);
                }
                ranges.add(new PageRange(page, page));
            }
        }

        return ranges;
    }

    private static class PageRange {
        final int start;
        final int end;

        PageRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    // PDF TO IMAGE CONVERSION
    /**
     * Converts PDF pages to images
     * @param file the PDF file to convert
     * @param format image format (PNG, JPEG, etc.)
     * @param dpi resolution for the images
     * @return list of image byte arrays
     */
    public List<byte[]> convertPdfToImages(MultipartFile file, String format, int dpi) throws IOException {
        logger.debug("Starting PDF to image conversion for file: {}, format: {}, DPI: {}",
                file.getOriginalFilename(), format, dpi);

        if (dpi < 72 || dpi > 600) {
            throw new IllegalArgumentException("DPI must be between 72 and 600");
        }

        List<byte[]> images = new ArrayList<>();

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);

                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    ImageIOUtil.writeImage(image, format.toLowerCase(), outputStream, dpi);
                    images.add(outputStream.toByteArray());

                    logger.debug("Converted page {} to {} image", i + 1, format);
                }
            }
        }

        logger.debug("PDF to image conversion completed. Generated {} images", images.size());
        return images;
    }

    // UTILITY METHODS FOR ZIP CREATION

    /**
     * Creates a ZIP file containing multiple PDF files
     * @param pdfFiles list of PDF byte arrays
     * @return ZIP file as byte array
     */
    public byte[] createZipFromPdfs(List<byte[]> pdfFiles) throws IOException {
        logger.debug("Creating ZIP file from {} PDF files", pdfFiles.size());

        try (ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(zipOutputStream)) {

            for (int i = 0; i < pdfFiles.size(); i++) {
                String filename = String.format("split_page_%d.pdf", i + 1);
                ZipEntry entry = new ZipEntry(filename);
                zip.putNextEntry(entry);
                zip.write(pdfFiles.get(i));
                zip.closeEntry();
            }

            zip.finish();
            return zipOutputStream.toByteArray();
        }
    }

    /**
     * Creates a ZIP file containing multiple image files
     * @param images list of image byte arrays
     * @param format image format extension
     * @return ZIP file as byte array
     */
    public byte[] createZipFromImages(List<byte[]> images, String format) throws IOException {
        logger.debug("Creating ZIP file from {} image files", images.size());

        try (ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(zipOutputStream)) {

            for (int i = 0; i < images.size(); i++) {
                String filename = String.format("page_%d.%s", i + 1, format.toLowerCase());
                ZipEntry entry = new ZipEntry(filename);
                zip.putNextEntry(entry);
                zip.write(images.get(i));
                zip.closeEntry();
            }

            zip.finish();
            return zipOutputStream.toByteArray();
        }
    }

}

