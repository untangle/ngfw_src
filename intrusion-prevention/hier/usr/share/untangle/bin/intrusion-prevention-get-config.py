#!/usr/bin/python3
"""
Retreive configuration variables from IPS.
"""
import getopt
import sys

if "@PREFIX@" != '':
    sys.path.insert(0, '@PREFIX@/usr/lib/python3/dist-packages')
#pylint: disable=wrong-import-position
import intrusion_prevention
#pylint: disable=wrong-import-position

def usage():
    """
    Show usage
    """
    print("usage")

def main(argv):
    """
    Main loop
    """
    global _debug
    _debug = False
    config_type = None

    try:
        opts, args = getopt.getopt(argv, "hsincaqvx:d", ["help", "variables", "debug"])
    except getopt.GetoptError as error:
        print(error)
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ("-h", "--help"):
            usage()
            sys.exit()
        elif opt in ("-d", "--debug"):
            _debug = True
        elif opt in ("--variables"):
            config_type = "variables"

    if config_type == "variables":
        suricata_conf = intrusion_prevention.SuricataConf(_debug=_debug)
        variables = suricata_conf.get_variables()
        for group in variables:
            for variable in variables[group]:
                print("%s=%s" % (variable, "default" if (variable == "HOME_NET" or variable == "EXTERNAL_NET") else variables[group][variable]))

if __name__ == "__main__":
    main(sys.argv[1:])
