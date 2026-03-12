package com.interview.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeParserService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParserService.class);

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new IllegalArgumentException("No filename");

        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return extractFromPdf(file.getInputStream());
        } else if (lower.endsWith(".docx")) {
            return extractFromDocx(file.getInputStream());
        } else if (lower.endsWith(".txt")) {
            return new String(file.getBytes());
        } else {
            throw new IllegalArgumentException("Unsupported file type. Use PDF, DOCX, or TXT.");
        }
    }

    private String extractFromPdf(InputStream is) throws IOException {
        try (PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.debug("Extracted {} chars from PDF", text.length());
            return text.trim();
        }
    }

    private String extractFromDocx(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            log.debug("Extracted {} chars from DOCX", text.length());
            return text.trim();
        }
    }
}
