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

    def sanitizeName(cls, name):
      return name.replace('%3a', ':')

class TestApt(AptSetup):
  PKGS_MAIN = ("untangle-libitem-opensource-package", "untangle-vm")
  PKGS_PREMIUM = ("untangle-libitem-professional-package", "untangle-support-agent")
  PKGS_UPSTREAM = ("spamassassin", "clamav", "ssh")

  def test_package(self, pkg):
    package          = self.cache[pkg]
    package._lookupRecord(True)
    record           = package._records.Record
    section          = apt_pkg.ParseSection(record)
    fileName         = self.sanitizeName(section["Filename"])
    versionedPackage = self.depcache.GetCandidateVer(self.pkgCache[pkg])
    packageFile      = versionedPackage.FileList[0][0]
    indexFile        = self.cache._list.FindIndex(packageFile)
    url              = indexFile.ArchiveURI(fileName)

def pytest_generate_tests(metafunc):
  if "pkg" in metafunc.funcargnames:
    for pkgs in (TestApt.PKGS_MAIN, TestApt.PKGS_PREMIUM, TestApt.PKGS_UPSTREAM):
      for pkg in pkgs:
        metafunc.addcall(funcargs=dict(pkg=pkg))

