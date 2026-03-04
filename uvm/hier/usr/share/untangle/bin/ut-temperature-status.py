#!/usr/bin/python3 -u
# -*- coding: utf-8 -*-
import uvm


import re


def extract_temperatures_to_json(systemManager):
    extracted_readings = []
    sensor_output = systemManager.getDeviceTemperatureInfo()

    # Regex patterns for Package id and Core lines
    # They capture the ID and the numerical temperature value (float),
    # ignoring any characters after the number.
    package_id_pattern = re.compile(r'Package id (\d+):\s*\+?([\d.]+)')
    core_pattern = re.compile(r'Core (\d+):\s*\+?([\d.]+)')

    # Iterate through each line of the data
    for line in sensor_output.splitlines():
        stripped_line = line.strip()

        # Check for Package id match
        package_match = package_id_pattern.search(stripped_line)
        if package_match:
            try:
                # For Package, the type is just "Overall CPU", id is omitted
                extracted_readings.append({
                    'type': 'Overall CPU',
                    'temperature': float(package_match.group(2))
                })
            except ValueError:
                pass # Skip if conversion fails
            continue

        # Check for Core match
        core_match = core_pattern.search(stripped_line)
        if core_match:
            try:
                # For Core, the type includes the core ID (e.g., "Core 0", "Core 1")
                extracted_readings.append({
                    'type': f"Core {core_match.group(1)}",
                    'temperature': float(core_match.group(2))
                })
            except ValueError:
                pass # Skip if conversion fails
            continue
    
    # Return the extracted data in JSON format
    return extracted_readings

def get_exceeded_temperatures(extracted_data_list, threshold):
    """
    Filters a list of temperature readings to find items exceeding a given threshold.
    """
    exceeded_items = []
    for item in extracted_data_list:
        if item.get('temperature') is not None and item['temperature'] > threshold:
            exceeded_items.append(item)
    return exceeded_items


# Extract data and get JSON output

uvm = uvm.Uvm().getUvmContext()
systemManager = uvm.systemManager()
extracted_readings  = extract_temperatures_to_json(systemManager)

temperature_threshold = systemManager.getSettings().get("thresholdTemperature", 105.0)

# 3. Get items that exceeded the threshold
exceeded_temperatures = get_exceeded_temperatures(extracted_readings, temperature_threshold)
#json_output = json.dumps(exceeded_temperatures, indent=2)
if exceeded_temperatures:
    formatted_items = []
    #print(f"The following items have exceeded the temperature threshold of {temperature_threshold} :")
    for item in exceeded_temperatures:
        # Format the output sentence for each exceeding item
        formatted_items.append(f"{item['type']}: {item['temperature']}°C")
    output_string = ", ".join(formatted_items)
    final_output = f"The following items have exceeded the temperature threshold of {temperature_threshold}°C: {output_string}."
    systemManager.logCriticalTemperature(final_output)
else:
    # If no items exceeded the threshold
    pass
