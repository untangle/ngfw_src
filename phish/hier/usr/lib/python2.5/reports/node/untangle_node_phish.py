# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
# Copyright (c) 2003-2009 Untangle, Inc.
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

import gettext
import reports.i18n_helper
import reports.node.untangle_base_spam

_ = reports.i18n_helper.get_translation('untangle-node-phish').lgettext

reports.engine.register_node(reports.node.untangle_base_spam.SpamBaseNode('untangle-node-phish', 'Phish', 'phish', 'Clam', _('Phish'), _('Clean'), _('Hourly Phish Rate'), _('Daily Phish Rate'), _('Top Ten Phishing Victims')))
