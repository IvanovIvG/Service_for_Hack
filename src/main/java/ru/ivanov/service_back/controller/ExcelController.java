package ru.ivanov.service_back.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.ivanov.service_back.dto.ExcelParseAndUploadResponse;
import ru.ivanov.service_back.service.ExcelProcessingService;
import ru.ivanov.service_back.service.ProcessResult;

@RestController
public class ExcelController {

    private final ExcelProcessingService excelProcessingService;

    @Autowired
    public ExcelController(ExcelProcessingService excelProcessingService) {
        this.excelProcessingService = excelProcessingService;
    }

    @PostMapping("/upload-and-parse")
    public ResponseEntity<?> uploadAndParse(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }

        if (isNotExcel(file)) {
            return ResponseEntity.badRequest().body("Only Excel files (.xlsx) are allowed");
        }

        try {
            ProcessResult result = excelProcessingService.parseAndSave(file);

            return ResponseEntity.ok()
                    .body(new ExcelParseAndUploadResponse(
                            "File processed successfully",
                            result.originalFileName(),
                            result.linesProcessed()
                    ));

        }
        catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error processing file: " + e.getMessage());
        }
    }

    private boolean isNotExcel(MultipartFile file){
        return !file.getOriginalFilename().endsWith(".xlsx");
    }
}