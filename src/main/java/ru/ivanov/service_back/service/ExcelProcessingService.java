package ru.ivanov.service_back.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.ivanov.service_back.model.FlightData;
import ru.ivanov.service_back.repositoriy.ExcelDataRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Ivan Ivanov
 **/
@Service
public class ExcelProcessingService {

    private final ExcelDataRepository repository;
    private final ExcelToModelParser excelToModelParser;

    private final Path flightsPath;
    private final Path flightsParsedPath;
    private final String pythonScriptPath;

    @Autowired
    public ExcelProcessingService(ExcelDataRepository repository, ExcelToModelParser excelToModelParser) {
        this.repository = repository;
        this.excelToModelParser = excelToModelParser;

        this.flightsPath = Paths.get("parsing", "flights.xlsx");
        this.flightsParsedPath = Paths.get("parsing", "flights_parsed.xlsx");
        this.pythonScriptPath = "parsing/df_parser.py";
    }


    public ProcessResult parseAndSave(MultipartFile file){
        try {
            storeOriginalFile(file);
            parseWithPython();
            List<FlightData> dataList = parseToModel();
            List<FlightData> savedData = saveToDatabase(dataList);
            return new ProcessResult(file.getOriginalFilename(), savedData.size());
        } finally {
            deleteFlightsTable();
            deleteFlightsParsedTable();
        }
    }

    private void storeOriginalFile(MultipartFile file) {
        try {
            Files.copy(file.getInputStream(), flightsPath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
        }
    }

    private void parseWithPython() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "py",
                    pythonScriptPath
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);

            if (!finished) {
                process.destroy();
                throw new RuntimeException("Python script timeout");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Python script failed with exit code: " + exitCode);
            }

            if (!Files.exists(flightsPath)) {
                throw new RuntimeException("Python script did not create output file");
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error executing Python script: " + e.getMessage());
        }
    }

    private List<FlightData> parseToModel() {
        return excelToModelParser.parseExcelToModel(flightsParsedPath);
    }

    private List<FlightData> saveToDatabase(List<FlightData> dataList) {
        try {
            return repository.saveAll(dataList);
        } catch (Exception e) {
            throw new RuntimeException("Error saving to database: " + e.getMessage());
        }
    }

    private void deleteFlightsTable() {
        try {
            if(Files.exists(flightsPath)) {
                Files.delete(flightsPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteFlightsParsedTable() {
        try {
            if(Files.exists(flightsParsedPath)) {
                Files.delete(flightsParsedPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}