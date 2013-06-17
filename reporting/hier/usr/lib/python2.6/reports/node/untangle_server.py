import gettext
import logging
import mx
import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper

from mx.DateTime import DateTimeDelta
from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
from psycopg2.extensions import TimestampFromMx
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import STACKED_BAR_CHART
from reports import SummarySection
from reports import TIMESTAMP_FORMATTER
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext
def N_(message): return message

class ServerNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-reporting')

    @print_timing
    def setup(self):
        self.__create_server_events()
        ft = FactTable('reports.server_totals', 'reports.server_events', 'time_stamp', [], [])
        ft.measures.append(Column('mem_free', 'int8', "avg(mem_free)"))
        ft.measures.append(Column('mem_cache', 'int8', "avg(mem_cache)"))
        ft.measures.append(Column('mem_buffers', 'int8', "avg(mem_buffers)"))
        ft.measures.append(Column('load_1', 'DECIMAL(6, 2)', "avg(load_1)"))
        ft.measures.append(Column('load_5', 'DECIMAL(6, 2)', "avg(load_5)"))
        ft.measures.append(Column('load_15', 'DECIMAL(6, 2)', "avg(load_15)"))
        ft.measures.append(Column('cpu_user', 'DECIMAL(6, 2)', "avg(cpu_user)"))
        ft.measures.append(Column('cpu_system', 'DECIMAL(6, 2)', "avg(cpu_system)"))
        ft.measures.append(Column('disk_total', 'int8', "avg(disk_total)"))
        ft.measures.append(Column('disk_free', 'int8', "avg(disk_free)"))
        ft.measures.append(Column('swap_total', 'int8', "avg(swap_total)"))
        ft.measures.append(Column('swap_free', 'int8', "avg(swap_free)"))
        reports.engine.register_fact_table(ft)

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table("server_events", cutoff)
        sql_helper.drop_fact_table("server_totals", cutoff)        

    def get_report(self):
        sections = []
        s = SummarySection('summary', _('Summary Report'),
                           [LoadUsage(), #CpuUsage(),
                            MemoryUsage(), SwapUsage(),
                            DiskUsage()])
        sections.append(s)
        return Report(self, sections)

    @print_timing
    def __create_server_events(self):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.server_events (
    time_stamp  TIMESTAMP,
    mem_free 	INT8,
    mem_cache 	INT8,
    mem_buffers INT8,
    load_1 	DECIMAL(6, 2),
    load_5 	DECIMAL(6, 2),
    load_15	DECIMAL(6, 2),
    cpu_user 	DECIMAL(6, 3),
    cpu_system 	DECIMAL(6, 3),
    disk_total 	INT8,
    disk_free 	INT8,
    swap_total 	INT8,
    swap_free 	INT8)""")

    def teardown(self):
        pass

class MemoryUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'free-memory', _('Free Memory'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        try:
            # MB
            sums = ["COALESCE(AVG(mem_free),0) / 1000000",
                    "COALESCE(AVG(mem_cache), 0) / 1000000"]

            q, h = sql_helper.get_averaged_query(sums, "reports.server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date)

            curs = conn.cursor()            
            curs.execute(q, h)

            dates = []
            free = []
            cached = []

            for r in curs.fetchall():
                dates.append(r[0])
                free.append(r[1])
                cached.append(r[2])

            if not free:
                free = [0,]
            if not cached:
                cached = [0,]

            ks = KeyStatistic(_('Avg Free Memory'), round(sum(free)/len(free), 2),
                              N_('MB'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Cached Memory'), round(sum(cached)/len(cached), 2),
                              N_('MB'))
            lks.append(ks)
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Memory (MB)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, free, _('Free Memory'))
        plot.add_dataset(dates, cached, _('Cached Memory'))

        return (lks, plot)

class LoadUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'load-usage', _('Cpu Load'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        try:

            sums = ["ROUND((COALESCE(AVG(load_1),0))::numeric, 2)",
                    "ROUND((COALESCE(AVG(load_5),0))::numeric, 2)",
                    "ROUND((COALESCE(AVG(load_15),0))::numeric, 2)"]

            q, h = sql_helper.get_averaged_query(sums, "reports.server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date)

            curs = conn.cursor()
            curs.execute(q, h)
                                                 
            dates = []
            load1 = []
            load5 = []
            load15 = []

            for r in curs.fetchall():
                dates.append(r[0])
                load1.append(r[1])
                load5.append(r[2])
                load15.append(r[3])

            if not load1:
                load1 = [0,]
            if not load5:
                load5 = [0,]
            if not load15:
                load15 = [0,]

            ks = KeyStatistic(_('Avg 1-min Load'), round(sum(load1)/len(load1), 2),
                              N_(''))
            lks.append(ks)
            ks = KeyStatistic(_('Max 1-min Load'), max(load1),
                              N_(''))
            lks.append(ks)
            ks = KeyStatistic(_('Avg 5-min Load'), round(sum(load5)/len(load5), 2),
                              N_(''))
            lks.append(ks)
            ks = KeyStatistic(_('Max 5-min Load'), max(load5),
                              N_(''))
            lks.append(ks)
            ks = KeyStatistic(_('Avg 15-min Load'), round(sum(load15)/len(load15), 2),
                              N_(''))
            lks.append(ks)
            ks = KeyStatistic(_('Max 15-min Load'), max(load15),
                              N_(''))
            lks.append(ks)
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Load'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, load1, _('1-min Load'))
        plot.add_dataset(dates, load5, _('5-min Load'))
        plot.add_dataset(dates, load15, _('15-min Load'))

        return (lks, plot)

class CpuUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'cpu-usage', _('Cpu Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        try:
            sums = ["ROUND((COALESCE(AVG(cpu_user),0)*100)::numeric, 2)",
                    "ROUND((COALESCE(AVG(cpu_system),0)*100)::numeric, 2)"]

            q, h = sql_helper.get_averaged_query(sums, "reports.server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date)

            curs = conn.cursor()            
            curs.execute(q, h)

            dates = []
            cpuUser = []
            cpuSystem = []
            
            for r in curs.fetchall():
                dates.append(r[0])
                cpuUser.append(r[1])
                cpuSystem.append(r[2])

            if not cpuUser:
                cpuUser = [0,]
            if not cpuSystem:
                cpuSystem = [0,]

            ks = KeyStatistic(_('Avg Cpu User'), round(sum(cpuUser)/len(cpuUser), 2),
                              N_('%'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Cpu System'), round(sum(cpuSystem)/len(cpuSystem), 2),
                              N_('%'))
            lks.append(ks)
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('CPU (%)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, cpuUser, _('Cpu User'))
        plot.add_dataset(dates, cpuSystem, _('Cpu System'))

        return (lks, plot)

class DiskUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'disk-usage', _('Disk Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        try:
            # GB
            sums = ["ROUND((COALESCE(AVG(disk_free),0) / 1000000000)::numeric, 2)",
                    "ROUND((COALESCE(AVG(disk_total),0) / 1000000000)::numeric, 2)"]

            q, h = sql_helper.get_averaged_query(sums, "reports.server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date)

            curs = conn.cursor()
            curs.execute(q, h)

            dates = []
            diskFree = []
            diskTotal = []

            for r in curs.fetchall():
                dates.append(r[0])
                diskFree.append(r[1])
                diskTotal.append(r[2])

            if not diskFree or not diskTotal:
                avg = avgp = None
            else:
                avg = round(sum(diskFree)/len(diskFree), 2)
                avgp = round(100 * avg / float(diskTotal[0]), 2)
                
            ks = KeyStatistic(_('Avg Disk Free'), avg,
                              N_('GB'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Disk Free'), avgp,
                              N_('%'))
            lks.append(ks)

                
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Disk (GB)'),
                     major_formatter=TIMESTAMP_FORMATTER,
                     y_axis_lower_bound=0)

        plot.add_dataset(dates, diskFree, _('Free Disk'))

        return (lks, plot)

class SwapUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'swap-usage', _('Swap Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        try:
            # MB
            sums = ["ROUND((COALESCE(AVG(swap_total-swap_free),0) / 1000000)::numeric, 2)",
                    "ROUND((COALESCE(AVG(swap_free),0) / 1000000)::numeric, 2)"]
                                                 
            q, h = sql_helper.get_averaged_query(sums, "reports.server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date)
            
            curs = conn.cursor()
            curs.execute(q, h)

            dates = []
            swapUsed = []
            swapFree = []

            for r in curs.fetchall():
                dates.append(r[0])
                swapUsed.append(r[1])
                swapFree.append(r[2])

            if not swapUsed:
                swapUsed = [0,]
            if not swapFree:
                swapFree = [0,]

            ks = KeyStatistic(_('Avg Swap Free'), round(sum(swapFree)/len(swapFree), 2),
                              N_('MB'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Swap Used'), round(sum(swapUsed)/len(swapUsed), 2),
                              N_('MB'))
            lks.append(ks)
                
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Swap (MB)'),
                     major_formatter=TIMESTAMP_FORMATTER,
                     y_axis_lower_bound=0)

        plot.add_dataset(dates, swapUsed, _('Swap Used'))

        return (lks, plot)

reports.engine.register_node(ServerNode())
