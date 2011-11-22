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

mail = BuildEnv::SRC['untangle-casing-mail']
http = BuildEnv::SRC['untangle-casing-http']
spam = BuildEnv::SRC['untangle-base-spam']
clam = BuildEnv::SRC['untangle-base-clam']
phish = BuildEnv::SRC['untangle-node-phish']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-phish', 'phish',
                     [mail['api'], http['api']],
                     [mail['api'], http['api']], { 'spam-base' => spam, 'clam-base' => clam })

deps = [http['api'], phish['impl'], phish['api'], spam['impl']]

ServletBuilder.new(phish, 'com.untangle.node.phish.jsp',
                   "./phish/servlets/phish", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
