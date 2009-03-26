import os
import pylab

from matplotlib.ticker import FuncFormatter
from mx.DateTime import DateTimeDeltaFromSeconds
from lxml.etree import Element
from lxml.etree import ElementTree

def __time_of_day_formatter(x, pos):
    t = DateTimeDeltaFromSeconds(x)
    return "%02d:%02d" % (t.hour, t.minute)

TIME_OF_DAY_FORMATTER = FuncFormatter(__time_of_day_formatter)
EVEN_HOURS_OF_A_DAY = [i * 7200 for i in range(12)]

class Report:
    def __init__(self, name, title, sections):
        self.__name = name
        self.__title = title
        self.__sections = sections

    def generate(self, date_base, end_date):
        node_base = '%s/%s' % (date_base, self.__name)

        element = Element('report')
        element.set('foo', 'bar')

        for s in self.__sections:
            element.append(s.generate(node_base, end_date))

        tree = ElementTree(element)

        if not os.path.exists(node_base):
            os.makedirs(node_base)

        tree.write("%s/report.xml" % node_base)

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

    def generate(self, node_base, end_date):
        # XXX return DOM
        pass

class SummarySection(Section):
    def __init__(self, name, title, summary_items=[]):
        Section.__init__(self, name, title)

        self.__summary_items = summary_items

    def generate(self, node_base, end_date):
        section_base = "%s_%s" % (node_base, self.name)

        element = Element('summary-section')
        element.set('name', self.name)

        for summary_item in self.__summary_items:
            summary_item.generate(section_base, end_date)

        return element

class DetailSection(Section):
    def __init__(self, name, tile):
        Section.__init__(self, name, title)

    def generate(self, node_base, end_date):
        # XXX return DOM
        pass

class Graph:
    def __init__(self, name):
        self.__name = name

    def get_key_statistics(self, end_date):
        return []

    def get_plot(self, end_date):
        return None

    @property
    def name(self):
        return self.__name

    def generate(self, section_base, end_date):
        self.__key_statistics = self.get_key_statistics(end_date)
        self.__plot = self.get_plot(end_date)

        filename_base = '%s-%s' % (section_base, self.__name)

        dir = os.path.dirname(filename_base)
        if not os.path.exists(dir):
            os.makedirs(dir)

        self.__plot.generate_graph(filename_base + '.png')
        self.__plot.generate_csv(filename_base + '.csv')

        # XXX return DOM

class LinePlot:
    def __init__(self, title=None, xlabel=None, ylabel=None):
        self.__title = title
        self.__xlabel = xlabel
        self.__ylabel = ylabel

        self.__datasets = []

    def add_dataset(self, xdata, ydata, label=None, linestyle='-'):
        m = {'xdata': xdata, 'ydata': ydata, 'label': label,
             'linestyle': linestyle}
        self.__datasets.append(m)

    def generate_csv(self, filename):
        pass # XXX do it!

    def generate_graph(self, filename):
        fix = pylab.figure()
        axes = pylab.axes()
        axes.xaxis.set_major_formatter(TIME_OF_DAY_FORMATTER)
        axes.xaxis.set_ticks(EVEN_HOURS_OF_A_DAY)
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
    def __init__(self, name, value, units):
        self.__name = name
        self.__value = value
        self.__units = units

    @property
    def name(self):
        return self.__name

    @property
    def value(self):
        return self.__value

    @property
    def units(self):
        return self.__units
