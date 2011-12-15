import logging, sys
from logging.handlers import SysLogHandler

MODULE_NAME = 'reports'

# line format
format = '%(asctime)s %(name)s[%(process)s] %(levelname)s: %(message)s'
syslogFormat = '%(name)s[%(process)s] %(levelname)s: %(message)s'
dateFmt = '%Y-%m-%d %H:%M:%S'
formatter = logging.Formatter(format, dateFmt)
syslogFormatter = logging.Formatter(syslogFormat)

# file logging
f = SysLogHandler(facility = SysLogHandler.LOG_LOCAL4)
f.setLevel(logging.DEBUG)
f.setFormatter(syslogFormatter)
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
