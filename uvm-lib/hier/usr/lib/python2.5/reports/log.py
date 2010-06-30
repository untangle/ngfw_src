import logging, sys

MODULE_NAME = 'reports'
LOGFILE = "/var/log/uvm/reporter.log"

# line format
format = '%(asctime)s [%(name)s] %(levelname)s: %(message)s'
dateFmt = '%Y-%m-%d %H:%M:%S'
formatter = logging.Formatter(format, dateFmt)

# file logging
f = logging.FileHandler("%s%s" % ('@PREFIX@', LOGFILE))
f.setLevel(logging.DEBUG)
f.setFormatter(formatter)
logging.getLogger('').addHandler(f)

# stdout logging
console = logging.StreamHandler(sys.stdout)
console.setLevel(logging.INFO)
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)

def getLogger(name):
  name = name.replace('%s.' % (MODULE_NAME,), '')
  logger = logging.getLogger(name)
  logger.setLevel(logging.DEBUG)
  return logger

def setConsoleLogLevel(level):
  global console
  console.setLevel(level)
