package in.guardianservices.document_utility_core.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PdfService {

    byte[] unlockPdf(MultipartFile file, String password) throws IOException;

    byte[] lockUnlockedPdfStandard(MultipartFile file, String password) throws IOException;

    String convertPdfToBase64(MultipartFile file) throws IOException;

    byte[] convertBase64ToPdf(String base64String);

    byte[] compressPdf(MultipartFile file, float quality) throws IOException;

    byte[] mergePdfs(List<MultipartFile> files) throws IOException;

    List<byte[]> splitPdf(MultipartFile file, String pageRanges) throws IOException;

    byte[] createZipFromPdfs(List<byte[]> splitPdfs) throws IOException;

    List<byte[]> convertPdfToImages(MultipartFile file, String format, int dpi) throws IOException;

    byte[] createZipFromImages(List<byte[]> images, String format) throws IOException;

    List<String> convert(String filePath) throws IOException;
}
