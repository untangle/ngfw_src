#! /usr/bin/python

import apt, apt_pkg, os.path, re, sys, urllib
import optparse

# constants
ops = { '<=' : lambda x: x <= 0,
        '<'  : lambda x: x < 0,
        '=' :  lambda x: x == 0,
        '>'  : lambda x: x > 0,
        '>=' : lambda x: x >= 0 }

# functions
TMP_DIR    = '/tmp/foo'
SOURCES    = TMP_DIR + '/sources.list'
PREFS      = TMP_DIR + '/preferences'
ARCHIVES   = TMP_DIR + '/archives'
STATE      = TMP_DIR + '/varlibapt'
LISTS      = STATE + '/lists'
STATUS_DIR = TMP_DIR + '/varlibdpkg'
STATUS     = STATUS_DIR + '/status'

def initializeChrootedAptFiles():
  os.system('rm -fr ' + TMP_DIR)

  os.makedirs(TMP_DIR)
  os.makedirs(ARCHIVES + '/partial')
  os.makedirs(STATE)
  os.makedirs(LISTS + '/partial')
  os.makedirs(STATUS_DIR)

  # touch status file
  open(STATUS, 'w')
  
  # create sources.list file
  open(SOURCES, 'w').write('''
deb http://linux.csua.berkeley.edu/debian sarge main contrib non-free
deb http://security.debian.org/ sarge/updates main contrib non-free
#php5
deb http://people.debian.org/~dexter php5 woody
# backports
deb http://www.backports.org/debian sarge-backports main contrib non-free
# volatile
deb http://debian.domainmail.org/debian-volatile sarge/volatile main contrib non-free
#deb http://10.0.0.44/testing testing untangle
#deb http://10.0.0.44/dev testing untangle\n''')

  # create preferences files
  open(PREFS, 'w').write('''
Package: *
Pin: origin 10.0.0.44
Pin-Priority: 700
Package: *
Pin: origin debian.domainmail.org
Pin-Priority: 680
Package: *
Pin: origin www.backports.org
Pin-Priority: 650
Package: *
Pin: origin linux.csua.berkeley.edu
Pin-Priority: 600\n''')


def initializeChrootedApt():
  apt_pkg.InitConfig()
  apt_pkg.InitSystem()
  apt_pkg.Config.Set("Dir::Etc::sourcelist", SOURCES)
#  apt_pkg.Config.Set("Dir::Etc::preferences", PREFS)
  apt_pkg.Config.Set("Dir::Cache::archives", ARCHIVES)
  apt_pkg.Config.Set("Dir::State", STATE)
  apt_pkg.Config.Set("Dir::State::Lists",  LISTS)
  apt_pkg.Config.Set("Dir::State::status", STATUS)

# this needs to be called before the classes are declared, since the static
# variables in them are initialized right away

print "Initializing chrooted apt"
initializeChrootedAptFiles()
initializeChrootedApt()

# classes
class Package:

  cache      = apt.Cache()

  print "Updating cache..."
  cache.update()
  cache.open(apt.progress.OpProgress())

  pkgCache      = apt_pkg.GetCache()
  depcache      = apt_pkg.GetDepCache(pkgCache)

  dependsKey    = 'Depends'
  suggestsKey   = 'Suggests'
  recommendsKey = 'Recommends'

  basePackages  = ()
  
#  basePackages = ( 'libc6', 'debconf', 'libx11-6', 'xfree86-common',
#                   'debianutils', 'zlib1g' )

  def __init__(self, name, version = None, fileName = None):
    self.name     = name
    self.version  = version
    self.fileName = fileName

  def __str__(self):
    return "%s %s" % (self.name, self.version)
      
  def __hash__(self):
    return self.name.__hash__()

  def __eq__(self, p):
    if not type(self) == type(p):
      return False
    return (self.name == p.name and self.version == p.version)

class VersionedPackage(Package):

  def __init__(self, name, version = None, fileName = None):
    Package.__init__(self, name, version, fileName)

    # FIXME
    self.isVirtual               = False
    self.foundDeps             = False
    self.foundAllDeps          = False

    if not self.version:
      try:
        self._package          = Package.cache[name]
        self._package._lookupRecord(True)
        self._record           = self._package._records.Record
        self._section          = apt_pkg.ParseSection(self._record)
        self.version           = self._section['Version']
        self.isRequired        = self._section['Priority'] == 'required'
        self.isImportant       = self._section['Priority'] == 'important'
        self.fileName          = self._sanitizeName(self._section["Filename"])
        
        self._versionedPackage = Package.depcache.GetCandidateVer(\
          Package.pkgCache[self.name])

        packageFile = self._versionedPackage.FileList[0][0]
        indexFile = Package.cache._list.FindIndex(packageFile)
        self.url = indexFile.ArchiveURI(self.fileName)
      except KeyError: # FIXME
        print "ooops, couldn't find package %s" % self.name
        self.isVirtual = True

  def _sanitizeName(self, name):
    return name.replace('%3a', ':')

  def getName(self):
    return self.name

  def getVersionedPackage(self):
    return self._versionedPackage
  
  def getDependsList(self, extra = None):
    if self.foundDeps:
      return self.deps
    
    if self.isVirtual or self.isRequired or self.isImportant:
      return []
    deps = self._versionedPackage.DependsList
    if Package.dependsKey in deps:
#      self.deps = [ DepPackage(self.name) ]
      self.deps = []
#      print [ p for p in deps[Package.dependsKey] ]
      intermediate = deps[Package.dependsKey]
      if extra:
        if Package.recommendsKey in deps:
          intermediate += deps[Package.recommendsKey]
        if Package.suggestsKey in deps:
          intermediate += deps[Package.suggestsKey]
        
      for p in [ p[0] for p in intermediate ]:
        name = p.TargetPkg.Name
        if not name in Package.basePackages:
          self.deps.append(DepPackage(name, p.TargetVer, p.CompType))
#      print "%s --> %s" % (self.name, [ str(p) for p in self.deps ])
    else:
      self.deps = []

    self.foundDeps = True
    return self.deps

  def _getAllDeps(self, deps = set(), extra = None):
    for p in self.getDependsList(extra):
      if not p in deps:
#        print "%s is a dep of %s" % (p, self)
        deps.add(p)
        for p1 in VersionedPackage(p.name)._getAllDeps():
          if not p1 in deps:
#            print "%s is a dep of %s" % (p, self)            
            deps.add(p1)

    return deps

  def getAllDeps(self):
    if self.isVirtual:
      return []
    if not self.foundAllDeps:
      # set extra to True to get recommends/suggest
      # FIXME: make this a CL option
      self.allDeps = self._getAllDeps(extra = None)
      self.allDeps.add(DepPackage(self.name))
      self.foundAllDeps = True
    return self.allDeps

  def satisfies(self, depPkg):
    if not depPkg.comp:
      return True
    r = apt_pkg.VersionCompare(self.version, depPkg.version)
    result = apply( ops[depPkg.comp], (r,) )
    print "compared package %s: %s to %s -> %s" % (depPkg.name,
                                                   self.version,
                                                   depPkg.version,
                                                   result)
    return result
  
  def getURL(self):
    return self.url

  def download(self, name = None):
    if not name:
      name = os.path.basename(self.fileName)      
    print "%s --> %s" % (self.url, name)
    urllib.urlretrieve(self.url, name)

class DepPackage(Package):

  def __init__(self, name, version = None, comp = None):
    Package.__init__(self, name)
    self.version = version
    self.comp = comp

  def __str__(self):
    return "%s %s %s" % (self.name,
                         self.comp,
                         self.version)

  def __hash__(self):
    return self.__str__().__hash__()

  def __eq__(self, p):
    if not type(self) == type(p):
      return False
    elif self.name == p.name and self.comp == p.comp \
             and self.version == p.version:
      return True
    else:
      return False

class UntangleStore:

  reObj = re.compile(r'([^_]+)_([^_]+)_[^\.]+\.deb')

  def __init__(self, basedir):
    self.basedir = basedir
    self.pkgs = {}
    for root, dirs, files in os.walk(basedir):
      if root.count('/.svn'):
        continue
      for f in files:
        m = UntangleStore.reObj.match(f)
        if m:
#          print "Found in store: %s (%s)" % (m.group(1), m.group(2))
          self.pkgs[m.group(1)] = VersionedPackage(m.group(1),
                                                   m.group(2),
                                                   os.path.join(root, f))

  def add(self, pkg):
    self.pkgs[pkg.name] = pkg

  def has(self, pkg):
    return pkg.name in self.pkgs

  def getByName(self, name):
    return self.pkgs[name]

  def get(self, pkg):
    return self.pkgs[pkg.name]

  def __str__(self):
    s = ""
    for p in self.pkgs.values():
      s += "%s\n" % p
    return s[:-1]


def parseCommandLineArgs(args):
  usage = "usage: %prog [options] <package> [<package>,...]"

  parser = optparse.OptionParser(usage=usage)
  parser.add_option("-f", "--force-download", dest="forceDownload",
                    action="store_true", default=False,
                    help="Force download of all dependencies" )
  
  options, args = parser.parse_args(args)
  
  if len(args) == 0:
    parser.error("Wrong number of arguments")
  else:
    pkgs = args
    
  return pkgs, options

# main
pkgs, options = parseCommandLineArgs(sys.argv[1:])
us = UntangleStore(os.path.join(sys.path[0], '../other'))

for arg in pkgs:
  pkg = VersionedPackage(arg)

  for p in pkg.getAllDeps():
    try:
#      print p.name
      versionedPackage = VersionedPackage(p.name)

      if (versionedPackage.isVirtual or versionedPackage.isRequired or versionedPackage.isImportant) and not options.forceDownload:
        print "%s won't be downloaded since --force-download wasn't used." % p.name
        continue

      if not us.has(versionedPackage):
        print "Package %s is missing" % p.name
      elif us.has(versionedPackage) and not us.get(versionedPackage).satisfies(p):
        print "Version of %s doesn't satisfy dependency (%s)" % (us.get(versionedPackage), p)
        print "Downloading new one, but you probably want to remove the older one (%s)" % us.getByName(p.name)
      else:
        continue
      
      versionedPackage.download()
      us.add(versionedPackage)

    except:
      print p, type(p), p.name, dir(p)
#      sys.exit(1)
  #  else:
  #    print "%s is in the store and satisfies the dependency" % us.get(versionedPackage)

os.system('rm -fr ' + TMP_DIR)
