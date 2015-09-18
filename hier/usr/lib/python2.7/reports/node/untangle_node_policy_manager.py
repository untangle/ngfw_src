import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
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
from uvm.settings_reader import get_node_settings_item

from reports.log import *
logger = getLogger(__name__)

_ = uvm.i18n_helper.get_translation('untangle-node-policy-manager').lgettext
def N_(message): return message

class PolicyManager(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-policy-manager','Policy Manager')
        self.__policy_names = self.__get_policy_names()

    @sql_helper.print_timing
    def setup(self):
        pass

    def create_tables(self):
        pass

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [PolicyHighlight(self.name),
                            BandwidthUsage(self.__policy_names),
                            DailySessionsByPolicy(self.__policy_names),
                            DailyTrafficByPolicy(self.__policy_names),
                            TopTenPoliciesBySessions(self.__policy_names),
                            TopTenPoliciesByBandwidth(self.__policy_names)])
        sections.append(s)

        return Report(self, sections)

    def __get_policy_names(self):
        policies = get_node_settings_item("untangle-node-policy","policies")                            
        pn = {}
        pn[0] = _("No Rack")
        if policies != None:
            for policy in policies['list']:
		policyId = int(policy['policyId'])
		# graphing library doesnt handle unicode - change to ascii
                pn[policyId] = policy['name'].encode('ascii','replace')
        return pn

class PolicyHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("mapped") + " " + "%(sessions)s" + " " +
                           _("sessions across") + " " +
                           "%(racks)s" + " " + _("racks"))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT COALESCE(sum(new_sessions), 0)::int AS sessions,
       COALESCE(COUNT(DISTINCT(policy_id)),0) AS racks
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h


class BandwidthUsage(Graph):
    def __init__(self, policy_names):
        Graph.__init__(self, 'bandwidth-usage', _('Bandwidth Usage'))
        self.__policy_names = policy_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        lks = []
            
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            # kB
            sums = ["ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000)::numeric, 2)"]

            if host:
                extra_where = ("hostname = %(host)s", { 'host' : host })
            elif user:
                extra_where = ("username = %(user)s" , { 'user' : user })
            else:
                extra_where = []

            time_interval = 60
            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 extra_fields = ["policy_id",],
                                                 time_interval = time_interval)
            curs.execute(q, h)

            data = {}
            for r in curs.fetchall():
                policy_id = r[1]
                t = data.get(policy_id, None)
                if not t:
                    t = ([], [])
                    data[policy_id] = t

                dates, values = t

                dates.append(r[0])
                values.append(float(r[2]) / time_interval)
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Throughput (kB/s)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        for policy_id, t in data.iteritems():
            policy_name = self.__policy_names.get(policy_id, _('No Rack'))
            dates, values = t
            if not values:
                values = [0,]
            plot.add_dataset(dates, values, policy_name)

            ks = KeyStatistic(_('%(rack)s Avg') %
                              { 'rack': policy_name },
                              "%.2f" % (sum(values)/len(values)), N_('kB/sec'))
            lks.append(ks)
            ks = KeyStatistic(_('%(rack)s Total') %
                              { 'rack': policy_name },
                              time_interval * sum(values), N_('kB'))
            lks.append(ks)

        return (lks, plot)

class DailySessionsByPolicy(Graph):
    def __init__(self, policy_names):
        Graph.__init__(self, 'sessions-by-policy', _('Sessions By Policy'))
        self.__policy_names = policy_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        lks = []
            
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            # kB
            sums = ["COALESCE(SUM(new_sessions), 0)"]

            if host:
                extra_where = ("hostname = %(host)s", { 'host' : host })
            elif user:
                extra_where = ("username = %(user)s" , { 'user' : user })
            else:
                extra_where = []

            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 extra_fields = ["policy_id",])
            curs.execute(q, h)

            data = {}
            for r in curs.fetchall():
                policy_id = r[1]
                t = data.get(policy_id, None)
                if not t:
                    t = ([], [])
                    data[policy_id] = t

                dates, values = t

                dates.append(r[0])
                values.append(r[2])
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Sessions'),
                     major_formatter=TIMESTAMP_FORMATTER)

        for policy_id, t in data.iteritems():
            policy_name = self.__policy_names.get(policy_id, _('No Rack'))
            dates, values = t
            if not values:
                values = [0,]
            plot.add_dataset(dates, values, policy_name)

            ks = KeyStatistic(_('%(rack)s Avg') %
                              { 'rack': policy_name },
                              sum(values)/len(values),
                              N_('Sessions')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('%(rack)s Total') %
                              { 'rack': policy_name },
                              max(values),
                              N_('Sessions')+'/'+_(unit))
            lks.append(ks)

        return (lks, plot)

class DailyTrafficByPolicy(Graph):
    def __init__(self, policy_names):
        Graph.__init__(self, 'traffic-by-policy', _('Traffic By Policy'))
        self.__policy_names = policy_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        lks = []
            
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            # kB
            sums = ["ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000)::numeric, 2)"]

            if host:
                extra_where = ("hostname = %(host)s", { 'host' : host })
            elif user:
                extra_where = ("username = %(user)s" , { 'user' : user })
            else:
                extra_where = []

            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 extra_fields = ["policy_id",])
            curs.execute(q, h)

            data = {}
            for r in curs.fetchall():
                policy_id = r[1]
                t = data.get(policy_id, None)
                if not t:
                    t = ([], [])
                    data[policy_id] = t

                dates, values = t

                dates.append(r[0])
                values.append(r[2])
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Time'), ylabel=_('Throughput (kB/s)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        for policy_id, t in data.iteritems():
            policy_name = self.__policy_names.get(policy_id, _('No Rack'))
            dates, values = t
            if not values:
                values = [0,]
            plot.add_dataset(dates, values, policy_name)

            ks = KeyStatistic(_('%(rack)s Avg') %
                              { 'rack': policy_name },
                              "%.2f" % (sum(values)/len(values)), N_('kB/sec'))
            lks.append(ks)
            ks = KeyStatistic(_('%(rack)s Total') %
                              { 'rack': policy_name },
                              max(values), N_('kB'))
            lks.append(ks)

        return (lks, plot)

class DailySessionsByPolicy(Graph):
    def __init__(self, policy_names):
        Graph.__init__(self, 'sessions-by-policy', _('Sessions By Policy'))
        self.__policy_names = policy_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        lks = []
            
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            # kB
            sums = ["COALESCE(SUM(c2s_bytes+s2c_bytes), 0)::float / 1000000"]

            if report_days == 1:
                unit = "Hour"
            else:
                unit = "Day"

            extra_where = [ ("policy_id IS NOT NULL", {}) ]
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 extra_fields = ["policy_id",])
            curs.execute(q, h)

            data = {}
            for r in curs.fetchall():
                policy_id = r[1]
                t = data.get(policy_id, None)
                if not t:
                    t = ([], [])
                    data[policy_id] = t

                dates, values = t

                dates.append(r[0])
                values.append(r[2])

            plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                         xlabel=_('Time'), ylabel=_('Sessions'),
                         major_formatter=TIMESTAMP_FORMATTER)

            for policy_id, t in data.iteritems():
                policy_name = self.__policy_names.get(policy_id, _('No Rack'))
                dates, values = t
                if not values:
                    values = [0,]
                plot.add_dataset(dates, values, policy_name)

                ks = KeyStatistic(_('%(rack)s Avg') %
                                  { 'rack': policy_name },
                                  sum(values)/len(values),
                                  N_('Mb')+'/'+_(unit))
                lks.append(ks)
                ks = KeyStatistic(_('%(rack)s Total') %
                                  { 'rack': policy_name },
                                  max(values),
                                  N_('Mb')+'/'+_(unit))
                lks.append(ks)

        finally:
            conn.commit()

        return (lks, plot)

class TopTenPoliciesBySessions(Graph):
    def __init__(self, policy_names):
        Graph.__init__(self, 'top-web-policies-by-sesssions', _('Top Policies By Sessions'))
        self.__policy_names = policy_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT policy_id,
       COALESCE(sum(new_sessions), 0)::int AS sessions
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
GROUP BY policy_id
ORDER BY sessions DESC"""

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()
            curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                policy_name = self.__policy_names.get(r[0], _('No Rack'))
                ks = KeyStatistic(policy_name, r[1], _('Sessions'))
                lks.append(ks)
                dataset[policy_name] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Policy'),
                     ylabel=_('Blocks Per Day'))
        plot.add_pie_dataset(dataset)

        return (lks, plot, 10)

class TopTenPoliciesByBandwidth(Graph):
    def __init__(self, policy_names):
        Graph.__init__(self, 'top-web-policies-by-bandwidth', _('Top Policies By Bandwidth'))
        self.__policy_names = policy_names

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT policy_id,
       COALESCE(sum(c2s_bytes + s2c_bytes)/1000000, 0)::bigint AS bandwidth
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
GROUP BY policy_id
ORDER BY bandwidth DESC"""

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()
            curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                policy_name = self.__policy_names.get(r[0], _('No Rack'))
                ks = KeyStatistic(policy_name, r[1], _('MB'))
                lks.append(ks)
                dataset[policy_name] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Policy'),
                     ylabel=_('MB'))
        plot.add_pie_dataset(dataset)

        return (lks, plot, 10)

reports.engine.register_node(PolicyManager())

