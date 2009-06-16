import commands, os, sys, xml.dom.minidom

for path, dirs, files in os.walk("./dist/usr/share/untangle/web/reports/data"):
  for f in files:
    if f == "report.xml":
      f = os.path.join(path, f)
      md = xml.dom.minidom.parse(f)

      try:
        sql = md.getElementsByTagName("sql")[0].childNodes[0].data
      except:
        continue

      status, output = commands.getstatusoutput("echo \"%s\" | psql -v ON_ERROR_STOP=1 -U postgres uvm" % (sql,))

      if status != 0:
        print "=" * 72
        print "Error in report: %s" % (f,)
        print output
