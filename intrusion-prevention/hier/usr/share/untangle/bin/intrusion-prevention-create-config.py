#!/usr/bin/python
"""
Create IPS configuration filesfrom settings
"""
import json
import os
import getopt
import sys
import re

from netaddr import IPNetwork

UNTANGLE_DIR = '%s/usr/lib/python%d.%d/dist-packages' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)
	
import intrusion_prevention

from intrusion_prevention import SuricataSignature
from intrusion_prevention import IntrusionPreventionRule

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
    app_id = "0"
    default_home_net = ""

    try:
        opts, args = getopt.getopt(argv, "hsincaqvx:d", ["help", "app_id=", "home_net=", "debug"] )
    except getopt.GetoptError as error:
        print error
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-d", "--debug"):
            _debug = True
        elif opt in ( "-n", "--app_id"):
            app_id = arg
        elif opt in ( "-v", "--home_net"):
            default_home_net = arg
            if default_home_net.find(",") != -1:
                default_home_net = "[" + default_home_net + "]"

    if _debug is True:
        print("app_id = %s" % (app_id))
        print("_debug = %r" % (_debug))

    if _debug is True:
        print("Loading ips settings")

    settings = intrusion_prevention.IntrusionPreventionSettings( app_id )
    if settings.exists() == False:
        print("cannot find settings file")
        sys.exit()
    settings.load()

    if _debug is True:
        print("Loading signatures")

    ##
    ## Load known signatures
    ##
    signatures = intrusion_prevention.SuricataSignatures()
    signatures.load()

    ##
    ## Integrate modifications from settings.
    ##
    for settings_signature in settings.settings["signatures"]["list"]:
        ##
        ## Add a custom new rule.
        ##
        match_signature = re.search( SuricataSignature.text_regex, settings_signature )
        if match_signature:
            signatures.add_signature(SuricataSignature( match_signature, "unknown"))

    if _debug is True:
        print("Applying rules")

    ## process rules over signatures
    rules = []
    for settings_rule in settings.settings["rules"]["list"]:
        rules.append(IntrusionPreventionRule(settings_rule))

    # Process rules in action precedence order.
    priority = { 'default': 0, 'log' : 1, 'blocklog': 2, 'block': 3, 'disable': 4}
    for rule in sorted(rules, key=lambda rule: (priority[rule.get_action()] )):
        if not rule.get_enabled():
            continue
        for signature in signatures.get_signatures().values():
            if rule.matches(signature):
                rule.set_signature_action(signature)

    # Disable signatures not modified by any rule.
    for signature in signatures.get_signatures().values():
        if not signature.get_action_changed():
            signature.set_action(False, False)

    if _debug is True:
        signature_action_counts = {
            'disabled': 0,
            'log': 0,
            'block': 0
        }
        for signature in signatures.get_signatures().values():
            action = signature.get_action()
            if action["log"] is False and action["block"] is False:
                signature_action_counts["disabled"] += 1
            elif action["block"] is True:
                signature_action_counts["block"] += 1
            elif action["log"] is True:
                signature_action_counts["log"] += 1
            else:
                print("Unknown Action")
                print action

        print(signature_action_counts)

    signatures.save()

    if _debug is True:
        print("Creating event map")
    intrusion_prevention_event_map = intrusion_prevention.IntrusionPreventionEventMap( signatures )
    intrusion_prevention_event_map.save()

    if _debug is True:
        print("Modifying suricata configuration")
    suricata_conf = intrusion_prevention.SuricataConf( _debug=_debug )

    # Override suricata configuration variables with settings variables
    for settings_variable in settings.get_variables():
        name = settings_variable["name"]
        value = settings_variable["value"]
        if settings_variable["name"] == "HOME_NET":
            if settings_variable["value"] == "default":
                value = default_home_net
        if settings_variable["name"] == "EXTERNAL_NET":
            if settings_variable["value"] == "default":
                value = "any"

        suricata_conf.set_variable( name, value )

    if "suricataSettings" in settings.settings:
        suricata_conf.set(settings.settings["suricataSettings"])

    suricata_conf.save()


    ### !!! write override for systemctl

    # queue_num = "0"
    # ipf = open( iptables_script )
    # for line in ipf:
    #     line = line.strip()
    #     setting = line.split("=")
    #     if setting[0] == "SURICATA_QUEUE_NUM":
    #         queue_num = setting[1] 
    # ipf.close()
    # 
    # !! also modify systemd/system/system.suricata/overlay file reference
    
if __name__ == "__main__":
    main(sys.argv[1:])
