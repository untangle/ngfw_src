import csv
import logging
import gettext
import mx
import os
import string
import sql_helper
import popen2
import re

from lxml.etree import CDATA
from lxml.etree import Element
from lxml.etree import ElementTree
from mx.DateTime import DateTimeDeltaFromSeconds
from reports.engine import get_node_base
from reportlab.platypus import Paragraph, Spacer
from reportlab.platypus.flowables import Image
from reportlab.platypus.tables import Table
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import inch
from popen2 import popen2

styles = getSampleStyleSheet()

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

    def to_html(self, writer, report_base, date_base, end_date):
        ni = writer.add_node_anchor(self.__name)

        writer.write("""\
      <table style="width:100%%;border-bottom:1px #CCC solid;margin-bottom:10px;">
        <tr>
          <td>
            <span style="font-size:16px;font-weight:bold;">%s</span><a name="%s"></a>
          </td>
          <td style="text-align:right;">
            <a href="#" style="font-size:12px;">Back To Top</a>
          </td>

        </tr>
      </table>""" % (ni.display_name, ni.anchor))

        node_base = self.get_node_base(self.__name, date_base)

        for s in self.__sections:
            s.to_html(writer, report_base, node_base, end_date)

    def get_flowables(self, report_base, date_base, end_date):
        node_base = get_node_base(self.__name, date_base)

        story = []
        story.append(Paragraph(self.__name, styles['Normal']))

        for s in self.__sections:
            story += s.get_flowables(report_base, node_base, end_date)
            story.append(Spacer(1, 0.2 * inch))

        return story

    def __get_node_title(self, name):
        title = None

        (stdout, stdin) = popen2('apt-cache show untangle-node-webfilter')
        try:
            for l in stdout:
                m = re.search('Description: (.*)', l)
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

    def to_html(self, writer, report_base, section_base, end_date):
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

    def to_html(self, writer, report_base, node_base, end_date):
        writer.write("""\
      <div style="margin-left:10px;">
        <table style="width:100%%;border-bottom:1px #CCC dotted;margin-bottom:10px;">
          <tr>
            <td>
              <span style="font-size:14px;font-weight:bold;;color:#009933">%s</span>
            </td>

          </tr>
        </table>
""" % _(self.title))

        section_base = "%s/%s" % (node_base, self.name)

        for si in self.__summary_items:
            si.to_html(writer, report_base, section_base, end_date)

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

    def to_html(self, writer, report_base, section_base, end_date):
        return

        start_date = end_date - mx.DateTime.DateTimeDelta(1)
        sql = self.get_sql(start_date, end_date)
        if not sql:
            return

        writer.write("""\
<table style="width:100%%;border-bottom:1px #CCC dotted;margin-bottom:10px;">
  <tr>
    <td>
      <span style="font-size:14px;font-weight:bold;;color:#009933">%s</span>
    </td>

  </tr>
</table>
<table style="width:100%%;font-size:12px;"><tbody><tr><td colspan="2">
        <div style="float: left; width: 100%%;"><table  style="width:100%%;"><thead>
              <tr style="background-color:#EFEFEF;text-align:left;"><th >Client Address</th>
""" % _(self.title))

        columns = self.get_columns()

        for c in columns:
            writer.write('<th >%s</th>' % _(c.title))

        conn = sql_helper.get_connection()

        try:
            curs = conn.cursor()
            curs.execute(sql)
            rows = curs.fetchall()
        finally:
            conn.commit()

        row_num = 0;
        for r in rows:
            if row_num % 2 == 0:
                writer.write("<tr>")
            else:
                writer.write('<tr style="background-color:#EFEFEF;">')

            row_num = row_num + 1

            for i, d in enumerate(r):
                # XXX custom formatters/units?
                writer.write('<td>%s</td>' % d)

            writer.write('</tr>')

        writer.write("""\
          </tbody></table>
""")

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

    def to_html(self, writer, report_base, section_base, end_date):
        image_cid = writer.encode_image('%s/%s-%s.png' % (report_base,
                                                          section_base,
                                                          self.__name))

        writer.write("""\
<table  style="width:100%%;border:1px #ccc solid;font-size:11px;">
  <tbody>
    <tr><td style="vertical-align:top;padding-bottom:1em;padding-right:1em;" ><img src="%s"/></td><td style="vertical-align:top;padding-bottom:1em;">
        <div style=" width: 236px;background-color:#cccccc;border:1px #ccc solid;border-bottom:none;padding:0.2em;color:#333;clear:left;font-weight:bold;">%s</div>

        <table style="border:1px #EFEFEF solid;" >
          <tbody>""" % (image_cid, _('Key Statistics')))

        row_num = 0
        for ks in self.__key_statistics:
            if row_num % 2 == 0:
                writer.write("""\
            <tr><td style="width:150px;">%s</td><td  style="width:80px;">%s %s</td></tr>"""
                             % (ks.name, ks.value, ks.unit))
            else:
                writer.write("""\
            <tr style="background-color:#EFEFEF;"><td style="width:150px;">%s</td><td style="width:80px;">%s %s</td></tr>""" % (ks.name, ks.value, ks.unit))

            row_num = row_num + 1

        writer.write("""\
          </tbody>
        </table>
    </td></tr>
  </tbody>
</table>
""")

    def get_flowables(self, report_base, section_base, end_date):
        img_file = '%s/%s-%s.png' % (report_base, section_base, self.__name)
        if not os.path.exists(img_file):
            logging.warn('skipping summary for missing png: %s' % img_file)
            return []
        image = Image(img_file)

        data = []

        for ks in self.__key_statistics:
            data.append([ks.name, ks.value, ks.unit])
        table = Table(data)

        return [image, table]

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
