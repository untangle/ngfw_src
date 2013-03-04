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

badness = HexColor(0xDD0000)
detected=HexColor(0xFF8000)
goodness = HexColor(0x00AA00)
blue = HexColor(0x3333FF)

color_palette = [HexColor(0x00AA00), HexColor(0x0066B3), HexColor(0xFFCC00),
                 HexColor(0x330099), HexColor(0x990099), HexColor(0xCCFF00),
                 HexColor(0x008F00), HexColor(0x00487D), HexColor(0xAA0000),
                 HexColor(0x8FB300), HexColor(0x80FF80), HexColor(0x80C9FF),
                 HexColor(0xFFC080), HexColor(0xFFE680), HexColor(0xAA80FF),
                 HexColor(0xEE00CC), HexColor(0xFF8080), HexColor(0x666600),
                 HexColor(0xFFBFFF), HexColor(0x00FFCC), HexColor(0xCC6699),
                 HexColor(0x999900)]
