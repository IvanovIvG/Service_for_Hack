package ru.ivanov.service_back.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ru.ivanov.service_back.model.FlightData;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * @author Ivan Ivanov
 **/
@Component
public class ExcelToModelParser {
    public List<FlightData> parseExcelToModel(Path filePath) {
        List<FlightData> dataList = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(fileInputStream))
        {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Пропускаем заголовок
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();

                if (isRowEmpty(currentRow)) {
                    continue;
                }

                FlightData flightData = createFlightDataFromRow(currentRow);
                if (flightData != null) {
                    dataList.add(flightData);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse processed Excel file: " + e.getMessage());
        }
        return dataList;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private FlightData createFlightDataFromRow(Row row) {
        try {
            FlightData data = new FlightData();

            setRegistrationId(data, row.getCell(0));
            setDate(data, row.getCell(1));
            setTimeStart(data, row.getCell(2));
            setTimeEnd(data, row.getCell(3));
            setRegion(data, row.getCell(4));
            setLatitude(data, row.getCell(5));
            setLongitude(data, row.getCell(6));
            setFlightType(data, row.getCell(7));
            setPurpose(data, row.getCell(8));
            setMainRegNumber(data, row.getCell(9));

            return data;

        } catch (Exception e) {
            System.out.println("Ошибка создания FlightData из строки: " + e.getMessage());
            return null;
        }
    }

    private void setRegistrationId(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            try {
                data.setRegistrationId(Long.valueOf(value));
            } catch (NumberFormatException e) {
                System.out.println("Ошибка парсинга registrationId: " + value);
            }
        }
    }

    private void setDate(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value.replace(" ", "T"));
                data.setDate(dateTime.toLocalDate());
            } catch (Exception e) {
                System.out.println("Ошибка парсинга даты: " + value);
            }
        }
    }

    private void setTimeStart(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            try {
                LocalTime timeStart = LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss"));
                data.setTimeStart(timeStart);
            } catch (Exception e) {
                System.out.println("Ошибка парсинга time_start: " + value);
            }
        }
    }

    private void setTimeEnd(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            try {
                LocalTime timeEnd = LocalTime.parse(value, ofPattern("HH:mm:ss"));
                data.setTimeEnd(timeEnd);
            } catch (Exception e) {
                System.out.println("Ошибка парсинга time_end: " + value);
            }
        }
    }

    private void setRegion(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            data.setRegion(value);
        }
    }

    private void setLatitude(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            try {
                data.setLat(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                System.out.println("Ошибка парсинга широты: " + value);
            }
        }
    }

    private void setLongitude(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            try {
                data.setLon(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                System.out.println("Ошибка парсинга долготы: " + value);
            }
        }
    }

    private void setFlightType(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            data.setFlightType(value);
        }
    }

    private void setPurpose(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            data.setPurpose(value);
        }
    }

    private void setMainRegNumber(FlightData data, Cell cell) {
        String value = getStringCellValue(cell);
        if (!value.isEmpty()) {
            data.setMainRegNumber(value);
        } else {
            System.out.println("Предупреждение: mainRegNumber пуст");
        }
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    // Для числовых значений убираем дробную часть если она .0
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((int) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            default:
                return "";
        }
    }
}
