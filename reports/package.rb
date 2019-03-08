# -*-ruby-*-
# $Id$

reports = BuildEnv::SRC['untangle-app-reports']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-reports', 'reports')

jt = [reports['src']]
ServletBuilder.new(reports, 'com.untangle.uvm.reports.jsp', ["reports/servlets/reports"], [], jt)
