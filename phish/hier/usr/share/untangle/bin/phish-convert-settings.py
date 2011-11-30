#!/usr/bin/python
import conversion.sql_helper as sql_helper
import sys
import os

global nodeName

#------------------------------------------------------------------------------
def action_string(arg):

    if (arg == "P"):
        str = "PASS"

    elif (arg == "M"):
        str = "MARK"

    elif (arg == "B"):
        str = "BLOCK"

    elif (arg == "Q"):
        str = "QUARANTINE"

    elif (arg == "S"):
        str = "SAFELIST"

    elif (arg == "Z"):
        str = "OVERSIZE"

    return str

#------------------------------------------------------------------------------

def get_smtp_config(sid, debug=False):
    if (debug):
        print "Getting n_spam_smtp_config for SID: ",sid

    n_spam_smtp_config = sql_helper.run_sql("select scan, strength, add_spam_headers, block_superspam, superspam_strength, fail_closed, limit_load, limit_scans, msg_size_limit, msg_action, tarpit, tarpit_timeout, scan_wan_mail from n_spam_smtp_config where config_id = '%s'" % sid, debug=debug)

    if n_spam_smtp_config == None:
        print "WARNING: missing n_spam_smtp_config result for TID %s" % tid

    if len(n_spam_smtp_config) > 1:
        print "WARNING: too many n_spam_smtp_config results (%i) for TID %s" % (len(n_spam_smtp_config),tid)

    scan = n_spam_smtp_config[0][0]
    strength = n_spam_smtp_config[0][1]
    add_spam_headers = n_spam_smtp_config[0][2]
    block_superspam =  n_spam_smtp_config[0][3]
    superspam_strength =  n_spam_smtp_config[0][4]
    fail_closed =  n_spam_smtp_config[0][5]
    limit_load =  n_spam_smtp_config[0][6]
    limit_scans =  n_spam_smtp_config[0][7]
    msg_size_limit =  n_spam_smtp_config[0][8]
    msg_action =  n_spam_smtp_config[0][9]
    tarpit =  n_spam_smtp_config[0][10]
    tarpit_timeout =  n_spam_smtp_config[0][11]
    scan_wan_mail =  n_spam_smtp_config[0][12]

    str = '\n'
    str += '\t{\n'
    str += '\t\t"javaClass": "com.untangle.node.spam.SpamSmtpConfig",\n'
    str += '\t\t"addSpamHeaders": %s,\n' % add_spam_headers
    str += '\t\t"blockSuperSpam": %s,\n' % block_superspam
    str += '\t\t"failClosed": %s,\n' % fail_closed
    str += '\t\t"headerName": "X-Spam-Flag",\n'
    str += '\t\t"loadLimit": %s,\n' % limit_load
    str += '\t\t"msgAction": "%s",\n' % action_string(msg_action)
    str += '\t\t"msgSizeLimit": %s,\n' % msg_size_limit
    str += '\t\t"scan": %s,\n' % scan
    str += '\t\t"scanLimit": %s,\n' % limit_scans
    str += '\t\t"scanWanMail": %s,\n' % scan_wan_mail
    str += '\t\t"strength": %s,\n' % strength
    str += '\t\t"superSpamStrength": %s,\n' % superspam_strength
    str += '\t\t"tarpit": %s,\n' % tarpit
    str += '\t\t"tarpitTimeout": %s,\n' % tarpit_timeout
    str += '\t},\n'

    return str

#------------------------------------------------------------------------------

def get_imap_config(sid, debug=False):
    if (debug):
        print "Getting n_spam_imap_config for SID: ",sid

    n_spam_imap_config = sql_helper.run_sql("select scan, strength, add_spam_headers, msg_size_limit, msg_action from n_spam_imap_config where config_id = '%s'" % sid, debug=debug)

    if n_spam_imap_config == None:
        print "WARNING: missing n_spam_imap_config result for TID %s" % tid

    if len(n_spam_imap_config) > 1:
        print "WARNING: too many n_spam_imap_config results (%i) for TID %s" % (len(n_spam_imap_config),tid)

    scan = n_spam_imap_config[0][0]
    strength = n_spam_imap_config[0][1]
    add_spam_headers = n_spam_imap_config[0][2]
    msg_size_limit =  n_spam_imap_config[0][3]
    msg_action =  n_spam_imap_config[0][4]

    str = '\n'
    str += '\t{\n'
    str += '\t\t"javaClass": "com.untangle.node.spam.SpamImapConfig",\n'
    str += '\t\t"addSpamHeaders": %s,\n' % add_spam_headers
    str += '\t\t"headerName": "X-Spam-Flag",\n'
    str += '\t\t"msgAction": "%s",\n' % action_string(msg_action)
    str += '\t\t"msgSizeLimit": %s,\n' % msg_size_limit
    str += '\t\t"scan": %s,\n' % scan
    str += '\t\t"strength": %s,\n' % strength
    str += '\t},\n'

    return str

#------------------------------------------------------------------------------

def get_pop_config(sid, debug=False):
    if (debug):
        print "Getting n_spam_pop_config for SID: ",sid

    n_spam_pop_config = sql_helper.run_sql("select scan, strength, add_spam_headers, msg_size_limit, msg_action from n_spam_pop_config where config_id = '%s'" % sid, debug=debug)

    if n_spam_pop_config == None:
        print "WARNING: missing n_spam_pop_config result for TID %s" % tid

    if len(n_spam_pop_config) > 1:
        print "WARNING: too many n_spam_pop_config results (%i) for TID %s" % (len(n_spam_pop_config),tid)

    scan = n_spam_pop_config[0][0]
    strength = n_spam_pop_config[0][1]
    add_spam_headers = n_spam_pop_config[0][2]
    msg_size_limit =  n_spam_pop_config[0][3]
    msg_action =  n_spam_pop_config[0][4]

    str = '\n'
    str += '\t{\n'
    str += '\t\t"javaClass": "com.untangle.node.spam.SpamPopConfig",\n'
    str += '\t\t"addSpamHeaders": %s,\n' % add_spam_headers
    str += '\t\t"headerName": "X-Spam-Flag",\n'
    str += '\t\t"msgAction": "%s",\n' % action_string(msg_action)
    str += '\t\t"msgSizeLimit": %s,\n' % msg_size_limit
    str += '\t\t"scan": %s,\n' % scan
    str += '\t\t"strength": %s,\n' % strength
    str += '\t},\n'

    return str

#------------------------------------------------------------------------------

def get_rbl_list(sid, debug=False):
    if (debug):
        print "Getting spam_rbls for SID: ",sid

    rbl_list = sql_helper.run_sql("select hostname, active, description from n_spam_rbl where id = '%s'" % sid, debug=debug)

    str = '{\n'
    str += '\t"javaClass": "java.util.LinkedList",\n'
    str += '\t"list": [\n'

    first = True
    for settings in rbl_list:
        hostname = settings[0]
        active = settings[1]
        description = settings[2]

        if not first:
            str += ',\n'
        str += '\t\t{\n'

        str += '\t\t\t"hostname": "%s",\n' % hostname
        str += '\t\t\t"active": %s,\n' % active

        # need to escape backslash and double quotes
        aa = description.replace('\\', '\\\\')
        bb = aa.replace('"', '\\"')
        str += '\t\t\t"description": "%s",\n' % bb
        str += '\t\t\t"javaClass": "com.untangle.node.spam.SpamRBL"\n'
        str += '\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t}'

    return str

#------------------------------------------------------------------------------

def get_settings(tid, debug=False):

    if (debug):
        print "Getting n_spam_settings for TID: ",tid

    n_spam_settings = sql_helper.run_sql("select settings_id, smtp_config, pop_config, imap_config from n_spam_settings where tid = '%s'" % tid, debug=debug)

    if n_spam_settings == None:
        print "WARNING: missing n_spam_settings result for TID %s" % tid

    if len(n_spam_settings) > 1:
        print "WARNING: too many n_spam_settings results (%i) for TID %s" % (len(n_spam_settings),tid)

    settings_id = n_spam_settings[0][0]
    smtp_config = n_spam_settings[0][1]
    pop_config = n_spam_settings[0][2]
    imap_config = n_spam_settings[0][3]

    n_spam_rbl_list = sql_helper.run_sql("select rule_id from n_spam_rbl_list where settings_id = '%s'" % settings_id, debug=debug)

    if n_spam_rbl_list == None:
        print "WARNING: missing n_spam_rbl_list result for SID %s" % settings_id

    if len(n_spam_rbl_list) > 1:
        print "WARNING: too many n_spam_rbl_list results (%i) for SID %s" % (len(n_spam_rbl_list),settings_id)

    rule_id = n_spam_rbl_list[0][0]

    if (debug):
        print "Getting n_phish_settings for TID: ",tid

    n_phish_settings = sql_helper.run_sql("select enable_google_sb from n_phish_settings where spam_settings_id = '%s'" % settings_id, debug=debug)

    if n_phish_settings == None:
        print "WARNING: missing n_phish_settings result for SID %s" % settings_id

    if len(n_phish_settings) > 1:
        print "WARNING: too many n_phish_settings results (%i) for SID %s" % (len(n_phish_settings),settings_id)

    enable_google_sb = n_phish_settings[0][0]

    str = '{\n'
    str += '\t"javaClass": "com.untangle.node.phish.PhishSettings",\n'
    str += '\t"enableGooglePhishList": %s,\n' % enable_google_sb
    str += '\t"version": 1,\n'
    str += '\t"smtpConfig": %s\n' % get_smtp_config(smtp_config, debug=debug)
    str += '\t"imapConfig": %s\n' % get_imap_config(imap_config, debug=debug)
    str += '\t"popConfig": %s\n' % get_pop_config(pop_config, debug=debug)
    str += '\t"spamRBLList": %s\n' % get_rbl_list(rule_id, debug=debug)
    str += '}\n'

    return str

#------------------------------------------------------------------------------

filename = None
if len(sys.argv) < 3:
    print "usage: %s node_id target_filename" % sys.argv[0]
    sys.exit(1)

nodeid = sys.argv[1]
target = sys.argv[2]
debug = False

try:
    (pathname, filename) = os.path.split(target)
    if pathname == "":
        pathname = "."

    fullname = pathname + "/" + filename

    if (debug):
        print("FULL: ",fullname)
        print("PATH: ",pathname)
        print("FILE: ",filename)

    if not os.path.exists(pathname):
        os.makedirs(pathname)

    settings_str = get_settings(nodeid, debug)

    if (debug):
        print settings_str

    file = open(target, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("Could not get result",e)
    sys.exit(1)

sys.exit(0)

