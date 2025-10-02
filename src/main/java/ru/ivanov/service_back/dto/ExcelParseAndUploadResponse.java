package ru.ivanov.service_back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Ivan Ivanov
 **/
@Data
@AllArgsConstructor
public class ExcelParseAndUploadResponse {
    private String message;
    private String originalFileName;
    private int recordsProcessed;
}
