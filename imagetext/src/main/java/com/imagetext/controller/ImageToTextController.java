package com.imagetext.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;


@RestController
@RequestMapping("/api")
public class ImageToTextController {
	

 @PostMapping("/convert")
 public ResponseEntity<String> convertImageToText(@RequestParam("file") MultipartFile file) {
     if (file.isEmpty()) {
         return ResponseEntity.badRequest().body("Please upload an image file");
     }

     try {
         String extractedText = extractTextFromImage(file);
         String correctedText = improveGrammarWithLanguageTool(extractedText);
         return ResponseEntity.ok(correctedText);
     } catch (Exception e) {
         e.printStackTrace(); // Log the exception
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the image");
     }
 }
 private String extractTextFromImage(MultipartFile file) throws TesseractException {
	    try {
	        // Convert MultipartFile to BufferedImage
	        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));

	        // Initialize Tesseract OCR engine
	        Tesseract tesseract = new Tesseract();
	        tesseract.setDatapath("src/main/resources/testdata");

	        // Recognize text from the image
	        String extractedText = tesseract.doOCR(bufferedImage);

	        return extractedText;
	    } catch (IOException e) {
	        e.printStackTrace();
	        throw new TesseractException("Error extracting text from image", e);
	    }
	}


 private String improveGrammarWithLanguageTool(String text) {
	    JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());

	    List<RuleMatch> matches;
	    try {
	        matches = langTool.check(text);
	    } catch (IOException e) {
	        e.printStackTrace();
	        return "Error checking grammar";
	    }

	    if (matches.isEmpty()) {
	        return text; // Return original text if no matches found
	    }

	    StringBuilder correctedText = new StringBuilder(text);
	    for (int i = matches.size() - 1; i >= 0; i--) {
	        RuleMatch match = matches.get(i);
	        if (!match.getSuggestedReplacements().isEmpty() && isSuggestionSimilar(match.getFromPos(), match.getToPos(), match.getSuggestedReplacements().get(0), text)) {
	            correctedText.replace(match.getFromPos(), match.getToPos(), match.getSuggestedReplacements().get(0));
	        }
	    }

	    return correctedText.toString();
	}

	private boolean isSuggestionSimilar(int fromPos, int toPos, String replacement, String originalText) {
	    double similarityThreshold = 0.5; // Adjust this threshold as needed
	    String originalWord = originalText.substring(fromPos, toPos);
	    int maxLength = Math.max(originalWord.length(), replacement.length());
	    
	    LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
	    int distance = levenshteinDistance.apply(originalWord.toLowerCase(), replacement.toLowerCase());
	    double similarity = 1.0 - ((double) distance / maxLength);
	    
	    return similarity >= similarityThreshold;
	}




}
