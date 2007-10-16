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

nfutil = "#{BuildEnv::SRC.staging}/nfutil"

source = FileList["#{SRC_HOME}/nfutil/src/*.c"]

## Make the binary dependent on all of the source files.
source.each { |f| file nfutil => f }

file nfutil do
  compilerEnv = CCompilerEnv.new()

  CBuilder.new(BuildEnv::SRC, compilerEnv).makeBinary(source, nfutil,
                                                      [], ['netfilter_queue'])
end

BuildEnv::SRC.installTarget.registerDependency(nfutil)
BuildEnv::SRC.installTarget.installFiles(nfutil, "#{BuildEnv::SRC['untangle-libuvm'].distDirectory}/usr/share/untangle/networking/")
