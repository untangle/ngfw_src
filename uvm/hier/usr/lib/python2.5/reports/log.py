import logging

MODULE_NAME = 'reports'

format = '%(asctime)s [%(name)s] %(levelname)s: %(message)s'
dateFmt = '%Y-%m-%d %H:%M:%S'
formatter = logging.Formatter(format, dateFmt)

f = logging.FileHandler("%s/var/log/uvm/reporter.log" % ('@PREFIX@',))
f.setLevel(logging.DEBUG)
f.setFormatter(formatter)
logging.getLogger('').addHandler(f)

console = logging.StreamHandler()
console.setLevel(logging.INFO)
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)

#  # default for root logger
# logging.getLogger('').setLevel(logging.DEBUG)

def getLogger(name):
  name = name.replace('%s.' % (MODULE_NAME,), '')
  logger = logging.getLogger(name)
  logger.setLevel(logging.DEBUG)
  return logger

def setLogLevel(level):
  global console
  console.setLevel(level)
