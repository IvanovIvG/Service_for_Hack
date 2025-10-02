package ru.ivanov.service_back.repositoriy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ivanov.service_back.model.FlightData;

/**
 * @author Ivan Ivanov
 **/
@Repository
public interface ExcelDataRepository extends JpaRepository<FlightData, Long> {
}
