import apt, apt_pkg, commands, os, os.path, re, sys, urllib

class AptSetup(object):
    @classmethod
    def setup_class(cls):
      apt_pkg.init()
      cls.cache    = apt.Cache()
      cls.cache.update()
      cls.pkgCache = apt_pkg.GetCache()
      cls.depcache = apt_pkg.GetDepCache(cls.pkgCache)

    @classmethod
    def teardown_class(cls):
      cls.cache    = None
      cls.pkgCache = None
      cls.depcache = None

class TestApt(AptSetup):
#   def test_sections(self):
#     # make sure main, hades & upstream are listed

  def test_opensource_libitem(self):
    package          = self.cache["untangle-libitem-opensource-package"]
    package._lookupRecord(True)
    record           = package._records.Record
    section          = apt_pkg.ParseSection(record)
    fileName         = sanitizeName(section["Filename"])
    versionedPackage = self.depcache.GetCandidateVer(self.pkgCache[self.name])
    packageFile      = versionedPackage.FileList[0][0]
    indexFile        = self.cache._list.FindIndex(packageFile)
    url              = indexFile.ArchiveURI(fileName)
    urllib.urlretrieve(url, "/dev/null") # try to get it
