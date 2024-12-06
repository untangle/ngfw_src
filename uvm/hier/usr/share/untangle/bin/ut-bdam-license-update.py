#!/usr/bin/python3 -u

import subprocess

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
            # Check if the service is active and running
            return result.returncode == 0 and "active (running)" in result.stdout
        elif command == 'restart':
            return result.returncode == 0
        
        return False  # Invalid command case

    except Exception as e:
        return False

def get_license_from_curl():
    # Run the curl command and get the latest license version
    """Get the latest license version."""
    try:
        result = subprocess.run(['curl', '-s', 'https://downloads.untangle.com/bdam/config'], stdout=subprocess.PIPE, text=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
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
                    print(f"Updating LicenseSerial to: {new_license}")
                    lines[i] = f"LicenseSerial={new_license}\n"  # Update the line with the new license

                break

        # Write the updated content back to the file
        with open(file_path, 'w') as file:
            file.writelines(lines)

    except Exception as e:
        #print(f"Error reading or writing the file: {e}")
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

