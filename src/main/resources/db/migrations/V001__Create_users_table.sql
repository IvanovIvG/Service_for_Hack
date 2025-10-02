CREATE TABLE flights
(
    flight_id       SERIAL PRIMARY KEY,
    registration_id bigint not null,
    date            DATE   NOT NULL,
    time_start      TIME,
    time_end        TIME,
    region_name     TEXT,
    lat             FLOAT8,
    lon             FLOAT8,
    flight_type     VARCHAR(20),
    purpose         TEXT,
    main_reg_number VARCHAR(200)
);