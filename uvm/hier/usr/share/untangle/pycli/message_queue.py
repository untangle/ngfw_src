import sys, time

import jscli, uvm

def convertTime(long):
  # oh boy...
  t = str(long)
  t = t[:-3] + '.' + t[-3:]
  return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(float(t)))

def getStat(hash):
  hash2 = {}
  hash3 = {}
  for k, v in hash.iteritems():
    if k in ('count', 'countSinceMidnight'):
      hash2[k] = v
    elif k in ('lastActivityDate',):
      hash3[k] = convertTime(v['time'])
  return hash2, hash3

def query():
  proxy = jscli.make_proxy(jscli.ArgumentParser(), 10)
  remoteContext = proxy.RemoteUvmContext

  nodeManager = uvm.NodeManager(remoteContext)
  messageManager = uvm.MessageManager(remoteContext)

  queue = messageManager.api_get_message_queue()
  tids = nodeManager.get_instances()

  return tids, queue

if __name__ == '__main__':
  print query()
