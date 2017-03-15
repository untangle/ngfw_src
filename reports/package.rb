# -*-ruby-*-
# $Id$

reports = BuildEnv::SRC['reports']

NodeBuilder.makeNode(BuildEnv::SRC, 'reports', 'reports')

jt = [reports['src']]
ServletBuilder.new(reports, 'com.untangle.uvm.reports.jsp', ["reports/servlets/reports"], [], jt)
