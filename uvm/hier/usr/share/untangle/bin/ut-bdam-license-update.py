#!/usr/bin/python3 -u

import subprocess
import os
import os.path
import datetime

bdamserver_log = open("/var/log/bdamserver.log", "a")

def log(message, log_type="INFO"):
    """Log to the log file with a timestamp and PID."""
    try:
        # Get the current timestamp
        timestamp = datetime.datetime.now().strftime("%a %b %d %H:%M:%S %Y")
        # Get the PID (Process ID)
        pid = os.getpid()
        # Construct the full log message
        full_message = f"{timestamp} [{pid}] {log_type}: {message}"
        
        # Write to the log file
        bdamserver_log.write(full_message + "\n")
        bdamserver_log.flush()
    except Exception as e:
        pass

def manage_service(service_name, command):
    """Manage the bdam service."""
    try:
        # Run the systemctl command with the provided command and service name
        result = subprocess.run(
            ['systemctl', command, service_name],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        if command == 'status':
            if result.returncode == 0 and "active (running)" in result.stdout:
                log(f"The service {service_name} is running.", f"INFO")
                return True
            else:
                log(f"The service {service_name} is not running.", f"ERROR")
                return False

        elif command == 'restart':
            if result.returncode == 0:
                log(f"The service {service_name} has been restarted.", f"INFO")
                return True
            else:
                log(f"Failed to restart the service {service_name}.", f"ERROR")
                return False
        
        # For any other commands
        log(f"Invalid command: {command} for service {service_name}.", f"ERROR")
        return False

    except Exception as e:
        log(f"Error while managing service {service_name}: {str(e)}", f"ERROR")
        return False

def get_license_from_curl():
    """Get the latest license version."""
    try:
        # Run curl command and capture both stdout and stderr
        result = subprocess.run(
            ['curl', '-s', '-w', '%{http_code}', 'https://downloads.edge.arista.com/bdam/config'],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        # Check the HTTP status code
        http_code = result.stdout[-3:]  
        if http_code != '200':
            log(f"Request failed with HTTP status code {http_code}. Please check the request.", f"ERROR")
            return None

        # If response is valid, return it
        response = result.stdout[:-3].strip()  # Exclude the HTTP code from stdout
        if response:
            log(f"Latest License serial number {response}.", f"INFO")
            return response
        else:
            log(f"Not able to retriew License serial number .", f"ERROR")
            return None

    except subprocess.CalledProcessError as e:
        log(f"Exception occured while getting the latest license .", f"ERROR")
        return None

def update_license_in_file(file_path, new_license):
    # Read the content of the file
    try:
        with open(file_path, 'r') as file:
            lines = file.readlines()

        # Check if the LicenseSerial line exists
        for i, line in enumerate(lines):
            if line.startswith('LicenseSerial='):
                current_license = line.strip().split('=')[1]

                if current_license != new_license:
                    lines[i] = f"LicenseSerial={new_license}\n"  # Update the line with the new license
                    log(f"Updated bdamserver.conf with latest License serial number  is {new_license}.", f"INFO")

                break

        # Write the updated content back to the file
        with open(file_path, 'w') as file:
            file.writelines(lines)

    except Exception as e:
        pass


def main():
    file_path = '/etc/bdamserver/bdamserver.conf'
    service_name = 'untangle-bdamserver'
    
    if manage_service(service_name, 'status'):  # Only proceed if the service is running
        # Get the new license serial from the curl request
        new_license = get_license_from_curl()
        
        if new_license:
            # Update the file with the new license if it's different from the current one
            update_license_in_file(file_path, new_license)
            
            # Restart the service after updating the license
            manage_service(service_name, 'restart')

if __name__ == "__main__":
    main()

