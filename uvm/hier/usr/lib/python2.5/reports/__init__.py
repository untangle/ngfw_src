import csv
import gettext
import mx
import os
import pylab
import string
import sql_helper

from matplotlib.ticker import FuncFormatter
from mx.DateTime import DateTimeDeltaFromSeconds
from lxml.etree import Element
from lxml.etree import CDATA
from lxml.etree import ElementTree

_ = gettext.gettext
def N_(message): return message

def __time_of_day_formatter(x, pos):
    t = DateTimeDeltaFromSeconds(x)
    return "%02d:%02d" % (t.hour, t.minute)

TIME_OF_DAY_FORMATTER = FuncFormatter(__time_of_day_formatter)
EVEN_HOURS_OF_A_DAY = [i * 7200 for i in range(12)]

params = {'axes.labelsize': 8,
          'text.fontsize': 8,
          'xtick.labelsize': 8,
          'ytick.labelsize': 8,
          'legend.fontsize': 8,
          'figure.dpi': 100,
          'figure.figsize': (3.5,2.5)}
pylab.rcParams.update(params)

class Report:
    def __init__(self, name, title, sections):
        self.__name = name
        self.__title = title
        self.__sections = sections

    def generate(self, report_base, date_base, end_date, host=None, user=None):
        node_base = self.__get_node_base(date_base, host, user)

        element = Element('report')
        element.set('name', self.__name)
        element.set('title', self.__title)
        if host:
            element.set('host', host)
        if user:
            element.set('user', user)

        for s in self.__sections:
            element.append(s.generate(report_base, node_base, end_date, host,
                                      user))

        tree = ElementTree(element)

        if not os.path.exists(node_base):
            os.makedirs(node_base)

        tree.write("%s/%s/report.xml" % (report_base, node_base),
                   encoding='utf-8', pretty_print=True, xml_declaration=True)

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

        node_base = self.__get_node_base(date_base)

        for s in self.__sections:
            s.to_html(writer, report_base, node_base, end_date)

    def __get_node_base(self, date_base, host=None, user=None):
        if host:
            return '%s/%s/host/%s' % (date_base, self.__name, host)
        elif user:
            return '%s/%s/user/%s' % (date_base, self.__name, user)
        else:
            return '%s/%s' % (date_base, self.__name)

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

    def generate(self, report_base, node_base, end_date, host=None, user=None):
        pass

    def to_html(self, writer, report_base, section_base, end_date):
        pass

class SummarySection(Section):
    def __init__(self, name, title, summary_items=[]):
        Section.__init__(self, name, title)

        self.__summary_items = summary_items

    def generate(self, report_base, node_base, end_date, host=None, user=None):
        section_base = "%s/%s" % (node_base, self.name)

        element = Element('summary-section')
        element.set('name', self.name)
        element.set('title', self.title)

        for summary_item in self.__summary_items:
            element.append(summary_item.generate(report_base, section_base,
                                                 end_date, host, user))

        return element

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

class DetailSection(Section):
    def __init__(self, name, title):
        Section.__init__(self, name, title)

    def get_columns(self, host=None, user=None):
        pass

    def get_sql(self, start_date, end_date, host=None, user=None):
        pass

    def generate(self, report_base, node_base, end_date, host=None, user=None):
        element = Element('detail-section')
        element.set('name', self.name)
        element.set('title', self.title)

        start_date = end_date - mx.DateTime.DateTimeDelta(1)
        sql = self.get_sql(start_date, end_date, host, user)

        sql_element = Element('sql')
        sql_element.text = CDATA(sql)
        element.append(sql_element)

        for c in self.get_columns(host, user):
            element.append(c.get_dom())

        return element

    def to_html(self, writer, report_base, section_base, end_date):
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
            start_date = end_date - mx.DateTime.DateTimeDelta(1)
            curs.execute(self.get_sql(start_date, end_date))
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

    def get_key_statistics(self, end_date, host=None, user=None):
        return []

    def get_plot(self, end_date, host=None, user=None):
        return None

    @property
    def name(self):
        return self.__name

    def generate(self, report_base, section_base, end_date, host=None,
                 user=None):
        self.__key_statistics = self.get_key_statistics(end_date)
        self.__plot = self.get_plot(end_date)

        filename_base = '%s-%s' % (section_base, self.__name)

        dir = os.path.dirname('%s/%s' % (report_base, filename_base))
        if not os.path.exists(dir):
            os.makedirs(dir)

        self.__plot.generate_graph('%s/%s.png' % (report_base, filename_base))
        self.__plot.generate_csv('%s/%s.csv' % (report_base, filename_base))

        element = Element('graph')
        element.set('name', self.__name)
        element.set('title', self.__title)
        element.set('image', filename_base + '.png')
        element.set('csv', filename_base + '.csv')

        for ks in self.__key_statistics:
            ks_element = Element('key-statistic')
            ks_element.set('name', ks.name)
            ks_element.set('value', str(ks.value))
            ks_element.set('unit', ks.unit)
            element.append(ks_element)

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


class LinePlot:
    def __init__(self, title=None, xlabel=None, ylabel=None,
                 major_formatter=None, xaxis_ticks=None):
        self.__title = title
        self.__xlabel = xlabel
        self.__ylabel = ylabel
        self.__major_formatter = major_formatter
        self.__xaxis_ticks = xaxis_ticks

        self.__datasets = []

    def add_dataset(self, xdata, ydata, label=None, linestyle='-'):
        m = {'xdata': xdata, 'ydata': ydata, 'label': label,
             'linestyle': linestyle}
        self.__datasets.append(m)

    def generate_csv(self, filename, host=None, user=None):
        data = {}
        z = 0
        for ds in self.__datasets:
            for x, y in zip(ds['xdata'], ds['ydata']):
                a = data.get(x, None)
                if a:
                    a[z] = y
                else:
                    a = [None for i in range(len(self.__datasets))]
                    a[z] = y
                    data[x] = a
            z = z + 1

        rows = [[k] + v for (k, v) in data.iteritems()]
        rows.sort()

        if self.__major_formatter:
            rows = map(lambda a: [self.__major_formatter(a[0], None)] + a[1:],
                       rows)

        w = csv.writer(open(filename, 'w'))
        w.writerows(rows)

    def generate_graph(self, filename, host=None, user=None):
        fix = pylab.figure()
        axes = pylab.axes()

        if self.__major_formatter:
            axes.xaxis.set_major_formatter(self.__major_formatter)
        if self.__xaxis_ticks:
            axes.xaxis.set_ticks(self.__xaxis_ticks)
        pylab.title(self.__title)
        pylab.xlabel(self.__xlabel)
        pylab.ylabel(self.__ylabel)
        fix.autofmt_xdate()

        for ds in self.__datasets:
            pylab.plot(ds['xdata'], ds['ydata'], linestyle=ds['linestyle'],
                       label=ds['label'])

        pylab.legend()
        pylab.savefig(filename)

class KeyStatistic:
    def __init__(self, name, value, unit):
        self.__name = name
        self.__value = value
        self.__unit = unit

    @property
    def name(self):
        return self.__name

    @property
    def value(self):
        return self.__value

    @property
    def unit(self):
        return self.__unit
