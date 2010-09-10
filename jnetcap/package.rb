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

jnetcap = BuildEnv::SRC['jnetcap']
uvm_lib    = BuildEnv::SRC['untangle-libuvm']

## jnetcap
j = JarTarget.build_target(jnetcap, Jars::Base, 'impl', "./jnetcap/impl" )
BuildEnv::SRC.installTarget.install_jars(j, "#{uvm_lib.distDirectory}/usr/share/untangle/lib",
                                        nil, false, true)

headerClasses = [ 'com.untangle.jnetcap.Netcap',
  'com.untangle.jnetcap.IPTraffic',
  'com.untangle.jnetcap.NetcapUDPSession',
  'com.untangle.jnetcap.NetcapSession',
  'com.untangle.jnetcap.NetcapTCPSession' ]

javah = JavahTarget.new(jnetcap, j, headerClasses)

compilerEnv = CCompilerEnv.new({'pkg' => "#{CCompilerEnv::JNetcap}" })


## jnetcap
ArchiveTarget.build_target(jnetcap, [ BuildEnv::SRC['libmvutil'], BuildEnv::SRC['jmvutil'], javah ], compilerEnv,
                          ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::SRC.installTarget => jnetcap
