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
#
# Aaron Read <amread@untangle.com>

import reportlab.lib.colors as colors

from reportlab.lib.colors import HexColor

badness = colors.red
goodness = colors.green

color_palette = [HexColor('0x00FFFF'), HexColor('0x808080'),
                 HexColor('0x000080'), HexColor('0xC0C0C0'),
                 HexColor('0x000000'), HexColor('0x008000'),
                 HexColor('0x808000'), HexColor('0x008080'),
                 HexColor('0x0000FF'), HexColor('0x00FF00'),
                 HexColor('0x800080'), HexColor('0xFFFFFF'),
                 HexColor('0xFF00FF'), HexColor('0x800000')]
