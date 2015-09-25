import gettext
import logging
import mx
import re
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import string
import uvm.i18n_helper

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
from psycopg2.extensions import TimestampFromMx
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import Highlight
from reports import HOUR_FORMATTER
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import STACKED_BAR_CHART
from reports import SummarySection
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports import TIMESTAMP_FORMATTER
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle').lgettext

def N_(message): return message

def key_statistic_sort_by_value(a, b):
    return cmp(a.value, b.value)

class WanBalancer(reports.engine.Node):
    def __init__(self):
        reports.engine.Node.__init__(self, 'untangle-node-wan-balancer','WAN Balancer')

    @sql_helper.print_timing
    def setup(self):
        pass

    def create_tables(self):
        pass

    def parents(self):
        return ['untangle-vm']

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [WanBalancerHighlight(self.name),
                            BandwidthUsage(self),
                            ActiveSessions(self),
                            DailySessionsByWan(self),
                            DailyTrafficByWan(self),
                            SessionsByWanInterface(self),
                            BandwidthByWanInterface(self)])

        sections.append(s)

        return Report(self, sections)

    def reports_cleanup(self, cutoff):
        pass

    @property
    def wan_clause(self):
        return reports.engine.get_wan_clause()

    @property
    def wan_interfaces(self):
        a = []

        #str = self.__iface_props.get('com.untangle.wan-interfaces', None)
        str = reports.engine.get_wan_clause()[1:-1]

        for i in str.split(','):
            try:
                a.append(int(i))
            except ValueError:
                logging.warn('could not add interface: %s' % i, exc_info=True)

        return a

    @property
    def interface_names(self):
        return reports.engine.get_wan_names_map()

class WanBalancerHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("spread") + " "
                           "%(sessions)s" + " " + _("sessions over") +
                           " " + "%(interfaces)s" + " " + _("interfaces"))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))
        
        query = """
SELECT COALESCE(SUM(new_sessions),0)::int AS sessions
FROM reports.session_totals
WHERE (client_intf IN %s OR server_intf IN %s) AND (time_stamp >= %%s AND time_stamp < %%s)
""" % (2*(reports.engine.get_wan_clause(),))

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
            h.update({'interfaces':reports.engine.get_number_wan_interfaces()})
        finally:
            conn.commit()

        return h

class BandwidthUsage(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'bandwidth-usage', _('Bandwidth Usage'))
        self.__wan_interfaces = set(node.wan_interfaces)
        self.__wan_clause = node.wan_clause
        self.__interface_names = node.interface_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        sd = DateFromMx(start_date)

        lks = []
        plot_data = {}

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["ROUND((COALESCE(sum(c2s_bytes + s2c_bytes), 0) / 1000)::numeric, 2)"] # kB

            extra_where = [("(client_intf IN %(clause)s OR server_intf IN %(clause)s)" % 
                            {'clause' : self.__wan_clause}, {})]
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            time_interval = 3600
            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 extra_fields = ('client_intf', 'server_intf'),
                                                 time_interval = time_interval)

            curs.execute(q, h)

            for r in curs.fetchall():
                time = r[0]
                client_intf = r[1]
                server_intf = r[2]
                bandwidth = r[3] / time_interval

                for intf in (client_intf, server_intf):
                    if intf in self.__wan_interfaces:
                        times, values = plot_data.get(intf, ([], []))
                        secs = time
                        if len(times) > 0 and times[-1] == secs:
                            values[-1] += bandwidth
                        else:
                            times.append(secs)
                            values.append(bandwidth)
                        plot_data[intf] = (times, values)

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                         xlabel=_('Time'), ylabel=_('kB/sec'),
                         major_formatter=TIMESTAMP_FORMATTER,
                         required_points=rp)

            for intf in self.__wan_interfaces:
                if not intf in plot_data:
                    plot_data[intf] = (rp, [0,]*len(rp))

            for k, v in plot_data.iteritems():
                time, bandwidth = v
                name = self.__interface_names.get(k, _('Unknown'))
                plot.add_dataset(time, bandwidth, name)

                ks = KeyStatistic(name + _('(avg)'), "%.2f" % (sum(bandwidth) / len(bandwidth)), N_('Kb/sec'))
                lks.append(ks)

                ks = KeyStatistic(name + _('(max)'), "%.2f" % max(bandwidth), N_('Kb/sec'))
                lks.append(ks)
                        
        finally:
            conn.commit()

        return (lks, plot)

class ActiveSessions(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'active-sessions', _('Active Sessions'))
        self.__wan_interfaces = set(node.wan_interfaces)
        self.__wan_clause = node.wan_clause
        self.__interface_names = node.interface_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        sd = DateFromMx(start_date)

        time_interval = 60

        lks = []
        plot_data = {}

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["coalesce(sum(num_sessions), 0)"]

            extra_where = [("(client_intf IN %(clause)s OR server_intf IN %(clause)s)" %
                            {'clause' : self.__wan_clause}, {})]
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.session_counts",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 extra_fields = ('client_intf', 'server_intf'))

            curs.execute(q, h)
            
            for r in curs.fetchall():
                time = r[0]
                client_intf = r[1]
                server_intf = r[2]
                sessions = r[3]

                for intf in (client_intf, server_intf):
                    if intf in self.__wan_interfaces:
                        times, values = plot_data.get(intf, ([], []))
                        secs = time
                        if len(times) > 0 and times[-1] == secs:
                            values[-1] += sessions
                        else:
                            times.append(secs)
                            values.append(sessions)
                        plot_data[intf] = (times, values)

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                         xlabel=_('Time'), ylabel=_('sessions'),
                         major_formatter=TIMESTAMP_FORMATTER,
                         required_points=rp)

            for intf in self.__wan_interfaces:
                if not intf in plot_data:
                    plot_data[intf] = (rp, [0,]*len(rp))

            for k, v in plot_data.iteritems():
                time, bandwidth = v
                name = self.__interface_names.get(k, _('Unknown'))
                plot.add_dataset(time, bandwidth, name)                       

                ks = KeyStatistic(name + _('(avg)'),
                                  "%.2f" % (sum(bandwidth)/len(bandwidth)),
                                  N_('Sessions'))
                lks.append(ks)
                ks = KeyStatistic(name + _('(max)'),
                                  max(bandwidth),
                                  N_('Sessions'))
                lks.append(ks)

                lks.sort(key_statistic_sort_by_value)
                        
        finally:
            conn.commit()

        return (lks, plot)

class DailySessionsByWan(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'sessions-by-wan',
                       _('Sessions By WAN'))
        self.__wan_interfaces = set(node.wan_interfaces)
        self.__wan_clause = node.wan_clause
        self.__interface_names = node.interface_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email or host or email:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []
        plot_data = {}

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["COALESCE(SUM(new_sessions), 0)"]

            extra_where = [("(client_intf IN %(clause)s OR server_intf IN %(clause)s)" %
                            {'clause' : self.__wan_clause}, {})]
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER

            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 extra_fields = ('client_intf', 'server_intf'),
                                                 time_interval = time_interval)

            curs.execute(q, h)
            
            for r in curs.fetchall():
                time = r[0]
                client_intf = r[1]
                server_intf = r[2]
                sessions = r[3]

                for intf in (client_intf, server_intf):
                    if intf in self.__wan_interfaces:
                        times, values = plot_data.get(intf, ([], []))
                        secs = time
                        if len(times) > 0 and times[-1] == secs:
                            values[-1] += sessions
                        else:
                            times.append(secs)
                            values.append(sessions)
                        plot_data[intf] = (times, values)

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            plot = Chart(type=STACKED_BAR_CHART, title=self.title,
                         xlabel=_('Time'), ylabel=_('sessions'),
                         major_formatter=TIMESTAMP_FORMATTER,
                         required_points=rp)

            for intf in self.__wan_interfaces:
                if not intf in plot_data:
                    plot_data[intf] = (rp, [0,]*len(rp))

            for k, v in plot_data.iteritems():
                time, bandwidth = v
                name = self.__interface_names.get(k, _('Unknown'))
                plot.add_dataset(time, bandwidth, name)                       

                ks = KeyStatistic(name + _('(avg)'),
                                  "%.2f" % (sum(bandwidth)/len(bandwidth)),
                                  N_('Sessions')+'/'+_(unit))
                lks.append(ks)
                ks = KeyStatistic(name + _('(max)'),
                                  max(bandwidth),
                                  N_('Sessions')+'/'+_(unit))
                lks.append(ks)

                lks.sort(key_statistic_sort_by_value)
                        
        finally:
            conn.commit()

        return (lks, plot)

class DailyTrafficByWan(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'traffic-by-wan',
                       _('Traffic By WAN'))
        self.__wan_interfaces = set(node.wan_interfaces)
        self.__wan_clause = node.wan_clause
        self.__interface_names = node.interface_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email or host or email:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []
        plot_data = {}

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["ROUND((COALESCE(SUM(c2s_bytes + s2c_bytes), 0) / 1000)::numeric, 2)"] #kB

            extra_where = [("(client_intf IN %(clause)s OR server_intf IN %(clause)s)" %
                            {'clause' : self.__wan_clause}, {})]
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER

            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 extra_fields = ('client_intf', 'server_intf'),
                                                 time_interval = time_interval)

            curs.execute(q, h)
            
            for r in curs.fetchall():
                time = r[0]
                client_intf = r[1]
                server_intf = r[2]
                data = r[3]

                for intf in (client_intf, server_intf):
                    if intf in self.__wan_interfaces:
                        times, values = plot_data.get(intf, ([], []))
                        secs = time
                        if len(times) > 0 and times[-1] == secs:
                            values[-1] += data
                        else:
                            times.append(secs)
                            values.append(data)
                        plot_data[intf] = (times, values)

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            plot = Chart(type=STACKED_BAR_CHART, title=self.title,
                         xlabel=_('Time'), ylabel=_('data'),
                         major_formatter=formatter,
                         required_points=rp)

            for intf in self.__wan_interfaces:
                if not intf in plot_data:
                    plot_data[intf] = (rp, [0,]*len(rp))

            for k, v in plot_data.iteritems():
                time, bandwidth = v
                name = self.__interface_names.get(k, _('Unknown'))
                plot.add_dataset(time, bandwidth, name)                       

                ks = KeyStatistic(name + _('(avg)'),
                                  "%.2f" % (sum(bandwidth)/len(bandwidth)),
                                  N_('Kb')+'/'+_(unit))
                lks.append(ks)
                ks = KeyStatistic(name + _('(max)'),
                                  max(bandwidth),
                                  N_('Kb')+'/'+_(unit))
                lks.append(ks)

                lks.sort(key_statistic_sort_by_value)
                        
        finally:
            conn.commit()

        return (lks, plot)

class SessionsByWanInterface(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'total-sessions-by-wan',
                       _('Total Sessions By WAN'))
        self.__wan_interfaces = set(node.wan_interfaces)
        self.__wan_clause = node.wan_clause
        self.__interface_names = node.interface_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT server_intf, client_intf, COALESCE(sum(new_sessions), 0)::int
FROM reports.session_totals
WHERE (client_intf IN %s OR server_intf IN %s) AND (time_stamp >= %%s AND time_stamp < %%s)
GROUP BY server_intf, client_intf""" % (2*(self.__wan_clause,))

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            data = {}

            for r in curs.fetchall():
                server_intf = r[0]
                client_intf = r[1]
                sessions = r[2]

                if client_intf in self.__wan_interfaces:
                    data[client_intf] = data.get(client_intf, 0) + sessions
                if server_intf in self.__wan_interfaces:
                    data[server_intf] = data.get(server_intf, 0) + sessions

            for k, v in data.iteritems():
                iface = self.__interface_names.get(k, _('Unknown'))
                ks = KeyStatistic(iface, v, _('Sessions'))
                lks.append(ks)
                dataset[iface] = v
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Interface'),
                     ylabel=_('Sessions'))
        plot.add_pie_dataset(dataset)

        return (lks, plot)

class BandwidthByWanInterface(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'bandwidth-by-wan-interface',
                       _('Bandwidth By WAN Interface'))
        self.__wan_interfaces = set(node.wan_interfaces)
        self.__wan_clause = node.wan_clause
        self.__interface_names = node.interface_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT server_intf, client_intf, COALESCE(sum(c2s_bytes + s2c_bytes)/1000000, 0)::bigint
FROM reports.session_totals
WHERE (client_intf IN %s OR server_intf IN %s) AND (time_stamp >= %%s AND time_stamp < %%s)
GROUP BY server_intf, client_intf""" % (2*(self.__wan_clause,))

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            data = {}

            for r in curs.fetchall():
                server_intf = r[0]
                client_intf = r[1]
                traffic = r[2]

                if client_intf in self.__wan_interfaces:
                    data[client_intf] = data.get(client_intf, 0) + traffic
                if server_intf in self.__wan_interfaces:
                    data[server_intf] = data.get(server_intf, 0) + traffic

            for k, v in data.iteritems():
                iface = self.__interface_names.get(k, _('Unknown'))
                ks = KeyStatistic(iface, v, _('MB'))
                lks.append(ks)
                dataset[iface] = v
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Interface'),
                     ylabel=_('MB'))
        plot.add_pie_dataset(dataset)

        return (lks, plot)

reports.engine.register_node(WanBalancer())
