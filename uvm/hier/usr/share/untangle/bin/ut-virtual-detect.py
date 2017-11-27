#!/usr/bin/python

import getopt
import sys

Debug = False

def main(argv):
    """
    Main
    """
    global Debug

    # Unknown.  Let the UI interpret this as "custom"
    # which could be baremetal or another VM.
    virtual_type = ""

    try:
        opts, args = getopt.getopt(argv, "hs:d", ["help", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-d", "--debug"):
            Debug = True

    # There are other possibilities but these are the systems we currently 
    # have available for testing.
    file = open("/sys/devices/virtual/dmi/id/product_serial")
    product_serial = file.read().lower()
    file.close()

    if product_serial[0:3] == "ec2":
        # Amazon EC2
        virtual_type = "Amazon EC2"
    elif product_serial[0:6] == "vmware":
        # Some VMware product such as ESX or Workstation.
        virtual_type = "VMware"

    print(virtual_type);

if __name__ == "__main__":
    main( sys.argv[1:] )
