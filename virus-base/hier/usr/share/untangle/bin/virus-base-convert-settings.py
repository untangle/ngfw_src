#!/usr/bin/python
import conversion.sql_helper as sql_helper
import sys
import os

global nodeName

def get_http_filetypes(tid, debug=False):
    if (debug):
        print "Getting filetypes for node_id: ",tid

    rule_list = sql_helper.run_sql("select live, string, description, name from settings.n_virus_settings join settings.n_virus_vs_ext using (settings_id) join settings.u_string_rule using (rule_id) where tid = '%s'" % tid, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for rule in rule_list:
        enabled = rule[0]
        string =  rule[1]
        description = rule[2]
        if description == "[no description]":
            description = rule[3]
    
        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s",\n' % string
        str += '\t\t\t"enabled": "%s",\n' % enabled
        str += '\t\t\t"description": "%s"\n' % description
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str

def get_http_mimetypes(tid, debug=False):
    if (debug):
        print "Getting mimetypes for node_id: ",tid

    rule_list = sql_helper.run_sql("select live, mime_type, description, name from settings.n_virus_settings join settings.n_virus_vs_mt using (settings_id) join settings.u_mimetype_rule using (rule_id) where tid = '%s'" % tid, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for rule in rule_list:
        enabled = rule[0]
        string =  rule[1]
        description = rule[2]
        if description == "[no description]":
            description = rule[3]
    
        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s",\n' % string
        str += '\t\t\t"enabled": "%s",\n' % enabled
        str += '\t\t\t"description": "%s"\n' % description
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str


def get_pop_config(config_id, debug=False):
    if (debug):
        print "Getting config for config_id: ", config_id
    
    config_list = sql_helper.run_sql("select scan, action from settings.n_virus_pop_config where config_id = '%s'" % config_id, debug=debug)

    if config_list == None:
        print "WARNING: missing results for TID %s" % tid
        
    if len(config_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
    
    config = config_list[0]
    return config

def get_imap_config(config_id, debug=False):
    if (debug):
        print "Getting config for config_id: ", config_id
    
    config_list = sql_helper.run_sql("select scan, action from settings.n_virus_imap_config where config_id = '%s'" % config_id, debug=debug)

    if config_list == None:
        print "WARNING: missing results for TID %s" % tid
        
    if len(config_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
    
    config = config_list[0]
    return config

def get_smtp_config(config_id, debug=False):
    if (debug):
        print "Getting config for config_id: ", config_id
    
    config_list = sql_helper.run_sql("select scan, action from settings.n_virus_smtp_config where config_id = '%s'" % config_id, debug=debug)

    if config_list == None:
        print "WARNING: missing results for TID %s" % tid
        
    if len(config_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
    
    config = config_list[0]
    return config

def get_config(config_id, debug=False):
    if (debug):
        print "Getting config for config_id: ", config_id
    
    config_list = sql_helper.run_sql("select scan from settings.n_virus_config where config_id = '%s'" % config_id, debug=debug)

    if config_list == None:
        print "WARNING: missing results for TID %s" % tid
        
    if len(config_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
    
    config = config_list[0]
    return config

def get_settings(tid, debug=False):
    if (debug):
        print "Getting settings for TID: ",tid

    settings_list = sql_helper.run_sql("select  settings_id, disable_ftp_resume, disable_http_resume, http_config, ftp_config, smtp_config, pop_config, imap_config from n_virus_settings where tid = '%s'" % tid, debug=debug)

    if settings_list == None:
        print "WARNING: missing results for TID %s" % tid
        
    if len(settings_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
        
    settings = settings_list[0]

    settings_id = settings[0]
    disable_ftp_resume = settings[1] 
    disable_http_resume = settings[2]
    http_config_id = settings[3]
    ftp_config_id = settings[4]
    smtp_config_id = settings[5]
    pop_config_id = settings[6]
    imap_config_id = settings[7]

    http_config = get_config(http_config_id, debug)
    scan_http = http_config[0]

    ftp_config = get_config(ftp_config_id, debug)
    scan_ftp = ftp_config[0]

    smtp_config = get_smtp_config(smtp_config_id, debug)
    scan_smtp = smtp_config[0]
    smtp_action = smtp_config[1]
    if (smtp_action == "R"): smtp_action = "remove"
    if (smtp_action == "P"): smtp_action = "pass"
    if (smtp_action == "B"): smtp_action = "block"

    pop_config = get_pop_config(pop_config_id, debug)
    scan_pop = pop_config[0]
    pop_action = pop_config[1]
    if (pop_action == "R"): pop_action = "remove"
    if (pop_action == "P"): pop_action = "pass"
    if (pop_action == "B"): pop_action = "block"

    imap_config = get_imap_config(imap_config_id, debug)
    scan_imap = imap_config[0]
    imap_action = imap_config[1]
    if (imap_action == "R"): imap_action = "remove"
    if (imap_action == "P"): imap_action = "pass"
    if (imap_action == "B"): imap_action = "block"

    str = '{\n'
    str += '\t"javaClass": "com.untangle.node.virus.VirusSettings",\n'
    str += '\t"version": "1",\n' 
    #str += '\t"allowFtpResume": "%s",\n' % ( not disable_ftp_resume )
    #str += '\t"allowHttpResume": "%s",\n' %  ( not disable_http_resume )
    str += '\t"scanHttp": \"%s\",\n' % scan_http
    str += '\t"scanFtp": \"%s\",\n' % scan_ftp
    str += '\t"scanSmtp": \"%s\",\n' % scan_smtp
    str += '\t"smtpAction": \"%s\",\n' % smtp_action
    str += '\t"scanPop": \"%s\",\n' % scan_pop
    str += '\t"popAction": \"%s\",\n' % pop_action
    str += '\t"scanImap": \"%s\",\n' % scan_imap
    str += '\t"imapAction": \"%s\",\n' % imap_action
    str += '\t"httpFileExtensions": %s,\n' % get_http_filetypes(tid)
    str += '\t"httpMimeTypes": %s\n' % get_http_mimetypes(tid)
    str += '}\n'
    
    return str
    

filename = None
if len(sys.argv) < 3:
    print "usage: %s TID [clam|commtouchav] [filename]" % sys.argv[0]
    sys.exit(1)


if len(sys.argv) > 1:
    tid = sys.argv[1]

if len(sys.argv) > 2:
    nodeName = sys.argv[2]

if len(sys.argv) > 3:
    filename = sys.argv[3]

try:
    settings_str = get_settings(tid, debug=True)
    print settings_str
    if filename == None:
        filename = "/usr/share/untangle/settings/untangle-node-clamav/settings_%s.js" % tid
    dir = os.path.dirname(filename)
    if not os.path.exists(dir):
        os.makedirs(dir)

    file = open(filename, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("could not get result",e);
    sys.exit(1)

sys.exit(0)

