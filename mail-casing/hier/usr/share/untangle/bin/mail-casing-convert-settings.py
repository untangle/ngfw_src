#!/usr/bin/python
import conversion.sql_helper as sql_helper
import simplejson
import sys
import os

#------------------------------------------------------------------------------

def pad(arg):
    str = ""
    for x in range(0,arg):
        str = str + " "
    return str

#------------------------------------------------------------------------------

def escape(arg):
    # first we escape backslash characters
    one = arg.replace('\\', '\\\\')

    # next we escape any double quote characters
    two = one.replace('"', '\\"')

    return two

#------------------------------------------------------------------------------

def get_quarantine_settings(sid, indent, debug=False):
    if (debug):
        print "Getting n_mail_quarantine_settings list for SID: ",sid

    settings = sql_helper.run_sql("select max_intern_time, max_idle_inbox_time, secret_key, hour_in_day, minute_in_day, max_quarantine_sz, send_daily_digests from n_mail_quarantine_settings where settings_id = '%s'" % sid, debug=debug)

    if (len(settings) == 0):
        return '{},'

    max_intern_time = settings[0][0]
    max_idle_inbox_time = settings[0][1]
    secret_key = settings[0][2]
    hour_in_day = settings[0][3]
    minute_in_day = settings[0][4]
    max_quarantine_sz = settings[0][5]
    send_daily_digests = settings[0][6]

    str = '{\n'

    str += pad(indent+4) + '"addressRemaps": %s\n' % get_address_remaps(sid, 8, debug=debug)

    str += pad(indent+4) + '"allowedAddressPatterns": %s\n' % get_allowed_address_patterns(sid, 8, debug=debug)

    str += pad(indent+4) + '"digestHourOfDay": %s,\n' % hour_in_day
    str += pad(indent+4) + '"digestMinuteOfDay": %s,\n' % minute_in_day
    str += pad(indent+4) + '"javaClass": "com.untangle.node.mail.papi.quarantine.QuarantineSettings",\n'
    str += pad(indent+4) + '"maxIdleInbox": %s,\n' % max_idle_inbox_time
    str += pad(indent+4) + '"maxMailIntern": %s,\n' % max_intern_time
    str += pad(indent+4) + '"maxQuarantineTotalSz": %s,\n' % max_quarantine_sz
#        str += pad(indent+4) + '"secretKey": "%s",\n' % escape(secret_key) if (secret_key != None) else ''
    str += pad(indent+4) + '"sendDailyDigests": %s\n' % send_daily_digests

    str += pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_address_remaps(sid, indent, debug=False):
    if (debug):
        print "Getting n_mail_email_addr_pair_rule list for SID: ",sid

    list = sql_helper.run_sql("select address1, address2, name, category, description, live, alert, log from n_mail_email_addr_pair_rule where settings_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.ArrayList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.ArrayList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        address1 = settings[0]
        address2 = settings[1]
        name = settings[2]
        category = settings[3]
        description = settings[4]
        live = settings[5]
        alert = settings[6]
        log = settings[7]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"address1": "%s",\n' % escape(address1) if (address1 != None) else ''
        str += pad(indent+12) + '"address2": "%s",\n' % escape(address2) if (address2 != None) else ''
        str += pad(indent+12) + '"alert": %s,\n' % alert
        str += pad(indent+12) + '"category": "%s",\n' % escape(category) if (category != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else ''
        str += pad(indent+12) + '"javaClass": "com.untangle.node.mail.papi.EmailAddressPairRule",\n'
        str += pad(indent+12) + '"live": %s,\n' % live
        str += pad(indent+12) + '"log": %s,\n' % log
        str += pad(indent+12) + '"name": "%s"\n' % escape(name) if (name != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_allowed_address_patterns(sid, indent, debug=False):
    if (debug):
        print "Getting n_mail_email_addr_rule list for SID: ",sid

    list = sql_helper.run_sql("select address, name, category, description, live, alert, log from n_mail_email_addr_rule where settings_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.ArrayList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.ArrayList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        address = settings[0]
        name = settings[1]
        category = settings[2]
        description = settings[3]
        live = settings[4]
        alert = settings[5]
        log = settings[6]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"address": "%s",\n' % escape(address) if (address != None) else ''
        str += pad(indent+12) + '"alert": %s,\n' % alert
        str += pad(indent+12) + '"category": "%s",\n' % escape(category) if (category != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else ''
        str += pad(indent+12) + '"javaClass": "com.untangle.node.mail.papi.EmailAddressRule",\n'
        str += pad(indent+12) + '"live": %s,\n' % live
        str += pad(indent+12) + '"log": %s,\n' % log
        str += pad(indent+12) + '"name": "%s"\n' % escape(name) if (name != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_safelist_settings(sid, indent, debug=False):
    if (debug):
        print "Getting n_mail_safe_settings list for SID: ",sid

    list = sql_helper.run_sql("select safels_id from n_mail_safelists where setting_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.ArrayList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.ArrayList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        safels_id = settings[0]

        item = sql_helper.run_sql("select recipient, sender from n_mail_safels_settings where safels_id = '%s'" % safels_id, debug=debug)
        recipient_id = item[0][0]
        sender_id = item[0][1]
        
        rrow = sql_helper.run_sql("select addr from n_mail_safels_recipient where id = '%s'" % recipient_id, debug=debug)
        recipient = rrow[0][0]

        srow = sql_helper.run_sql("select addr from n_mail_safels_sender where id = '%s'" % sender_id, debug=debug)
        sender = srow[0][0]
       
        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"javaClass": "com.untangle.node.mail.papi.safelist.SafelistSettings",\n'
        str += pad(indent+12) + '"recipient": "%s",\n' % escape(recipient) if (recipient != None) else ''
        str += pad(indent+12) + '"sender": "%s"\n' % escape(sender) if (sender != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_settings(debug=False):
    if (debug):
        print "Getting n_mail_settings"

    node_settings = sql_helper.run_sql("select settings_id, smtp_enabled, pop_enabled, imap_enabled, smtp_timeout, pop_timeout, imap_timeout, quarantine_settings, smtp_allow_tls from n_mail_settings", debug=debug)

    if node_settings == None:
        print "WARNING: missing n_mail_settings result"

    if len(node_settings) > 1:
        print "WARNING: too many n_mail_settings results (%i)" % (len(node_settings))

    settings_id = node_settings[0][0]
    smtp_enabled = node_settings[0][1]
    pop_enabled = node_settings[0][2]
    imap_enabled = node_settings[0][3]
    smtp_timeout = node_settings[0][4]
    pop_timeout = node_settings[0][5]
    imap_timeout = node_settings[0][6]
    quarantine_settings = node_settings[0][7]
    smtp_allow_tls = node_settings[0][8]

    str = '{\n'
    str += pad(4) + '"imapEnabled": %s,\n' % imap_enabled
    str += pad(4) + '"imapTimeout": %s,\n' % imap_timeout
    str += pad(4) + '"javaClass": "com.untangle.node.mail.papi.MailNodeSettings",\n'
    str += pad(4) + '"popEnabled": %s,\n' % pop_enabled
    str += pad(4) + '"popTimeout": %s,\n' % pop_timeout

    str += pad(4) + '"quarantineSettings": %s\n' % get_quarantine_settings(quarantine_settings, 4, debug=debug)

    str += pad(4) + '"safelistSettings": %s\n' % get_safelist_settings(settings_id, 4, debug=debug)

    str += pad(4) + '"smtpAllowTLS": %s,\n' % smtp_allow_tls
    str += pad(4) + '"smtpEnabled": %s,\n' % smtp_enabled
    str += pad(4) + '"smtpTimeout": %s,\n' % smtp_timeout
    str += pad(4) + '"version": 1\n'
    str += '}\n'

    return str

#------------------------------------------------------------------------------

filename = None
if len(sys.argv) < 2:
    print "usage: %s target_filename" % sys.argv[0]
    sys.exit(1)

target = sys.argv[1]
debug = False
#debug = True

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

    settings_str = get_settings(debug)

    if (debug):
        print settings_str

    file = open(target, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("Could not get result",e)
    sys.exit(1)

sys.exit(0)
