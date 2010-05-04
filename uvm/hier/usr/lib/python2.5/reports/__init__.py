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
# Sebastien Delafond <seb@untangle.com>

import codecs
import cStringIO
import csv
import gettext
import logging
import mx
import os
import re
import reportlab.lib.colors
import string
import sys

from lxml.etree import CDATA
from lxml.etree import Element
from lxml.etree import ElementTree
from mx.DateTime import DateTimeDeltaFromSeconds
from reportlab.graphics.shapes import Rect
from reportlab.lib.colors import HexColor
from reportlab.lib.colors import Color
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import Paragraph
from reportlab.platypus import Spacer
from reportlab.platypus.flowables import PageBreak
from reportlab.platypus.flowables import Image
from reportlab.platypus.flowables import KeepTogether
from reportlab.platypus.tables import Table
from reportlab.platypus.tables import TableStyle

import reports.colors
import reports.sql_helper as sql_helper
import reports.i18n_helper

from reports.engine import get_node, get_node_base
from reports.pdf import STYLESHEET
from reports.pdf import SectionHeader

from reports.log import *
logger = getLogger(__name__)

HNAME_LINK = 'HostLink'
USER_LINK = 'UserLink'
EMAIL_LINK = 'EmailLink'
URL_LINK = 'URLLink'

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext

def __time_of_day_formatter(x, pos):
    t = DateTimeDeltaFromSeconds(x)
    return "%02d:%02d" % (t.hour, t.minute)

def __date_formatter(x, pos):
    return x.strftime("%b-%d")

def __timestamp_formatter(x, pos):
    return x.strftime("%Y-%m-%d %H:%M")

def __identity_formatter(x, pos):
    return x

class UTF8Recoder:
    """
    Iterator that reads an encoded stream and reencodes the input to UTF-8
    """
    def __init__(self, f, encoding):
        self.reader = codecs.getreader(encoding)(f)

    def __iter__(self):
        return self

    def next(self):
        return self.reader.next().encode("utf-8")

class UnicodeReader:
    """
    A CSV reader which will iterate over lines in the CSV file "f",
    which is encoded in the given encoding.
    """

    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        f = UTF8Recoder(f, encoding)
        self.reader = csv.reader(f, dialect=dialect, **kwds)

    def next(self):
        row = self.reader.next()
        return [unicode(s, "utf-8") for s in row]

    def __iter__(self):
        return self

class UnicodeWriter:
    """
    A CSV writer which will write rows to CSV file "f",
    which is encoded in the given encoding.
    """

    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        # Redirect output to a queue
        self.queue = cStringIO.StringIO()
        self.writer = csv.writer(self.queue, dialect=dialect, **kwds)
        self.stream = f
        self.encoding = encoding

    def writerow(self, row):
        self.writer.writerow([unicode(s).encode("utf-8") for s in row])        
        # Fetch UTF-8 output from the queue ...
        data = self.queue.getvalue()
        data = data.decode("utf-8")
#         # ... and reencode it into the target encoding
#         data = data.encode(self.encoding)
        # write to the target stream
        self.stream.write(data)
        # empty queue
        self.queue.truncate(0)

    def writerows(self, rows):
        for row in rows:
            self.writerow(row)


class Formatter:
    def __init__(self, name, function):
        self.__name = name
        self.__function = function

    @property
    def name(self):
        return self.__name

    @property
    def function(self):
        return self.__function

TIME_OF_DAY_FORMATTER = Formatter('time-of-day', __time_of_day_formatter)
DATE_FORMATTER = Formatter('date', __date_formatter)
TIMESTAMP_FORMATTER = Formatter('timestamp', __timestamp_formatter)
IDENTITY_FORMATTER = Formatter('identity', __identity_formatter)

TIME_SERIES_CHART = 'time-series-chart'
STACKED_BAR_CHART = 'stacked-bar-chart'
PIE_CHART = 'pie-chart'

class Report:
    def __init__(self, node, sections):
        self.__name = node.name
        self.__title = node.display_title
        self.__view_position = node.view_position
        self.__sections = sections

    @property
    def title(self):
        return self.__title

    @property
    def name(self):
        return self.__name

    @property
    def sections(self):
        return self.__sections

    @property
    def view_position(self):
        return self.__view_position

    def generate(self, report_base, date_base, end_date, report_days=1,
                 host=None, user=None, email=None):
        logger.info("About to generate report for %s: host='%s', user='%s', email='%s'" % (self.__name,
                                                                                           host,
                                                                                           user,
                                                                                           email))
        
        node_base = get_node_base(self.__name, date_base,
                                  report_days=report_days, host=host, user=user,
                                  email=email)

        element = Element('report')
        element.set('name', self.__name)
        element.set('title', self.__title)
        if host:
            element.set('host', host)
        if user:
            element.set('user', user)
        if email:
            element.set('email', email)

        for s in self.__sections:
            section_element = s.generate(report_base, node_base, end_date,
                                         report_days=report_days, host=host,
                                         user=user, email=email)

            if section_element is not None:
                element.append(section_element)

        if len(element.getchildren()):
            tree = ElementTree(element)

            if not os.path.exists('%s/%s' % (report_base, node_base)):
                os.makedirs('%s/%s' % (report_base, node_base))

            report_file = "%s/%s/report.xml" % (report_base, node_base)

            logger.info('writing %s' % (report_file,))
            tree.write(report_file, encoding='utf-8', pretty_print=True,
                       xml_declaration=True)

    def get_flowables(self, report_base, date_base, end_date, report_days=1):
        node_base = get_node_base(self.__name, date_base,
                                  report_days=report_days)

        sh = SectionHeader(self.__title)

        story = [sh, Spacer(1, 0.25 * inch)]

        for s in self.__sections:
            story += s.get_flowables(report_base, node_base, end_date)

        return story

class Section:
    def __init__(self, name, title):
        self.__name = name
        self.__title = title
        if self.__title: # FIXME: why ???
            self.__title = self.__title.decode('utf-8')

    @property
    def name(self):
        return self.__name

    @property
    def title(self):
        return self.__title

    def generate(self, report_base, node_base, end_date, report_days=1,
                 host=None, user=None, email=None):
        pass

    def get_flowables(self, report_base, date_base, end_date):
        return []

class SummarySection(Section):
    def __init__(self, name, title, summary_items=[]):
        Section.__init__(self, name, title)

        self.__summary_items = summary_items

    @property
    def summary_items(self):
        return self.__summary_items

    def generate(self, report_base, node_base, end_date, report_days=1,
                 host=None, user=None, email=None):
        section_base = "%s/%s" % (node_base, self.name)

        element = Element('summary-section')
        element.set('name', self.name)
        element.set('title', self.title)

        for summary_item in self.__summary_items:
            report_element = summary_item.generate(report_base, section_base,
                                                   end_date,
                                                   report_days=report_days,
                                                   host=host, user=user,
                                                   email=email)
            if report_element is not None:
                element.append(report_element)

        if len(element.getchildren()):
            return element
        else:
            return None

    def get_flowables(self, report_base, node_base, end_date):
        section_base = "%s/%s" % (node_base, self.name)

        story = []

        for si in self.__summary_items:
            story += si.get_flowables(report_base, section_base, end_date)
            story.append(PageBreak())

        return story

class DetailSection(Section):
    def __init__(self, name, title):
        Section.__init__(self, name, title)

    def get_columns(self, host=None, user=None, email=None):
        pass

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        pass

    def generate(self, report_base, node_base, end_date, report_days=1,
                 host=None, user=None, email=None):
        element = Element('detail-section')
        element.set('name', self.name)
        element.set('title', self.title)

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        sql = self.get_sql(start_date, end_date, host, user, email)

        if not sql:
            logger.warn('no sql for DetailSection: %s' % self.name)
            sql = ''

        sql_element = Element('sql')
        sql_element.text = CDATA(sql)
        element.append(sql_element)

        columns = self.get_columns(host, user, email)
        if not columns:
            logger.warn('no columns for DetailSection: %s' % self.name)
            columns = []

        for c in columns:
            element.append(c.get_dom())

        return element

    def get_flowables(self, report_base, date_base, end_date):
        return []

    def write_csv(self, filename, start_date, end_date, host=None, user=None,
                  email=None):
        logger.debug('Generating CSV for %s' % (self.name,))

        sql = self.get_sql(start_date, end_date, host=None, user=None,
                           email=None)
        f = codecs.open(filename, 'w', 'utf-8')
        w = UnicodeWriter(f)
        conn = sql_helper.get_connection()

        try:
            r = []
            for c in self.get_columns(host=None, user=None, email=None):
                r.append(c.title or '')
            w.writerow(r)

            curs = conn.cursor()
            curs.execute(sql)

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                r1 = []
                for el in r: # strip off milliseconds
                    if isinstance(el, mx.DateTime.DateTimeType):
                        r1.append(el.strftime("%Y-%m-%d %H:%M:%S"))
                    else:
                        r1.append(el)
                w.writerow(r1)
        except Exception:
            logger.error("error adding details for '%s'" % self.name, exc_info=True)
        finally:
            f.close()
            conn.commit()

class ColumnDesc():
    def __init__(self, name, title, type=None):
        self.__name = name

        self.__title = title
        if self.__title: # FIXME: why ???
            self.__title = self.__title.decode('utf-8')

        self.__type = type

    @property
    def title(self):
        return self.__title

    def get_dom(self):
        element = Element('column')
        element.set('name', self.__name)
        element.set('title', self.__title or '')
        if self.__type:
            element.set('type', self.__type)

        return element

class Highlight:
    def __init__(self, node_name, string_template):
        logger.debug("About to generate highlight for %s" % (node_name,))
        self.__name = node_name
        self.__title = get_node(node_name).display_title
        self.__string_template = string_template.replace(self.__name,
                                                         self.__title)

        self.__values = ()

    @property
    def name(self):
        return self.__name

    def generate(self, report_base, section_base,
                 end_date, report_days=1,
                 host=None, user=None, email=None):
        try:
            self.__values = self.get_highlights(end_date, report_days,
                                                host, user, email)
        except:
            logger.error("could not generate highlight: %s" \
                         % (self.name,), exc_info=True)
            self.__values = ()

        if not self.__values:
            return None

        element = Element('highlight')
        element.set('name', self.__name)
        element.set('string-template', self.__string_template)

        for k,v in self.__values.iteritems():
            value_element = Element('highlight-value')

            try:
                k = k.decode('utf-8')
            except:
                pass

            try:
                value_element.set('name', k)
            except:
                logger.critical("Could not set name to '%s' (type '%s')" % (k, type(k)))
                raise

            value_element.set('value', str(v))
            element.append(value_element)

        return element

    def get_flowables(self, report_base, section_base, end_date):
        data = [[Paragraph(_('Highlights'),
                           STYLESHEET['TableTitle']),]]

        data.append([Paragraph(self.get_string(), STYLESHEET['Smaller']),])

        table = Table(data)

        return [table,]

    def get_string(self):
        h = {}
        for k, v in self.__values.iteritems():
            h[k] = "<b>%s</b>" % (v,)
        return (self.__string_template % h).replace(self.__title,
                                                    "<b>%s</b>" % (self.__title,))


class Graph:
    def __init__(self, name, title):
        self.__name = name
        self.__title = title

        self.__plot = None
        self.__key_statistics = []

    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        return (self.get_key_statistics(end_date, report_days, host, user, email),
                self.get_plot(end_date, report_days, host, user, email))

    def get_key_statistics(self, report_days, end_date, host=None, user=None,
                           email=None):
        return []

    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        return None

    @property
    def name(self):
        return self.__name

    @property
    def title(self):
        return self.__title

    def generate(self, report_base, section_base, end_date, report_days=1,
                 host=None, user=None, email=None):

        try:
            graph_data = self.get_graph(end_date, report_days, host, user,
                                        email)
        except:
            logger.warn("could not generate graph: %s (%s)" \
                             % (self.name, self.title), exc_info=True)
            return None

        if not graph_data:
            return None

        self.__key_statistics, self.__plot = graph_data

        if not self.__plot:
            return None

        if not self.__key_statistics:
            self.__key_statistics = []

        filename_base = '%s-%s' % (section_base, self.__name)

        dir = os.path.dirname('%s/%s' % (report_base, filename_base))
        if not os.path.exists(dir):
            os.makedirs(dir)

        self.__plot.generate_csv('%s/%s.csv' % (report_base, filename_base))

        element = Element('graph')
        element.set('name', self.__name)
        element.set('title', self.__title)
        element.set('type', self.__plot.type)
        element.set('image', filename_base + '.png')
        element.set('csv', filename_base + '.csv')

        for ks in self.__key_statistics:
            ks_element = Element('key-statistic')
            name = ks.name
            if not ks.name:
                name = ''

            try:
                name = name.decode('utf-8')
            except:
                pass

            try:
                ks_element.set('name', name)
            except:
                logger.critical("Could not set name to '%s' (type '%s')" % (name, type(name)))
                raise

            if type(ks.value) == float:
                ks.value = '%.2f' % ks.value
            ks_element.set('value', str(ks.value))
            if ks.unit:
                ks_element.set('unit', ks.unit)
            if ks.link_type:
                ks_element.set('link-type', ks.link_type)
            element.append(ks_element)

        element.append(self.__plot.get_dom())

        return element

    def get_flowables(self, report_base, section_base, end_date):
        img_file = '%s/%s-%s.png' % (report_base, section_base, self.__name)
        if not os.path.exists(img_file):
            logger.warn('skipping summary for missing png: %s' % img_file)
            return []
        image = Image(img_file)

        zebra_colors = [HexColor(0xE0E0E0), None]

        if self.__plot and self.__plot.type == PIE_CHART:
            colors = self.__plot.colors
            background_colors = [None]
            data = [[Paragraph(_('Key Statistics'), STYLESHEET['TableTitle']),
                     '', '']]
        else:
            colors = None
            background_colors = zebra_colors
            data = [[Paragraph(_('Key Statistics'), STYLESHEET['TableTitle']),
                     '']]

        for i, ks in enumerate(self.__key_statistics):
            n = ks.name
            if colors:
                val, unit = ks.scaled_value
                if unit:
                    data.append(['', n, "%s %s" % (val, unit)])
                else:
                    data.append(['', n, "%s" % val])
                c = colors.get(n, None)
                if c:
                    background_colors.append(c)
                else:
                    background_colors.append(zebra_colors[(i + 1) % 2])
            else:
                data.append([n, "%s %s" % ks.scaled_value])

        style = []

        if colors:
            style.append(['ROWBACKGROUNDS', (0, 0), (0, -1), background_colors])
            style.append(['ROWBACKGROUNDS', (1, 0), (-1, -1), zebra_colors])
            style.append(['SPAN', (0, 0), (2, 0)])
        else:
            style.append(['SPAN', (0, 0), (1, 0)])
            style.append(['ROWBACKGROUNDS', (0, 1), (-1, -1), zebra_colors])

        style += [['BACKGROUND', (0, 0), (-1, 0), reportlab.lib.colors.grey],
                  ['BOX', (0, 0), (-1, -1), 1, reportlab.lib.colors.grey]]

        ks_table = Table(data, style=style)

        return [image, Spacer(1, 0.125 * inch), ks_table]

class Chart:
    def __init__(self, type=TIME_SERIES_CHART, title=None, xlabel=None,
                 ylabel=None, major_formatter=IDENTITY_FORMATTER,
                 required_points=[]):
        self.__type = type

        self.__title = title
        if self.__title: # FIXME: why ???
            self.__title = self.__title.decode('utf-8')
        self.__xlabel = xlabel
        if self.__xlabel: # FIXME: why ???
            self.__xlabel = self.__xlabel.decode('utf-8')
        self.__ylabel = ylabel
#         if self.__ylabel: # FIXME: why ???
#             self.__ylabel = self.__ylabel.decode('utf-8')
            
        self.__major_formatter = major_formatter

        self.__datasets = []

        self.__header = [xlabel]

        self.__colors = {}
        self.__color_num = 0

        self.__required_points = required_points

        self.__display_limit = None

    @property
    def type(self):
        return self.__type

    @property
    def colors(self):
        return self.__colors

    def add_dataset(self, xdata, ydata, label=None, color=None, linestyle='-'):
        if self.__type == PIE_CHART:
            raise ValueError('using 2D dataset for pie chart')

        if not color:
            color = reports.colors.color_palette[self.__color_num]
            self.__color_num += 1

        label = str(label)

        m = {'xdata': xdata, 'ydata': ydata, 'label': label,
             'linestyle': linestyle, 'color': color}
        self.__datasets.append(m)

        self.__header.append(label)

        self.__colors[label] = color

    def add_pie_dataset(self, data, colors={}, display_limit=None):
        self.__display_limit = display_limit

        if self.__type != PIE_CHART:
            raise ValueError('using pie dataset for non-pie chart')

        for k, v in colors.iteritems():
            self.__colors[str(k)] = v

        items = []

        for k, v in data.iteritems():
            items.append([k, v])

        items.sort(cmp=self.__pie_sort, reverse=True)

        self.__datasets = {}

        c = 0

        for i in items:
            k = str(i[0])
            self.__datasets[k] = i[1]

            if ((not display_limit or c < display_limit)
                and (not self.__colors.has_key(k)
                     and self.__color_num < len(reports.colors.color_palette))):
                self.__colors[k] = reports.colors.color_palette[self.__color_num]
                self.__color_num += 1
                c += 1

    def generate_csv(self, filename, host=None, user=None, email=None):
        if self.__type == PIE_CHART:
            self.__generate_pie_csv(filename, host=host, user=user, email=email)
        else:
            self.__generate_2d_csv(filename, host=host, user=user, email=email)

    def get_dom(self):
        element = Element('plot')
        if self.__type:
            element.set('type', self.__type)
        if self.__title:
            element.set('title', self.__title)
        if self.__xlabel:
            element.set('x-label', self.__xlabel)
        if self.__ylabel:
            element.set('y-label', self.__ylabel)
        if self.__major_formatter:
            element.set('major-formatter', self.__major_formatter.name)
        if self.__display_limit:
            element.set('display-limit', str(self.__display_limit))

        for t, c in self.__colors.iteritems():
            ce = Element('color')
            ce.set('title', str(t).decode('utf-8'))
            ce.set('value', "%02x%02x%02x" % c.bitmap_rgb())
            element.append(ce)

        return element

    def __generate_2d_csv(self, filename, host=None, user=None, email=None):
        data = {}
        z = 0

        for ds in self.__datasets:
            for x, y in zip(ds['xdata'], ds['ydata']):
                a = data.get(x, None)
                if a:
                    a[z] = y
                else:
                    a = [0 for i in range(len(self.__datasets))]
                    a[z] = y
                    data[x] = a
            z = z + 1
            
        for x in self.__required_points:
            a = data.get(x, None)
            if not a:
                a = [0 for i in range(len(self.__datasets))]
                data[x] = a

        rows = [[k] + v for (k, v) in data.iteritems()]
        rows.sort()

        w = csv.writer(open(filename, 'w'))
        w.writerow(self.__header)
        for r in rows:
            if self.__major_formatter:
                r[0] = self.__major_formatter.function(r[0], None)
            for i, e in enumerate(r):
                if e is None:
                    r[i] = 0
            w.writerow(r)

    def __generate_pie_csv(self, filename, host=None, user=None, email=None):
        items = []

        for k, v in self.__datasets.iteritems():
            items.append([k, v])

        items.sort(cmp=self.__pie_sort, reverse=True)

        w = csv.writer(open(filename, 'w'))
        w.writerow([_('slice'), _('value')])
        for e in items:
            k = e[0]
            v = e[1]
            if k is None:
                k = 'None'
            if v is None:
                v = 0
            w.writerow([k, v])

    def __pie_sort(self, a, b):
        return cmp(a[1], b[1])

class KeyStatistic:
    def __init__(self, name, value, unit=None, link_type=None):
        if name is None:
            logger.warn('KeyStatistic name is None')
            name = _("Unknown")
        if value is None:
            logger.warn('KeyStatistic for %s value is None' % name)
            value = 0

        self.__name = name
        self.__value = value
        self.__unit = unit
        self.__link_type = link_type

    @property
    def scaled_value(self):
        if self.__unit is None:
            return (self.__value, self.__unit)
        if self.__unit.startswith('bytes'):
            if self.__value < 1000000:
                s = string.split(self.__unit, '/')
                s[0] = _('KB')
                return ('%.2f' % (self.__value / 1000.0), string.join(s, '/'))
            elif self.__value < 1000000000:
                s = string.split(self.__unit, '/')
                s[0] = _('MB')
                return ('%.2f' % (self.__value / 1000000.0), string.join(s, '/'))
            else:
                s = string.split(self.__unit, '/')
                s[0] = _('GB')
                return ('%.2f' % (self.__value / 1000000000.0), string.join(s, '/'))
        elif type(self.__value) is float:
            return ('%.2f' % self.__value, self.__unit)
        else:
            return (self.__value, self.__unit)

    @property
    def name(self):
        return self.__name

    @property
    def value(self):
        return self.__value

    @property
    def unit(self):
        return self.__unit

    @property
    def link_type(self):
        return self.__link_type
