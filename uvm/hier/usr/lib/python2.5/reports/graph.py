import os

from matplotlib.ticker import FuncFormatter
from mx.DateTime import DateTimeDeltaFromSeconds

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

        for s in self.__sections:
            s.generate(node_base, end_date)

        # print XML

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

        for summary_item in self.__summary_items:
            summary_item.generate(section_base, end_date) # XXX add to dom

        # XXX return DOM

class DetailSection(Section):
    def __init__(self, name, tile):
        Section.__init__(self, name, title)

    def generate(self, node_base, end_date):
        # XXX return DOM
        pass

class Graph:
    def __init__(self, name):
        self.__name = name
        self.__graph_filename = None
        self.__key_statistics = []

    @property
    def graph_filename(self):
        return self.__graph_filename

    @property
    def key_statistics(self):
        return self.__key_statistics

    @property
    def name(self):
        return self.__name

    def generate(self, section_base, end_date):
        self.__graph_filename = '%s-%s.png' % (section_base, self.__name)
        os.makedirs(os.path.dirname(self.__graph_filename))

        # XXX return DOM
        return self.generate_graph(end_date)

    def generate_graph(self, end_date):
        pass

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
