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

from intrusion_prevention.suricata_signature import SuricataSignature
from intrusion_prevention.intrusion_prevention_rule import IntrusionPreventionRule

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
    # classtypes = []
    # categories = []
    # msgs = []
    iptables_script = ""
    default_home_net = ""
    # default_interfaces = ""
    # signatures_path = None

    try:
        # opts, args = getopt.getopt(argv, "hsincaqvx:d", ["help", "app_id=", "classtypes=", "categories=", "msgs=", "iptables_script=", "home_net=", "interfaces=", "debug", "signatures=" ] )
        # opts, args = getopt.getopt(argv, "hsincaqvx:d", ["help", "app_id=", "iptables_script=", "home_net=", "interfaces=", "debug", "signatures=" ] )
        opts, args = getopt.getopt(argv, "hsincaqvx:d", ["help", "app_id=", "iptables_script=", "home_net=", "debug"] )
    except getopt.GetoptError:
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
        # elif opt in ( "-c", "--classtypes"):
        #     classtypes = arg.split(",")
        # elif opt in ( "-a", "--categories"):
        #     categories = arg.split(",")
        # elif opt in ( "-m", "--msgs"):
        #     msgs = arg.split(",")
        elif opt in ( "-i", "--iptables_script"):
            iptables_script = arg
        elif opt in ( "-v", "--home_net"):
            default_home_net = arg
            if default_home_net.find(",") != -1:
                default_home_net = "[" + default_home_net + "]"
        # elif opt in ( "-x", "--interfaces"):
        #     default_interfaces = arg.split(",")
        # elif opt in ( "-r", "--signatures"):
        #     signatures_path = arg

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

    ## get current signatures
    ## apply settings signature mods
    ## apply rules   
    #
    signatures = intrusion_prevention.SuricataSignatures()
    signatures.load()

    ##
    ## Integrate modifications from settings.
    ##
    for settings_signature in settings.settings["signatures"]["list"]:
        if type(settings_signature) == dict:
            ##
            ## Modify a rule.  NO LONGER NEEDED?
            ##
            for signature in signatures.get_signatures().values():
                if signature.get_sid() == settings_signature["sid"] and signature.get_gid() == settings_signature["gid"]:
                    signature.set_action(settings_signature["log"], settings_signature["block"])
        else:
            ##
            ## Add a new rule.
            ##
            match_signature = re.search( SuricataSignature.text_regex, settings_signature )
            if match_signature:
                # signatures.add_signature(SuricataSignature( match_signature, category, signature_path))
                signatures.add_signature(SuricataSignature( match_signature, "unknown"))

    if _debug is True:
        print("Applying rules")

    ## process rules over signatures
    rules = []
    for settings_rule in settings.settings["rules"]["list"]:
        rules.append(IntrusionPreventionRule(settings_rule))
        # rule = IntrusionPreventionRule(settings_rule)
        # print rule.get_action()

    ## !!! maybe a new Rules module?
    #
    # Process rules in action precedence order.
    priority = { 'default': 0, 'log' : 1, 'blocklog': 2, 'block': 3, 'disable': 4}
    for rule in sorted(rules, key=lambda rule: (priority[rule.get_action()] )):
        if not rule.get_enabled():
            continue
        for signature in signatures.get_signatures().values():
            if rule.matches(signature):
                rule.set_signature_action(signature)

    # For any rule that wasn't changed by rulees, disable.
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


    # get signature report

#    print len(signatures.get_signatures().values())
#    
    # sys.exit(1)

    # signatures.save(suricata_conf.get_variable( "RULE_PATH" ), classtypes, categories, msgs )
    # signatures.save(suricata_conf.get_variable( "PREPROC_RULE_PATH" ), classtypes, categories, msgs )
    signatures.save()

    if _debug is True:
        print("Creating event map")
    intrusion_prevention_event_map = intrusion_prevention.IntrusionPreventionEventMap( signatures )
    intrusion_prevention_event_map.save()

    if _debug is True:
        print("Modifying suricata configuration")
    suricata_conf = intrusion_prevention.SuricataConf( _debug=_debug )

    print("loaded conf")
    # print suricata_conf.conf
    # for key in suricata_conf.conf:
    #     print key
    #     print type(suricata_conf.conf[key])
    #     if type(suricata_conf.conf[key]) is dict:
    #         for subkey in suricata_conf.conf[key]:
    #             print "\t" + subkey

    print 'HOME_NET=' + suricata_conf.get_variable('HOME_NET')



    # # Override suricata configuration variables with settings variables
    # for settings_variable in settings.get_variables():
    #     suricata_conf.set_variable( settings_variable["variable"], settings_variable["definition"] )

    # if suricata_conf.get_variable('HOME_NET') == None:
    suricata_conf.set_variable( "HOME_NET", default_home_net )
    suricata_conf.set_variable("EXTERNAL_NET", "!$HOME_NET");

    if "suricata" in settings.settings:
        suricata_conf.set(settings.settings["suricata"])

    # interfaces = settings.get_interfaces()
    # interfaces = None
    # if interfaces == None:
    #     interfaces = default_interfaces

    # for include in suricata_conf.get_includes():
    #     match_include_signature = re.search( intrusion_prevention.SuricataConf.include_signaturepath_regex, include["file_name"] )
    #     if match_include_signature:
    #         suricata_conf.set_include( include["file_name"], False )
    # # suricata_conf.set_include( "$RULE_PATH/" + os.path.basename( signatures.get_file_name() ) )
    # # suricata_conf.set_include( "$PREPROC_RULE_PATH/" + os.path.basename( signatures.get_file_name() ) )

    suricata_conf.save()
	
    # suricata_debian_conf = intrusion_prevention.SuricataDebianConf( _debug=_debug )

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
    
    # suricata_debian_conf.set_variable("HOME_NET", suricata_conf.get_variable("HOME_NET"))
    # suricata_debian_conf.set_variable("OPTIONS", "--daq-dir /usr/lib/daq --daq nfq --daq-var queue=" + queue_num + " -Q")
    # suricata_debian_conf.set_variable("INTERFACE", ":".join(interfaces))
    # suricata_debian_conf.save()

if __name__ == "__main__":
    main(sys.argv[1:])
