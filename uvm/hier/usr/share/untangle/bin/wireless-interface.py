#!/usr/bin/python3
import getopt
import json
import re
import subprocess
import sys

Result = {}
Debug = False

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

    Iw_reg_group_re = re.compile("^(global|phy\#\d+)")
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
        if Debug:
            Result["debug"].append(' '.join(command))
        proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
        while True:
            line = proc.stdout.readline()
            if not line:
                break
            line = line.rstrip()
            if Debug:
                Result["debug"].append(line)

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
            if Debug:
                Result["debug"].append(' '.join(command))
            proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
            while True:
                line = proc.stdout.readline()
                if not line:
                    break
                if Debug:
                    Result["debug"].append(line)

                matches = WirelessInterface.Iw_info_wiphy_re.search(line)
                if matches is not None:
                    # Found the physical numeric identifier.  Physical name is phy#<identifier>.
                    self.physical_name = f"phy#{matches.group(1)}"

        return self.physical_name

    def get_regulatory_country_code(self):
        """
        Determine the region.
        """
        if self.region is None:
            command = ["iw",self.get_physical_name(), "reg","get"]
            if Debug:
                Result["debug"].append(' '.join(command))
            proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
            groups = {}
            current_group = None
            while True:
                line = proc.stdout.readline()
                if not line:
                    break

                if Debug:
                    Result["debug"].append(line)

                matches = WirelessInterface.Iw_reg_group_re.search(line)
                if matches is not None:
                    # Found the current group.
                    current_group = matches.group(1)
                    continue

                if current_group is not None:
                    matches = WirelessInterface.Iw_reg_country_re.search(line)
                    if matches is not None:
                        # Found the country code
                        groups[current_group] = matches.group(1)
                        current_group = None

            if self.get_physical_name() in groups:
                # Exact physical name match (aka complaint)
                self.region = groups[self.get_physical_name()]
            elif "global" in groups:
                # Non-compliant driver; try global
                self.region = groups["global"]

        return self.region

    def get_channels(self, region=None):
        """
        Return list of channel to frequency
        If region is specified, temporarily set it, get the channels, and set it back.  
        """
        channels = []

        current_region = self.get_regulatory_country_code()
        if region is not None:
            # Temporarily set the region
            command = ["iw","reg","set", region]
            if Debug:
                Result["debug"].append(' '.join(command))
            region_command = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
            region_command_output = region_command.communicate()[0]

        in_frequencies = False
        command = ["iw",self.get_physical_name(),"info"]
        if Debug:
            Result["debug"].append(' '.join(command))
        proc = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
        while True:
            line = proc.stdout.readline()
            if not line:
                break
            if Debug:
                Result["debug"].append(line)

            if ":" in line:
                # Channels to scape are under Frequenies: sections
                in_frequencies = f"{WirelessInterface.Iw_info_frequencies_section}:" in line
                continue

            if in_frequencies:
                # Process channels in frequencies section
                matches = WirelessInterface.Iw_info_invalid_channels_re.search(line)
                if matches is not None:
                    continue
                matches = WirelessInterface.Iw_info_channel_re.search(line)
                if matches is not None:
                    if matches.group(3) in WirelessInterface.Disabled_channels:
                        continue
                    # iw displays frequency as MHz, but we want to present as GHz.
                    channels.append({
                        "channel": int(matches.group(3)),
                        "frequency": f"{matches.group(1)}.{matches.group(2)} GHz"
                    })

        if region is not None:
            # Set the region back to current
            command = ["iw","reg","set", current_region]
            if Debug:
                Result["debug"].append(' '.join(command))
            region_command = subprocess.Popen(command, stderr=subprocess.STDOUT,stdout=subprocess.PIPE, text=True)
            region_command_output = region_command.communicate()[0]

        return channels

def usage():
    """
    Usage
    """
    print("usage")
    print("help\t\tUsage")
    print("debug\t\tInclude debug in result")
    print("query\t\tMethod[,args].  This can be specified multiple times.")
    print()
    print("Valid query arguments include:")
    print("\tis_regulatory_compliant\t\tBoolean true if driver is iw compliant, false otherwise")
    print("\tget_physical_name\t\tReturn string of physical name (e.g.,phy#0)")
    print("\tget_regulatory_country_code\tReturn string region (country)")
    print("\tget_channels\t\t\tReturn list of channels and frequencies.  If region argument is passed, get list for that region")
    print()
    print("Result returned as json object")

def main(argv):
    """
    Main
    """
    global Debug
    global Result

    try:
        opts, args = getopt.getopt(argv, "hdirq", ["help", "debug", "interface=", "query="] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    interface_name = None
    queries = []
    invalid = False
    for opt, arg in opts:
        if opt in ("-h", "--help"):
            usage()
            sys.exit()
        elif opt in ("-d", "--debug"):
            Debug = True
        elif opt in ("-i", "--interface"):
            interface_name = arg
        elif opt in ("-q", "--query"):
            if "," in arg:
                [query,query_args] = arg.split(",")
            else:
                query = arg
                query_args = None

            queries.append({"query": query, "args": query_args})
            if query not in dir(WirelessInterface):
                print(f"unknown query method: {arg}")
                invalid = True
    
    if interface_name is None or invalid is True:
        print("Missing interface")
        usage()
        sys.exit(2)

    if Debug is True:
        Result["debug"] = []
        Result["debug"].append(f"interface_name={interface_name}")

    interface = WirelessInterface(interface_name)
    for q in queries:
        if q["args"] is not None:
            Result[q["query"]] = getattr(interface, q["query"])(q["args"])
        else:
            Result[q["query"]] = getattr(interface, q["query"])()
    
    print(json.dumps(Result))

if __name__ == "__main__":
    main(sys.argv[1:])
