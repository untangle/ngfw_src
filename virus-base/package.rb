# -*-ruby-*-
# $HeadURL$
# Copyright (c) 2003-2007 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#

implDeps = []
guiDeps = []

%w(untangle-casing-mail untangle-casing-ftp untangle-casing-http).each do |c|
  implDeps << BuildEnv::SRC[c]['localapi']
end

virus = BuildEnv::SRC['untangle-base-virus']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-virus', 'virus-base', implDeps)

http = BuildEnv::SRC['untangle-casing-http']

deps = [virus['impl'], http['localapi']]

ServletBuilder.new(virus, 'com.untangle.node.virus.jsp',
                   "./virus-base/servlets/virus", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
