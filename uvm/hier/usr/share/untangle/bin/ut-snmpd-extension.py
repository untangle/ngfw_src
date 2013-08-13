#! /usr/bin/python -u

""" 'pass_persist' extension for snmpd. Do not remote the '-u' above,
as we need to run in unbuffered mode."""

import os.path, signal, sys, time

# in-house modules
sys.path.append("@PREFIX@/usr/lib/python2.6")
from untangle.log import *

sys.path.append("@PREFIX@/usr/share/untangle/pycli")
import message_queue

logger = getLogger('snmp', False)

class Oid:
  def __init__(self, name, type, value):
    self.name = name
    self.type = type
    self.value = value

class SnmpExtension:
  TTL = 15

  def __init__(self, baseOid):
    signal.signal(signal.SIGTERM, self.shutDown)
#    signal.signal(signal.SIGPIPE, self.shutDown)

    self.baseOid = baseOid
    self.last_req = 0
    self.oids = {}

  def dispatch(self, line):
    if line == '':
      return
    elif "PING" in line:
      self.doWrite("PONG")
    elif "getnext" in line:
      self.doGetNext(self.readTarget(self.readStdin()))
    elif "get" in line:
      self.doGet(self.readTarget(self.readStdin()))
    elif "set" in line:
      self.doWrite("NONE")

  def readStdin(self,):
    line = sys.stdin.readline().strip()
    logger.debug("in: %s" % line)
    return line

  def doWrite(self, string):
    logger.debug("out: %s" % string)
    print string

  def getOid(self, oid):
    return self.oids.get(oid, None)

  def doGet(self, oid):
    self.checkCache()
    oid = self.getOid(oid)
    if oid:
      self.sendData(oid)
    else:
      self.doWrite("NONE")

  def doGetNext(self, target):
    self.checkCache()
    try:
      i = self.sortedOids.index(target)
    except:
      i = -1

    if i+1 >= len(self.sortedOids):
      self.doWrite("NONE")
    else:
      oid = self.getOid(self.sortedOids[i+1])
      self.sendData(oid)

  def readTarget(self, line):
    # return a relative version of the target OID
    ext = line.replace(self.baseOid + ".", "")
    logger.info("relative target: '%s'" % ext)
    return ext

  def isCacheExpired(self):
    now = int(time.time())
    ret = ((now - self.last_req) > self.TTL)
    if ret:
      logger.info("cache expired: last update was on '%s')" % self.last_req)
    return ret

  def sortOids(self, o1, o2):
    s1 = o1.split('.')
    s2 = o2.split('.')
    if len(s1) != len(s2):
      d = abs(len(s2) - len(s1))
      if len(s1) < len(s2):
        s1.extend((0,)*d)
      else:
        s2.extend((0,)*d)

    for e1, e2 in zip(s1, s2):
      order = int(e1).__cmp__(int(e2))
      if order == 0:
        continue
      else:
        return order

  @staticmethod
  def cached(meth):
    def new(self):
      logger.info("getting fresh OID values")
      ret = meth(self)
      self.sortedOids = self.oids.keys()
      self.sortedOids.sort(self.sortOids)
      self.last_req = int(time.time())
      return ret
    return new

  def getData(self):
    """ To be implemented by subclasses and decorated with @cached. """
    pass

  def checkCache(self):
    if self.isCacheExpired():
      self.getData()

  def shutDown(self, sig = None, frame = None):
    if sig:
      logger.info("caught signal: %s" % sig)
    sys.exit(1)

  def sendData(self, oid):
    self.doWrite(self.baseOid + "." + oid.name)
    self.doWrite(oid.type)
    self.doWrite(oid.value)

  def run(self):
    logger.info("starting up")
    while not sys.stdin.closed:
      line = self.readStdin()
      if line:
        self.dispatch(line)
      else:
        self.shutDown()

class SnmpExtensionStatic(SnmpExtension):
  @SnmpExtension.cached
  def getData(self):
    oid = Oid("1.1", "integer", 123)
    self.oids[oid.name] = oid
    oid = Oid("1.2", "integer", 456)
    self.oids[oid.name] = oid

class SnmpExtensionFs(SnmpExtension):
  BASE_PATH = "/tmp/base"

  @SnmpExtension.cached
  def getData(self):
    for root, dirs, files in os.walk(self.BASE_PATH):
      if 'type' in files:
        name = root.replace(self.BASE_PATH + "/", "").replace("/", ".")
        type = open(os.path.join(root, 'type')).read().strip()
        value = open(os.path.join(root, 'value')).read().strip()
        self.oids[name] = Oid(name, type, value)

class SnmpExtensionUvm(SnmpExtension):

  NODES = { 'untangle-node-webfilter' : 1,
            'untangle-node-firewall' : 2,
            'untangle-node-ips' : 3,
            'untangle-node-protofilter' : 4,
            'untangle-node-ips' : 5,
            'untangle-node-phish' : 6,
            'untangle-node-clam' : 9,
            'untangle-node-spamassassin' : 10,
            'untangle-node-commtouchav' : 11,
            'untangle-node-webcache' : 12,
            'untangle-node-commtouchas' : 13,
            'untangle-node-sitefilter' : 14,
            'untangle-node-openvpn' : 15,
            'untangle-node-bandwidth' : 16,
            'untangle-node-adblocker' : 17,
            'untangle-node-ipsec' : 18,
            'untangle-node-classd' : 19 }

  RACKS = { 'Policy(default: Default Rack)' : 1 }

  EVENTS = { 'scan' : 7,
             'block' : 8,
             'pass' : 9,
             'detect' : 10,
             'remove' : 11,
             'mark' : 12,
             'quarantine' : 13,
             'hitCount' : 14,
             'systemCount' : 15,
             'missCount' : 16,
             'bypassCount' : 17,
             'spam' : 18,
             'nonspam' : 19,
             'boost' : 20,
             'log' : 21,
             'connect' : 22,
             'prioritize' : 23,
             'quotaExceeded' : 24,
             'penaltyBox' : 25 }

  COUNTS = { 'count' : 1,
             'countSinceMidnight' : 2,
             'lastActivityDate' : 8 } 

  @staticmethod
  def createMibData():
    print """SNMPv2-MIB DEFINITIONS ::= BEGIN

IMPORTS
    MODULE-IDENTITY, OBJECT-TYPE, NOTIFICATION-TYPE,
    TimeTicks, Counter32, snmpModules, mib-2
        FROM SNMPv2-SMI
    DisplayString, TestAndIncr, TimeStamp

        FROM SNMPv2-TC
    MODULE-COMPLIANCE, OBJECT-GROUP, NOTIFICATION-GROUP
        FROM SNMPv2-CONF;

untangle MODULE-IDENTITY
    LAST-UPDATED "20112170000Z"
    ORGANIZATION "Untangle Inc."
    CONTACT-INFO
    "WG-EMail: seb@untangle.com"

    DESCRIPTION
            "The MIB module for Untangle SNMP entities."
    ::= { mib-2 30054 }

bwc OBJECT IDENTIFIER ::= { untangle 16 }

bwc-foo OBJECT-TYPE
    SYNTAX      DisplayString (SIZE (0..255))
    MAX-ACCESS  read-only
    STATUS      current
    DESCRIPTION
            "meh"
    ::= { bwc 1 }

"""

#     for node in SnmpExtensionUvm.NODES:
#       for rack in range(20):
#         for event in SnmpExtensionUvm.EVENTS:
#           for count in SnmpExtensionUvm.COUNTS:
#           print 

    print "END"

  def convertTime(self, long):
    # oh boy...
    t = str(long)
    t = t[:-3] + '.' + t[-3:]
#    return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(float(t)))
    epoch = time.mktime(time.localtime(float(t)))
    epochNow = time.mktime(time.localtime())
    return (epochNow - epoch) * 100 # hundredth of seconds

  def getStat(self, hash):
    hash2 = {}
    for k, v in hash.iteritems():
      if k in ('count', 'countSinceMidnight'):
        hash2[k] = v
      elif k in ('lastActivityDate',):
        hash2[k] = self.convertTime(v['time'])
    return hash2

  def getName(self, tid, k, k2):
    logger.debug("%s, %s, %s" % (tid, k, k2))

    name, rack = tid[0], tid[1]

    if not rack in self.RACKS:
      self.RACKS[rack] = max(self.RACKS.values()) + 1

    out = '.'.join(map(str, (self.NODES[tid[0]], self.RACKS[rack], self.EVENTS[k], self.COUNTS[k2])))
    logger.debug(out)
    return out

  @SnmpExtension.cached
  def getData(self):
    tids, queue = message_queue.query()

    for tid, stats in queue['stats']['map'].iteritems():
      if tid != '0':
        tid = tids[tid]
        myStats = {}
        for k, v in stats['metrics']['map'].iteritems():
          myStats[k] = self.getStat(v)

#        print myStats
        for k, v in myStats.iteritems():
          for k2, v in v.iteritems():
            name = self.getName(tid, k, k2)
            if k2.count('Date'): 
              type = "timeticks"
            else:
              type = "integer"
            value = v
            oid = Oid(name, type, value)
            self.oids[name] = oid
            
            # aggregate for all racks
            node, rack, event, count = oid.name.split('.')
            if rack == 0:
              continue
            aggName = "%s.%s.%s.%s" % (node, 0, event, count)
            aggOid = self.oids.get(aggName, None)
            if aggOid:
              if oid.type != "timeticks":
                aggOid.value += oid.value
              else:
                aggOid.value = max(aggOid.value, oid.value)
            else:
              self.oids[aggName] = Oid(aggName, oid.type, oid.value)

## main

if len(sys.argv) == 2:
  # se = SnmpExtensionStatic(sys.argv[1])
  # se = SnmpExtensionFs(sys.argv[1])
  se = SnmpExtensionUvm(sys.argv[1])
  se.run()
else:
  SnmpExtensionUvm.createMibData()
