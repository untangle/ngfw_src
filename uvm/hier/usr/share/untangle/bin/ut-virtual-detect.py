#!/usr/bin/python3

import getopt
import sys
import os
import re

Debug = False
DEVICE_PATTERN = re.compile(r"^(V|VP)\d{4}$")

def get_virtual_type():
    # Unknown.  Let the UI interpret this as "custom"
    # which could be baremetal or another VM.
    virtual_type = ""

    # There are other possibilities but these are the systems we currently 
    # have available for testing.
    if os.path.exists("/sys/devices/virtual/dmi/id/product_serial"):
        file = open("/sys/devices/virtual/dmi/id/product_serial")
        product_serial = file.read().lower()
        file.close()

        if product_serial[0:3] == "ec2":
            # Amazon EC2
            virtual_type = "Amazon EC2"
        elif product_serial[0:6] == "vmware":
            # Some VMware product such as ESX or Workstation.
            virtual_type = "VMware"
    elif os.path.exists("/proc/xen"):
        virtual_type = "Xen"
    if os.path.exists("/sys/devices/virtual/dmi/id/product_name"):
        file = open("/sys/devices/virtual/dmi/id/product_name")
        product_name = file.read().strip()
        file.close()
        #Device models generally follow patterns like: V###, VP###
        if product_name and DEVICE_PATTERN.match(product_name):
            virtual_type = product_name
    return virtual_type;

def main(argv):
    """
    Main
    """
    global Debug

    try:
        opts, args = getopt.getopt(argv, "hs:d", ["help", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-d", "--debug"):
            Debug = True

    virtual_type = ""
    try:
        virtual_type = get_virtual_type()
    except:
        pass
    print(virtual_type)

if __name__ == "__main__":
    main( sys.argv[1:] )
