#!/usr/bin/python3
import argparse
import json
import re
import subprocess

Result = {}
DEBUG = False

class WirelessInterface():
    """
    Collect and set wireless interface settings via the iw command.
    """
    Iw_info_wiphy_re = re.compile(".*wiphy (\\d)")

    Iw_info_frequencies_section = "Frequencies"
    Iw_info_invalid_channels_re = re.compile("passive scanning|no IBSS|disabled|no IR|radar detection")
    Iw_info_channel_re = re.compile("^\\s+\\* (\\d)(\\d+) MHz \\[(\\d+)\\]")
    # Channels we should never use
    # https://community.netgear.com/t5/Nighthawk-WiFi-Routers/Why-you-should-NEVER-select-channel-165-in-5-GHz/td-p/1848856)
    # - 165: "Just bad":
    Disabled_channels = ["165"]

    Iw_reg_group_re = re.compile("^(global|phy#\d+)")
    Iw_reg_country_re = re.compile("country ([^:]+):")

    def __init__(self, interface_name=None):
        """
        Initialize object
        """
        self.interface_name = interface_name
        self.physical_name = None
        self.region = None

    def is_regulatory_compliant(self):
        """
        Check for compliance by ensuring that the "iw reg get" command has an interface.
        If True, we can expect all iw queries to work as expected.
        If False, all bets are off.
        """
        result = False

        command = ["iw","reg","get"]
        if DEBUG:
            Result["DEBUG"].append(' '.join(command))
        proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
        while True:
            line = proc.stdout.readline()
            if not line:
                break
            line = line.rstrip()
            if DEBUG:
                Result["DEBUG"].append(line)

            if line.startswith("phy#") and line == self.get_physical_name():
                # Presence of physical name indicates driver is complaint.
                result = True
                break

        return result

    def get_physical_name(self):
        """
        Determine the physical name, typically named like phy#0
        """
        if self.physical_name is None:
            # Only lookup if we've not determined the physical name yet.
            command = ["iw",self.interface_name,"info"]
            if DEBUG:
                Result["DEBUG"].append(' '.join(command))
            proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
            while True:
                line = proc.stdout.readline()
                if not line:
                    break
                if DEBUG:
                    Result["DEBUG"].append(line)

                matches = WirelessInterface.Iw_info_wiphy_re.search(line)
                if matches:
                    # Found the physical numeric identifier.  Physical name is phy#<identifier>.
                    self.physical_name = f"phy#{matches.group(1)}"

        return self.physical_name

    def get_valid_country_codes(self):
        """
        Query regulatory database and return valid country codes
        """
        country_codes = []
        command = ["regdbdump","/usr/lib/crda/regulatory.bin"]
        if DEBUG:
            Result["DEBUG"].append(' '.join(command))
        proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
        groups = {}
        current_group = None
        while True:
            line = proc.stdout.readline()
            if not line:
                break

            if DEBUG:
                Result["DEBUG"].append(line)

            matches = WirelessInterface.Iw_reg_country_re.search(line)
            if matches:
                # Found the country code
                if matches.group(1) == "00":
                    # Ignore the "unset" value
                    continue
                country_codes.append(matches.group(1))
        return country_codes

    def get_regulatory_country_code(self):
        """
        Determine the region.
        """
        if self.region is None:
            command = ["iw","reg","get"]
            if DEBUG:
                Result["DEBUG"].append(' '.join(command))
            proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
            groups = {}
            current_group = None
            while True:
                line = proc.stdout.readline()
                if not line:
                    break

                if DEBUG:
                    Result["DEBUG"].append(line)

                matches = WirelessInterface.Iw_reg_group_re.search(line)
                if matches:
                    # Found the current group.
                    current_group = matches.group(1)
                    continue

                if current_group:
                    matches = WirelessInterface.Iw_reg_country_re.search(line)
                    if matches:
                        # Found the country code
                        groups[current_group] = matches.group(1)
                        current_group = None

            if self.get_physical_name() in groups:
                # Exact physical name match (aka complaint)
                self.region = groups[self.get_physical_name()]

            if "global" in groups:
                # Based on what we've seen it's the global group that matters.
                # The physical region usually does not match the global, but if
                # we've seen the physical decvice appear in this query,
                # it seems to mean it always follows what the global is, even
                # if the physical is different.
                # That seems to make sense since regulatory setting is a systemwide
                # setting at this time.
                self.region = groups["global"]

        return self.region

    def get_channels(self, region=None):
        """
        Return list of channel to frequency
        If region is specified, temporarily set it, get the channels, and set it back.  
        """
        channels = []

        current_region = self.get_regulatory_country_code()
        if region:
            # Temporarily set the region
            command = ["iw","reg","set", region]
            if DEBUG:
                Result["DEBUG"].append(' '.join(command))
            subprocess.run(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True, check=False)

        in_frequencies = False
        command = ["iw",self.get_physical_name(),"info"]
        if DEBUG:
            Result["DEBUG"].append(' '.join(command))
        proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
        while True:
            line = proc.stdout.readline()
            if not line:
                break
            if DEBUG:
                Result["DEBUG"].append(line)

            if ":" in line:
                # Channels to scape are under Frequenies: sections
                in_frequencies = f"{WirelessInterface.Iw_info_frequencies_section}:" in line
                continue

            if in_frequencies:
                # Process channels in frequencies section
                matches = WirelessInterface.Iw_info_invalid_channels_re.search(line)
                if matches:
                    continue
                matches = WirelessInterface.Iw_info_channel_re.search(line)
                if matches:
                    if matches.group(3) in WirelessInterface.Disabled_channels:
                        continue
                    # iw displays frequency as MHz, but we want to present as GHz.
                    channels.append({
                        "channel": int(matches.group(3)),
                        "frequency": f"{matches.group(1)}.{matches.group(2)} GHz"
                    })

        if region:
            # Set the region back to current
            command = ["iw","reg","set", current_region]
            if DEBUG:
                Result["DEBUG"].append(' '.join(command))
            subprocess.run(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True, check=False)

        return channels

def main():
    """
    Main
    """
    global DEBUG
    global Result

    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument("-d", "--debug", help="Include DEBUG in result", action="store_true")
    arg_parser.add_argument("-i", "--interface", help="Interface name", required=True)
    arg_parser.add_argument("-q", "--query", help="Query to run", required=True, action="append")

    args = arg_parser.parse_args()

    DEBUG=args.debug
    if DEBUG is True:
        Result["DEBUG"] = []
        Result["DEBUG"].append(f"interface={args.interface}")

    interface = WirelessInterface(args.interface)
    for query in args.query:
        if "," in query:
            [method,args] = query.split(",")
        else:
            method = query
            args = None

        if method not in dir(WirelessInterface):
            print(f"unknown query method: {method}")
            continue

        if args:
            Result[method] = getattr(interface, method)(args)
        else:
            Result[method] = getattr(interface, method)()

    print(json.dumps(Result))

if __name__ == "__main__":
    main()