import csv
import mx
import os
import pylab
import string

from matplotlib.ticker import FuncFormatter
from mx.DateTime import DateTimeDeltaFromSeconds
from lxml.etree import Element
from lxml.etree import CDATA
from lxml.etree import ElementTree

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

    def generate(self, report_base, date_base, end_date):
        node_base = '%s/%s' % (date_base, self.__name)

        element = Element('report')
        element.set('name', self.__name)
        element.set('title', self.__title)

        for s in self.__sections:
            element.append(s.generate(report_base, node_base, end_date))

        tree = ElementTree(element)

        if not os.path.exists(node_base):
            os.makedirs(node_base)

        tree.write("%s/%s/report.xml" % (report_base, node_base),
                   encoding='utf-8', pretty_print=True, xml_declaration=True)

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

    def generate(self, report_base, node_base, end_date):
        # XXX return DOM
        pass

class SummarySection(Section):
    def __init__(self, name, title, summary_items=[]):
        Section.__init__(self, name, title)

        self.__summary_items = summary_items

    def generate(self, report_base, node_base, end_date):
        section_base = "%s/%s" % (node_base, self.name)

        element = Element('summary-section')
        element.set('name', self.name)
        element.set('title', self.title)

        for summary_item in self.__summary_items:
            element.append(summary_item.generate(report_base, section_base,
                                                 end_date))

        return element

class DetailSection(Section):
    def __init__(self, name, title, columns=[], sql_template=None):
        Section.__init__(self, name, title)
        self.__columns = columns
        self.__sql_template = sql_template

    def generate(self, report_base, node_base, end_date):
        element = Element('detail-section')
        element.set('name', self.name)
        element.set('title', self.title)
        if self.__sql_template:
            sql_element = Element('sql')

            ed = '%d-%d-%d' % (end_date.year, end_date.month, end_date.day)

            odb = (end_date - mx.DateTime.DateTimeDelta(1))
            one_day_before = '%d-%d-%d' % (odb.year, odb.month, odb.day)

            t = string.Template(self.__sql_template)
            sql_element.text = CDATA(t.substitute(end_date=ed,
                                                  one_day_before=one_day_before))

            element.append(sql_element)

        for c in self.__columns:
            element.append(c.get_dom())

        return element

class ColumnDesc():
    def __init__(self, name, title, type=None):
        self.__name = name
        self.__title = title
        self.__type = type

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

    def get_key_statistics(self, end_date):
        return []

    def get_plot(self, end_date):
        return None

    @property
    def name(self):
        return self.__name

    def generate(self, report_base, section_base, end_date):
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

    def generate_csv(self, filename):
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

    def generate_graph(self, filename):
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
