import logging, sys

# line format
format = '%(asctime)s [%(name)s] %(levelname)s: %(message)s'
dateFmt = '%Y-%m-%d %H:%M:%S'
formatter = logging.Formatter(format, dateFmt)

def getLogger(name, console = True):
  # file logging
  f = logging.FileHandler("%s/var/log/uvm/%s.log" % ('@PREFIX@', name))
  f.setLevel(logging.DEBUG)
  f.setFormatter(formatter)
  logging.getLogger('').addHandler(f)

  # stdout logging
  if console:
    console = logging.StreamHandler(sys.stdout)
    console.setLevel(logging.INFO)
    console.setFormatter(formatter)
    logging.getLogger('').addHandler(console)

  logger = logging.getLogger(name)
  logger.setLevel(logging.DEBUG)
  return logger
