package ru.ivanov.service_back.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ivanov.service_back.model.FlightData;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Ivanov
 **/
class ExcelToModelParserTest {
    private ExcelToModelParser parser;

    @BeforeEach
    void setUp() {
        parser = new ExcelToModelParser();
    }

    @Test
    void parseExcelToModel_ValidFile_ReturnsFlightDataList() throws IOException {
        Path testFile = Paths.get("src/test/resources/testExcel/flights_parsed.xlsx");


        List<FlightData> result = parser.parseExcelToModel(testFile);

        for (FlightData data: result){
            System.out.println(data);
        }
        assertNotNull(result);
        assertEquals(3, result.size());

    }

}