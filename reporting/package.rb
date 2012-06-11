# -*-ruby-*-
# $Id$

reporting = BuildEnv::SRC['untangle-node-reporting']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-reporting', 'reporting')

jt = [reporting['api']]
ServletBuilder.new(reporting, 'com.untangle.uvm.reports.jsp', "reporting/servlets/reports", [], jt)
