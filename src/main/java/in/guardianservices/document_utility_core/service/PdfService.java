package in.guardianservices.document_utility_core.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PdfService {

    byte[] unlockPdf(MultipartFile file, String password) throws IOException;

    byte[] lockUnlockedPdfStandard(MultipartFile file, String password) throws IOException;
}
