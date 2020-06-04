#!/usr/bin/env python3

import os, getopt, sys, json, subprocess, parse, platform

def debug(str):
    if False:
        print(str)

def run( cmd, ignore_errors=False, print_cmd=False ):
    if print_cmd:
        print(cmd)
    ret = os.system( cmd )
    if ret != 0 and not ignore_errors:
        print("ERROR: Command failed: %i \"%s\"" % (ret, cmd))

def runSubprocess(cmd):
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
    result=[]
    for line in proc.stdout:
        result.append(line)
    return result

def statusToJSON(input):
    #Parse patterns for qos-service.py status output
    firstLine='interface: {} class {} {} {} rate {} ceil {} burst {} cburst {}'
    secondLine=' Sent {:d} bytes {:d} pkt (dropped {:d}, overlimits {:d} requeues {:d}) '
    lastLine=' tokens: {} ctokens:{}'
    priorityParser='parent {} leaf {} prio {:d}'
    indexMap={1:firstLine, 2:secondLine, 3:lastLine}
    priorityQueueToName = { '10:': '0 - Default','11:': '1 - Very High','12:': '2 - High', '13:':'3 - Medium','14:':'4 - Low','15:':'5 - Limited','16:':'6 - Limited More','17:':'7 - Limited Severely' }

    output = []
    count = 1
    entry = {}
    skipEntry=False
    for line in input:
        line = str(line, 'ascii','ignore')
        if count <= 3:
            res=parse.parse(indexMap[count],line)
            if res == None:
                continue
            parsed = res.fixed
            if count == 1:
                entry['interface_name']=parsed[0]
                entry['burst']=parsed[len(parsed)-2]
                if 'prio' in parsed[3]:
                    skipEntry=False
                    prioParse = parse.parse(priorityParser, parsed[3])
                    val=prioParse.fixed
                    entry['priority']=priorityQueueToName[val[1]]
                if parsed[3]=='root':
                    skipEntry=True
            if count == 2:
                entry['sent']=str(parsed[0]) + ' bytes'
            if count == 3:
                entry['tokens']=int(parsed[0])
                entry['ctokens']=int(parsed[1])
            count+=1
        else:
            if not skipEntry:
                output.append(dict(entry))
            entry.clear()
            count=1
            continue
    newlist = sorted(output, key=lambda k: k['priority'])
    return newlist
    

def status( qos_interfaces, wan_intfs ):

    json_objs = []
    for wan_intf in wan_intfs:
        result=''
        wan_dev = wan_intf.get('systemDev')
        imq_dev = wan_intf.get('imqDev')
        wan_name = wan_intf.get('name')
        result= runSubprocess( "tc -s class ls dev %s | sed \"s/^class/interface: %s Outbound class/\"" % (wan_dev, wan_name) )
        result.extend( runSubprocess( "tc -s class ls dev %s | sed \"s/^class/interface: %s Inbound class/\"" % (imq_dev, wan_name)))
        json_objs.extend( statusToJSON(result) )
       
        #run("echo ------ Qdisc  ------")
        #run("tc -s qdisc ls dev %s" % wan_dev)
        #run("tc -s qdisc ls dev %s" % imq_dev)
        #run("echo ------ Class  ------")
        #run("tc -s class ls dev %s" % wan_dev)
        #run("tc -s class ls dev %s" % imq_dev)
        #run("echo ------ Filter ------")
        #run("tc -s filter ls dev %s" % wan_dev)
        #run("tc -s filter ls dev %s" % imq_dev)
    print(json_objs)
        

#
# Main
#

if not os.path.exists('/usr/share/untangle/settings/untangle-vm/network.js'):
    print("Failed to read network settings")
    sys.exit(1)

network_settings = json.loads(open('/usr/share/untangle/settings/untangle-vm/network.js', 'r').read())
if network_settings == None:
    print("Failed to read network settings")
    sys.exit(1)

qos_settings = network_settings.get('qosSettings')
if qos_settings == None:
    print("Failed to read qos settings")
    sys.exit(1)

if qos_settings.get('defaultPriority') == None:
    print("Failed to read default class")
    sys.exit(1)

if network_settings.get('interfaces') == None:
    print("Failed to read interfaces")
    sys.exit(1)
interfaces = network_settings.get('interfaces').get('list')

wan_intfs = []
for intf in interfaces:
    if intf.get('configType') == "ADDRESSED" and intf.get('isWan'):
        if intf.get('systemDev') == None:
            print("Failed to read systemDev on %s" % intf.get('name'))
            sys.exit(1)
        if intf.get('imqDev') == None:
            print("Failed to read imqDev on %s" % intf.get('name'))
            sys.exit(1)
        if intf.get('downloadBandwidthKbps') == None:
            print("Failed to read downloadBandwidthKbps on %s" % intf.get('name'))
            sys.exit(1)
        if intf.get('uploadBandwidthKbps') == None:
            print("Failed to read uploadBandwidthKbps on %s" % intf.get('name'))
            sys.exit(1)
        wan_intfs.append(intf)


status( qos_settings, wan_intfs )
sys.exit(0)


