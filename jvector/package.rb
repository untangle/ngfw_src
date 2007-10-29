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

jvector = BuildEnv::SRC['jvector']
jnetcap = BuildEnv::SRC['jnetcap']
uvm_lib    = BuildEnv::SRC['untangle-libuvm']

## jvector
deps = Jars::Base + [jnetcap['impl']]
j = JarTarget.build_target(jvector, deps, 'impl', "./jvector/impl")
BuildEnv::SRC.installTarget.install_jars(j, "#{uvm_lib.distDirectory}/usr/share/untangle/lib",
                                        nil, false, true)

headerClasses = [ 'com.untangle.jvector.OutgoingSocketQueue',
  'com.untangle.jvector.IncomingSocketQueue',
  'com.untangle.jvector.Relay',
  'com.untangle.jvector.Vector',
  'com.untangle.jvector.Sink',
  'com.untangle.jvector.Source',
  'com.untangle.jvector.TCPSink',
  'com.untangle.jvector.TCPSource',
  'com.untangle.jvector.UDPSource',
  'com.untangle.jvector.UDPSink',
  'com.untangle.jvector.Crumb' ]

javah = JavahTarget.new(jvector, j, headerClasses)

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::JVector}" })


ArchiveTarget.build_target(jvector, [BuildEnv::SRC['libmvutil'], BuildEnv::SRC['jmvutil'], javah],
                          compilerEnv, ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::SRC.installTarget => jvector
