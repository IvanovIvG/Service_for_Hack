package ru.ivanov.service_back.service;

import lombok.Data;

/**
 * @author Ivan Ivanov
 **/
public record ProcessResult(String originalFileName, int linesProcessed) {
}
