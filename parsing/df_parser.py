import re
import pandas as pd
import os

# Словарь площадей регионов (пример — подставь актуальные данные)
REGION_AREAS = {
    'Санкт-Петербургский': 84500,       # Ленинградская область + Санкт-Петербург (≈ 84.5 тыс. км²)
    'Ростовский': 100800,               # Ростовская область
    'Новосибирский': 178200,            # Новосибирская область
    'Екатеринбургский': 194800,         # Свердловская область
    'Московский': 44300,                # Московская область + Москва (≈ 44.3 тыс. км²)
    'Красноярский': 2366797,            # Красноярский край (огромный!)
    'Тюменский': 1435200,               # Тюменская область + ХМАО + ЯНАО
    'Самарский': 53600,                 # Самарская область
    'Калининградский': 15100,           # Калининградская область
    'Хабаровский': 788600,              # Хабаровский край
    'Магаданский': 462500,              # Магаданская область
    'Якутский': 3083523,                # Республика Саха (Якутия) — самый большой субъект РФ!
    'Иркутский': 774800,                # Иркутская область
    'Симферопольский': 26100            # Республика Крым + Севастополь (≈ 26.1 тыс. км²)
}


def parse_coord(coord_str):
    """
    Парсит строку координат вида "440846N0430829E" → (lat, lon) десятичные градусы
    """
    if not coord_str or len(coord_str) < 14:
        return None, None
    try:
        # Широта: первые 7 символов, например "440846N"
        lat_part = coord_str[:7]
        lat_deg = int(lat_part[:2])
        lat_min = int(lat_part[2:4])
        lat_sec = int(lat_part[4:6])
        lat_dir = lat_part[6]
        lat = lat_deg + lat_min /60 + lat_sec /3600
        if lat_dir == 'S':
            lat = -lat

        # Долгота: оставшиеся символы, например "0430829E"
        lon_part = coord_str[7:]
        # Может начинаться с 0, например "043..."
        lon_deg = int(lon_part[:3])  # первые 3 цифры — градусы
        lon_min = int(lon_part[3:5])
        lon_sec = int(lon_part[5:7])
        lon_dir = lon_part[7] if len(lon_part) > 7 else ''
        lon = lon_deg + lon_min /60 + lon_sec /3600
        if lon_dir == 'W':
            lon = -lon

        return round(lat, 6), round(lon, 6)
    except Exception:
        return None, None

def parse_time_to_minutes(t):
    """
    Преобразует время вида "0600" → 360 минут с полуночи
    """
    if not t or len(t) < 4:
        return None
    try:
        hour = int(t[:2])
        minute = int(t[2:4])
        return hour * 60 + minute
    except Exception:
        return None

def get_period_of_day(hour):
    """
    Классифицирует час: утро, день, вечер, ночь
    """
    if 6 <= hour < 12:
        return 'утро'
    elif 12 <= hour < 18:
        return 'день'
    elif 18 <= hour < 24:
        return 'вечер'
    else:
        return 'ночь'

def extract_from_shr(shr_text):
    """
    Извлекает данные из строки SHR
    """
    data = {}
    if not isinstance(shr_text, str):
        return data

    # Ищем DOF, OPR, TYP, STS, SID, REG, RMK
    patterns = {
        'DOF': r'DOF/(\S+)',
        'OPR': r'OPR/([^\s]+(?:\s+[^\s]+)*)',
        'TYP': r'TYP/(\S+)',
        'STS': r'STS/(\S+)',
        'SID': r'SID/(\S+)',
        'REG': r'REG/([^\s]+)',
        'RMK': r'RMK/([^\n]*)'
    }

    for key, pattern in patterns.items():
        match = re.search(pattern, shr_text)
        data[key] = match.group(1) if match else None

    # Извлекаем REG, если несколько через запятую
    if data.get('REG'):
        regs = re.split(r'[, ]+', data['REG'])
        data['REG'] = [r.strip() for r in regs if r.strip()]

    # Извлекаем цель из RMK (по ключевым словам)
    purpose_keywords = [
        'МОНИТОРИНГ ПАВОДКООПАСНЫХ',
        'ИНСПЕКЦИЯ',
        'СЪЁМКА',
        'ПАТРУЛИРОВАНИЕ',
        'ОБСЛЕДОВАНИЕ',
        'ДОСТАВКА'
    ]
    rmk = data.get('RMK', '')
    if isinstance(rmk, str):
        for kw in purpose_keywords:
            if kw in rmk:
                data['purpose'] = kw
                break

    return data

def extract_from_dep(dep_text):
    """
    Извлекает данные из строки DEP
    """
    data = {}
    if not isinstance(dep_text, str):
        return data

    lines = dep_text.split('\n')
    for line in lines:
        line = line.strip().lstrip('-')
        if ' ' in line:
            parts = line.split(' ', 1)
            key, val = parts[0], parts[1]
            data[key] = val
        elif '/' in line:
            # Например: "ADEPZ/440846N0430829E"
            pass

    return data

def extract_from_arr(arr_text):
    """
    Извлекает данные из строки ARR
    """
    data = {}
    if not isinstance(arr_text, str):
        return data

    lines = arr_text.split('\n')
    for line in lines:
        line = line.strip().lstrip('-')
        if ' ' in line:
            parts = line.split(' ', 1)
            key, val = parts[0], parts[1]
            data[key] = val

    return data

def parse_flight_row(row):
    """
    Парсит одну строку DataFrame и возвращает словарь с извлечёнными данными
    """
    shr = row['SHR']
    dep = row['DEP']
    arr = row['ARR']
    center = row['Центр ЕС ОрВД']

    # Извлечение из SHR
    shr_data = extract_from_shr(shr)
    # Извлечение из DEP
    dep_data = extract_from_dep(dep)
    # Извлечение из ARR
    arr_data = extract_from_arr(arr)

    # Основные данные
    flight_id = shr_data.get('SID') or dep_data.get('SID') or arr_data.get('SID')
    dof = shr_data.get('DOF') or dep_data.get('ADD') or arr_data.get('ADA')
    atd = dep_data.get('ATD')
    ata = arr_data.get('ATA')
    adepz = dep_data.get('ADEPZ')
    adarrz = arr_data.get('ADARRZ')

    # Парсим дату
    date = None
    if dof:
        try:
            date = pd.to_datetime(dof, format='%y%m%d')
        except:
            pass

    # Парсим время
    start_minutes = parse_time_to_minutes(atd)
    end_minutes = parse_time_to_minutes(ata)
    duration = None
    if start_minutes is not None and end_minutes is not None:
        duration = end_minutes - start_minutes
        if duration < 0:
            duration += 24 *60  # если пересекает полночь

    hour_start = start_minutes // 60 if start_minutes else None
    period = get_period_of_day(hour_start) if hour_start is not None else None

    # Координаты
    lat, lon = None, None
    if adepz:
        lat, lon = parse_coord(adepz)

    # Оператор, тип, цель
    operator = shr_data.get('OPR')
    flight_type = shr_data.get('STS') or shr_data.get('TYP')
    purpose = shr_data.get('purpose')

    # Регистрационные номера - внёс корректировку
    reg_numbers = shr_data.get('REG', [])

    # Извлекаем первый регистрационный номер
    main_reg = None
    if isinstance(reg_numbers, list) and len(reg_numbers) > 0:
        if len(reg_numbers) > 1:
            main_reg = reg_numbers[1]  # ← Второй элемент, если больше одного
        else:
            main_reg = reg_numbers[0]  # ← Иначе — первый (и единственный)

    # Площадь региона
    area = REGION_AREAS.get(center, None)

    return {
        'flight_id': flight_id,
        'date': date,
        'time_start': atd,
        'time_end': ata,
        'duration_minutes': duration,
        'hour_start': hour_start,
        'period_of_day': period,
        'region_name': center,
        'area_km2': area,
        'lat': lat,
        'lon': lon,
        'operator': operator,
        'flight_type': flight_type,
        'purpose': purpose,
        'bvs_reg_numbers': reg_numbers,
        'main_reg_number': main_reg
    }

## Сюда вместо df_dict['df_2025'] можно вписать любой другой excel, csv файл
## пример: df = pd.read_excel('path'), наверхну код можно будет закоментить тогда
## путь к файлу
script_dir = os.path.dirname(os.path.abspath(__file__))
file_path = os.path.join(script_dir, 'flights.xlsx')
df = pd.read_excel(file_path)
parsed_data = df.apply(parse_flight_row, axis=1, result_type='expand')

df_enriched = pd.concat([df, parsed_data], axis=1)

df_enriched.drop(['SHR', 'DEP', 'ARR', 'bvs_reg_numbers', 'Центр ЕС ОрВД', 'operator', 'period_of_day', 'duration_minutes', 'hour_start', 'area_km2'], axis = 1, inplace=True)
df_enriched['time_start'] = pd.to_datetime(df_enriched['time_start'], format='%H%M').dt.time
df_enriched['time_end'] = pd.to_datetime(df_enriched['time_end'], format='%H%M').dt.time
df_enriched['flight_id'] = df_enriched['flight_id'].str.replace(')','')

file_path = os.path.join(script_dir, 'flights_parsed.xlsx')
df_enriched.to_excel(file_path, index=False)