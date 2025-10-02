package ru.ivanov.service_back.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @author Ivan Ivanov
 **/
@Entity
@Table(name = "flights")
@Data
public class FlightData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id")
    private Long flightId;

    @Column(name = "registration_id")
    @NotNull
    private Long registrationId;

    @Column(name = "date")
    @NotNull
    private LocalDate date;

    @Column(name = "time_start")
    private LocalTime timeStart;

    @Column(name = "time_end")
    private LocalTime timeEnd;

    @Column(name = "region_name")
    private String region;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "flight_type")
    @Size(max = 20)
    private String flightType;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "main_reg_number")
    @Size(max = 200)
    private String mainRegNumber;
}
