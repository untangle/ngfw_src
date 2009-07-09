import csv
import gettext
import logging
import mx
import os
import popen2
import re
import reportlab.lib.colors as colors
import sql_helper
import string

from lxml.etree import CDATA
from lxml.etree import Element
from lxml.etree import ElementTree
from mx.DateTime import DateTimeDeltaFromSeconds
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import Paragraph
from reportlab.platypus import Spacer
from reportlab.platypus.flowables import Image
from reportlab.platypus.flowables import KeepTogether
from reportlab.platypus.tables import Table
from reportlab.platypus.tables import TableStyle
from reports.engine import ReportDocTemplate
from reports.engine import get_node_base
from reports.pdf import STYLESHEET

HNAME_LINK = 'HostLink'
USER_LINK = 'UserLink'
EMAIL_LINK = 'EmailLink'

_ = gettext.gettext
def N_(message): return message

def __time_of_day_formatter(x, pos):
    t = DateTimeDeltaFromSeconds(x)
    return "%02d:%02d" % (t.hour, t.minute)

def __date_formatter(x, pos):
    return "%d-%02d-%02d" % (x.year, x.month, x.day)

def __identity_formatter(x, pos):
    return x

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
IDENTITY_FORMATTER = Formatter('identity', __identity_formatter)

TIME_SERIES_CHART = 'time-series-chart'
STACKED_BAR_CHART = 'stacked-bar-chart'
PIE_CHART = 'pie-chart'

class Report:
    def __init__(self, name, sections):
        self.__name = name
        self.__title = self.__get_node_title(self.__name)
        self.__sections = sections

    def generate(self, report_base, date_base, end_date, host=None, user=None,
                 email=None):
        node_base = get_node_base(self.__name, date_base, host, user, email)

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
            section_element = s.generate(report_base, node_base, end_date, host,
                                         user, email)

            if section_element is not None:
                element.append(section_element)

        if len(element.getchildren()):
            tree = ElementTree(element)

            if not os.path.exists('%s/%s' % (report_base, node_base)):
                os.makedirs('%s/%s' % (report_base, node_base))

            report_file = "%s/%s/report.xml" % (report_base, node_base)

            logging.info('writing %s' % report_file)
            tree.write(report_file, encoding='utf-8', pretty_print=True,
                       xml_declaration=True)

    def get_flowables(self, report_base, date_base, end_date):
        node_base = get_node_base(self.__name, date_base)

        story = [Paragraph(self.__title, STYLESHEET['Heading1'])]

        for s in self.__sections:
            story += s.get_flowables(report_base, node_base, end_date)

        return story

    def __get_node_title(self, name):
        title = None

        (stdout, stdin) = popen2.popen2(['apt-cache', 'show', name])
        try:
            for l in stdout:
                m = re.search('Display-Name: (.*)', l)
                if m:
                    title = m.group(1)
                    break
        finally:
            stdout.close()
            stdin.close()

        return title

class Section:
    def __init__(self, name, title):
        self.__name = name
        self.__title = title

    @property
    def name(self):
        return self.__name

    @property
    def title(self):
        return self.__title

    def generate(self, report_base, node_base, end_date, host=None, user=None,
                 email=None):
        pass

    def get_flowables(self, report_base, date_base, end_date):
        return []

class SummarySection(Section):
    def __init__(self, name, title, summary_items=[]):
        Section.__init__(self, name, title)

        self.__summary_items = summary_items

    def generate(self, report_base, node_base, end_date, host=None, user=None,
                 email=None):
        section_base = "%s/%s" % (node_base, self.name)

        element = Element('summary-section')
        element.set('name', self.name)
        element.set('title', self.title)

        for summary_item in self.__summary_items:
            report_element = summary_item.generate(report_base, section_base,
                                                   end_date, host, user, email)
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
            story.append(Spacer(1, 0.2 * inch))

        return story

class DetailSection(Section):
    def __init__(self, name, title):
        Section.__init__(self, name, title)

    def get_columns(self, host=None, user=None, email=None):
        pass

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        pass

    def generate(self, report_base, node_base, end_date, host=None, user=None,
                 email=None):
        element = Element('detail-section')
        element.set('name', self.name)
        element.set('title', self.title)

        start_date = end_date - mx.DateTime.DateTimeDelta(1)
        sql = self.get_sql(start_date, end_date, host, user, email)

        if not sql:
            logging.warn('no sql for DetailSection: %s' % self.name)
            sql = ''

        sql_element = Element('sql')
        sql_element.text = CDATA(sql)
        element.append(sql_element)

        columns = self.get_columns(host, user, email)
        if not columns:
            logging.warn('no columns for DetailSection: %s' % self.name)
            columns = []

        for c in columns:
            element.append(c.get_dom())

        return element

    def get_flowables(self, report_base, date_base, end_date):
        return []

class ColumnDesc():
    def __init__(self, name, title, type=None):
        self.__name = name
        self.__title = title
        self.__type = type

    @property
    def title(self):
        return self.__title

    def get_dom(self):
        element = Element('column')
        element.set('name', self.__name)
        element.set('title', self.__title)
        element.set('type', self.__type)

        return element

class Graph:
    def __init__(self, name, title):
        self.__name = name
        self.__title = title

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

    def generate(self, report_base, section_base, end_date, host=None,
                 user=None, email=None):
        graph_data = self.get_graph(end_date, 7, host, user, email)

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
        element.set('image', filename_base + '.png')
        element.set('csv', filename_base + '.csv')

        for ks in self.__key_statistics:
            ks_element = Element('key-statistic')
            ks_element.set('name', ks.name)
            if type(ks.value) == float:
                ks.value = '%.2f' % ks.value
            ks_element.set('value', str(ks.value))
            ks_element.set('unit', ks.unit)
            if ks.link_type:
                ks_element.set('link-type', ks.link_type)
            element.append(ks_element)

        element.append(self.__plot.get_dom())

        return element

    def get_flowables(self, report_base, section_base, end_date):
        img_file = '%s/%s-%s.png' % (report_base, section_base, self.__name)
        if not os.path.exists(img_file):
            logging.warn('skipping summary for missing png: %s' % img_file)
            return []
        image = Image(img_file, width=(3.5 * inch))

        data = [[_('Key Statistics'), '']]

        for ks in self.__key_statistics:
            data.append([ks.name, "%s %s" % ks.scaled_value])

        ks_table = Table(data, colWidths=[1.5 * inch, 1.5 * inch],
                         style=[('ROWBACKGROUNDS', (0, 0), (-1, -1),
                                 (colors.lightgrey, None)),
                                ('SPAN', (0, 0), (1, 0)),
                                ('BACKGROUND', (0, 0), (1, 0), colors.grey),
                                ('BOX', (0, 0), (-1, -1), 1, colors.grey),
                                ('FONTNAME', (0, 0), (-1, -1), 'Helvetica')])

        t = Table([[image, ks_table]],
                  style=[('VALIGN', (1, 0), (1, 0), 'MIDDLE')])

        return [t]

class Chart:
    def __init__(self, type=TIME_SERIES_CHART, title=None, xlabel=None,
                 ylabel=None, major_formatter=IDENTITY_FORMATTER):
        self.__type = type
        self.__title = title
        self.__xlabel = xlabel
        self.__ylabel = ylabel
        self.__major_formatter = major_formatter

        self.__datasets = []

        self.__header = [xlabel]

    def add_dataset(self, xdata, ydata, label=None, linestyle='-'):
        if self.__type == PIE_CHART:
            raise ValueError('using 2D dataset for pie chart')

        m = {'xdata': xdata, 'ydata': ydata, 'label': label,
             'linestyle': linestyle}
        self.__datasets.append(m)

        self.__header.append(label)

    def add_pie_dataset(self, data):
        if self.__type != PIE_CHART:
            raise ValueError('using pie dataset for non-pie chart')

        self.__datasets = data

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

        rows = [[k] + v for (k, v) in data.iteritems()]
        rows.sort()

        if self.__major_formatter:
            fn = self.__major_formatter.function
            rows = map(lambda a: [fn(a[0], None)] + a[1:], rows)

        w = csv.writer(open(filename, 'w'))
        w.writerow(self.__header)
        w.writerows(rows)

    def __generate_pie_csv(self, filename, host=None, user=None, email=None):
        rows = [self.__datasets.keys(), self.__datasets.values()]

        w = csv.writer(open(filename, 'w'))
        w.writerow([_('slice'), _('value')])
        w.writerows(self.__datasets.items())

class KeyStatistic:
    def __init__(self, name, value, unit, link_type=None):
        if name is None:
            logging.warn('KeyStatistic name is None')
            name = _("Unknown")
        if value is None:
            logging.warn('KeyStatistic for %s value is None' % name)
            value = 0

        self.__name = name
        self.__value = value
        self.__unit = unit
        self.__link_type = link_type

    @property
    def scaled_value(self):
        if self.__unit.startswith('bytes'):
            s = string.split(self.__unit, '/')
            s[0] = _('MB')
            return ('%.2f' % (self.__value / 1000000), string.join(s, '/'))
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
