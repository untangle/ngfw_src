import jslint, os, os.path

class TestJS():
  def test_js(self, fullPath):
    pbs = jslint.Problems(fullPath)
    if not len(pbs.problems) == 0:
      print pbs
      assert False

def pytest_generate_tests(metafunc):
  if "fullPath" in metafunc.funcargnames:
    for root, dirs, files in os.walk('@PREFIX@/usr/share/untangle/web'):
      for f in files:
        if not f.endswith('.js') or f.endswith('-min.js'):
          continue
        metafunc.addcall(funcargs=dict(fullPath=os.path.join(root, f)))
