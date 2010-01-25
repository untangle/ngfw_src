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

import gettext
import logging
import mx
import psycopg
import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper

from mx.DateTime import DateTimeDelta
from psycopg import DateFromMx
from psycopg import QuotedString
from psycopg import TimestampFromMx
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
from sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext
def N_(message): return message

class ServerNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-reporting')

    @print_timing
    def setup(self, start_date, end_date):
        self.__create_n_server_events(start_date, end_date)

        ft = FactTable('reports.n_server_totals',
                       'reports.n_server_events',
                       'time_stamp', [], [])
        ft.measures.append(Column('mem_free',
                                  'int8',
                                  "avg(mem_free)"))
        ft.measures.append(Column('mem_cache',
                                  'int8',
                                  "avg(mem_cache)"))
        ft.measures.append(Column('mem_buffers',
                                  'int8',
                                  "avg(mem_buffers)"))
        ft.measures.append(Column('load_1',
                                  'DECIMAL(6, 2)',
                                  "avg(load_1)"))
        ft.measures.append(Column('load_5',
                                  'DECIMAL(6, 2)',
                                  "avg(load_5)"))
        ft.measures.append(Column('load_15',
                                  'DECIMAL(6, 2)',
                                  "avg(load_15)"))
        ft.measures.append(Column('cpu_user',
                                  'DECIMAL(6, 2)',
                                  "avg(cpu_user)"))
        ft.measures.append(Column('cpu_system',
                                  'DECIMAL(6, 2)',
                                  "avg(cpu_system)"))
        ft.measures.append(Column('disk_total',
                                  'int8',
                                  "avg(disk_total)"))
        ft.measures.append(Column('disk_free',
                                  'int8',
                                  "avg(disk_free)"))
        reports.engine.register_fact_table(ft)

    @print_timing
    def events_cleanup(self, cutoff):
        sql_helper.run_sql("""\
DELETE FROM events.n_server_events WHERE time_stamp < %s""", (cutoff,))

    def reports_cleanup(self, cutoff):
        pass

    def get_report(self):
        sections = []
        s = SummarySection('summary', _('Summary Report'),
                           [MemoryUsage(), LoadUsage(),
                            CpuUsage(), DiskUsage()])
        sections.append(s)
        return Report(self, sections)

    @print_timing
    def __create_n_server_events(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.n_server_events (
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
    disk_free 	INT8)""", 'time_stamp', start_date, end_date)

        sd = DateFromMx(sql_helper.get_update_info('reports.n_server_events',
                                                   start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_server_events
      (time_stamp, mem_free,
      mem_cache, mem_buffers,
      load_1, load_5, load_15,
      cpu_user, cpu_system,
      disk_total, disk_free)
SELECT time_stamp, mem_free,
      mem_cache, mem_buffers,
      load_1, load_5, load_15,
      cpu_user, cpu_system,
      disk_total, disk_free
FROM events.n_server_evt
WHERE time_stamp >= %s AND time_stamp < %s""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_server_events', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def teardown(self):
        pass

class MemoryUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'memory-usage', _('Memory Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        conn = sql_helper.get_connection()
        try:
            lks = []

            ks_query = """\
SELECT avg(mem_free), avg(mem_cache), avg(mem_buffers)
FROM reports.n_server_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

            curs = conn.cursor()
            curs.execute(ks_query, (one_week, ed))
            r = curs.fetchone()

            ks = KeyStatistic(_('Avg free memory'), r[0] / 10**6,
                              N_('MB'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg cached memory'), r[1] / 10**6,
                              N_('MB'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg buffered memory'), r[2] / 10**6,
                              N_('MB'))
            lks.append(ks)

            # MB
            sums = ["COALESCE(AVG(mem_free),0) / 1000000",
                    "COALESCE(AVG(mem_cache), 0) / 1000000",
                    "COALESCE(AVG(mem_buffers), 0) / 1000000"]

            q, h = sql_helper.get_averaged_query([], "reports.n_server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 avgs=sums)
            curs.execute(q, h)

            dates = []
            free = []
            cached = []
            buffered = []

            for r in curs.fetchall():
                dates.append(r[0])
                free.append(r[1])
                cached.append(r[2])
                buffered.append(r[3])
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('TIME'), ylabel=_('Memory (MB)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, free, _('Free memory'))
        plot.add_dataset(dates, buffered, _('Buffered memory'))
        plot.add_dataset(dates, cached, _('Cached memory'))

        return (lks, plot)

class LoadUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'load-usage', _('CPU Load'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        conn = sql_helper.get_connection()
        try:
            lks = []

            ks_query = """\
SELECT avg(load_1), avg(load_5), avg(load_15)
FROM reports.n_server_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

            curs = conn.cursor()
            curs.execute(ks_query, (one_week, ed))
            r = curs.fetchone()

            ks = KeyStatistic(_('Avg 1-min load'), r[0],
                              N_(''))
            lks.append(ks)
            ks = KeyStatistic(_('Avg 5-min load'), r[1],
                              N_(''))
            lks.append(ks)
            ks = KeyStatistic(_('Avg 15-min load'), r[2],
                              N_(''))
            lks.append(ks)

            sums = ["COALESCE(AVG(load_1),0)",
                    "COALESCE(AVG(load_5),0)",
                    "COALESCE(AVG(load_15),0)"]

            q, h = sql_helper.get_averaged_query([], "reports.n_server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 avgs=sums)

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
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Load'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, load1, _('1-min load'))
        plot.add_dataset(dates, load5, _('5-min load'))
        plot.add_dataset(dates, load15, _('10-min load'))

        return (lks, plot)

class CpuUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'cpu-usage', _('CPU Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        conn = sql_helper.get_connection()
        try:
            lks = []

            ks_query = """\
SELECT avg(cpu_user), avg(cpu_system)
FROM reports.n_server_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

            curs = conn.cursor()
            curs.execute(ks_query, (one_week, ed))
            r = curs.fetchone()

            ks = KeyStatistic(_('Avg CPU user'), r[0],
                              N_('%'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg CPU system'), r[1],
                              N_('%'))
            lks.append(ks)

            sums = ["COALESCE(AVG(cpu_user),0)",
                    "COALESCE(AVG(cpu_system),0)"]

            q, h = sql_helper.get_averaged_query([], "reports.n_server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 avgs=sums)
            curs.execute(q, h)

            dates = []
            cpuUser = []
            cpuSystem = []
            
            for r in curs.fetchall():
                dates.append(r[0])
                cpuUser.append(r[1])
                cpuSystem.append(r[2])
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('CPU (%)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, cpuUser, _('CPU user'))
        plot.add_dataset(dates, cpuSystem, _('CPU system'))

        return (lks, plot)

class DiskUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'disk-usage', _('Disk Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        conn = sql_helper.get_connection()
        try:
            lks = []

            ks_query = """\
SELECT avg(disk_free), avg(disk_total)
FROM reports.n_server_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

            curs = conn.cursor()
            curs.execute(ks_query, (one_week, ed))
            r = curs.fetchone()

            ks = KeyStatistic(_('Avg disk free'), r[0] / 10**9,
                              N_('GB'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg disk free'), r[0] * 100 / r[1],
                              N_('%'))
            lks.append(ks)

            # GB
            sums = ["COALESCE(AVG(disk_free),0) / 1000000000"]
                                                 
            q, h = sql_helper.get_averaged_query([], "reports.n_server_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 avgs=sums)

            curs.execute(q, h)

            dates = []
            diskFree = []

            for r in curs.fetchall():
                dates.append(r[0])
                diskFree.append(r[1])
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Disk (GB)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, diskFree, _('Free disk'))

        return (lks, plot)

reports.engine.register_node(ServerNode())
