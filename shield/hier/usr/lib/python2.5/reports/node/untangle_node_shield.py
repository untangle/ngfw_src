import gettext
import logging
import mx
import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper

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
from reports import SummarySection
from reports import TIMESTAMP_FORMATTER
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-shield').lgettext

class Shield(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-shield')

    @print_timing
    def setup(self, start_date, end_date, start_time):
        self.__create_n_shield_rejection_totals(start_date, end_date)
        self.__create_n_shield_totals(start_date, end_date)

    @sql_helper.print_timing
    def events_cleanup(self, cutoff):
        sql_helper.clean_table("events", "n_shield_rejection_evt", cutoff);
        sql_helper.clean_table("events", "n_shield_statistic_evt", cutoff);

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table("n_shield_rejection_totals", cutoff)
        sql_helper.drop_fact_table("n_shield_totals", cutoff)        

    @print_timing
    def __create_n_shield_rejection_totals(self, start_date, end_date):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.n_shield_rejection_totals (
    time_stamp timestamp without time zone,
    client_addr inet,
    client_intf integer,
    mode        integer,
    reputation  float8,
    limited     integer,
    dropped     integer,
    rejected    integer,
    event_id bigserial)""",  'time_stamp', start_date, end_date)

        # old tables did not have event_id
        sql_helper.add_column('reports', 'n_shield_rejection_totals', 'event_id', 'bigserial')
        # old tables did not have reputation
        sql_helper.add_column('reports', 'n_shield_rejection_totals', 'reputation', 'float8')

        # convert from old name
        sql_helper.rename_column("reports","n_shield_rejection_totals","trunc_time","time_stamp");

        sql_helper.create_index("reports","n_shield_rejection_totals","event_id");
        sql_helper.create_index("reports","n_shield_rejection_totals","time_stamp");

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_shield_rejection_totals
      (time_stamp, client_addr, client_intf, mode, reputation, limited, dropped, rejected)
SELECT time_stamp, client_addr, client_intf, mode, reputation, limited, dropped, rejected
FROM events.n_shield_rejection_evt""", (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def __create_n_shield_totals(self, start_date, end_date):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.n_shield_totals (
    time_stamp timestamp without time zone,
    accepted   integer,
    limited    integer,
    dropped    integer,
    rejected   integer)""",  'time_stamp', start_date, end_date)

        # convert from old name
        sql_helper.rename_column("reports","n_shield_totals","trunc_time","time_stamp");

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_shield_totals
      (time_stamp, accepted, limited, dropped, rejected)
SELECT time_stamp, accepted, limited, dropped, rejected
FROM events.n_shield_statistic_evt""", (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [ShieldHighlight(self.name),
                            DailyRequest(),
                            BlockedHosts(),
                            LimitedHosts()])
        sections.append(s)

        sections.append(ShieldDetail())

        return Report(self, sections)

class ShieldHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("scanned") + " " + "%(sessions)s" + " " +
                           _("sessions, of which it limited") +
                           " " + "%(limited)s" + " " + _("and dropped") +
                           " " + "%(dropped)s")

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        query = """
SELECT COALESCE(sum(accepted+limited+dropped+rejected), 0) AS sessions,
       COALESCE(sum(limited), 0) AS limited,
       COALESCE(sum(dropped), 0) AS dropped
FROM reports.n_shield_totals"""

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, ())

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h

class DailyRequest(Graph):
    def __init__(self):
        Graph.__init__(self, 'requests', _('Requests'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []
        
        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["coalesce(sum(accepted), 0)",
                    "coalesce(sum(limited), 0)",
                    "coalesce(sum(dropped+rejected), 0)"]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_shield_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_field = "time_stamp")
            curs.execute(q, h)

            times = []
            accepted = []
            limited = []
            blocked = []

            for r in curs.fetchall():
                times.append(r[0])
                accepted.append(r[1])
                limited.append(r[2])
                blocked.append(r[3])

            if not accepted:
                accepted = [0,]
            if not limited:
                limited = [0,]
            if not blocked:
                blocked = [0,]

            ks = KeyStatistic(_('Avg Requests'), sum(accepted+limited+blocked)/len(accepted),
                              _('Sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Requests'), sum(accepted+limited+blocked),
                              _('Sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Limited'), sum(limited)/len(accepted),
                              _('Sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Limited'), sum(limited),
                              _('Sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Blocked'), sum(blocked)/len(accepted),
                              _('Sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Blocked'), sum(blocked),
                              _('Sessions/minute'))
            lks.append(ks)

            plot = Chart(type=TIME_SERIES_CHART,
                         title=_('Request'),
                         xlabel=_('Date'),
                         ylabel=_('Requests Per Minute'),
                         major_formatter=TIMESTAMP_FORMATTER)

            plot.add_dataset(times, accepted, label=_('Accepted'))
            plot.add_dataset(times, limited, label=_('Limited'))
            plot.add_dataset(times, blocked, label=_('Blocked'))
                
        finally:
            conn.commit()

        return (lks, plot)

class BlockedHosts(Graph):
    def __init__(self):
        Graph.__init__(self, 'blocked-hosts', _('Top Blocked Hosts'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_addr, sum(dropped + rejected) AS blocked
FROM reports.n_shield_rejection_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND ((dropped > 0) OR (rejected > 0))
GROUP BY client_addr
ORDER BY blocked desc"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                host = r[0]
                num = r[1]

                lks.append(KeyStatistic(host, num, _('Blocks'),
                           link_type=reports.HNAME_LINK))
                pds[host] = num
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Blocked Hosts'))

        plot.add_pie_dataset(pds, display_limit=10)


        return (lks, plot, 10)

class LimitedHosts(Graph):
    def __init__(self):
        Graph.__init__(self, 'limited-hosts', _('Top Limited Hosts'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_addr, sum(limited) AS limited
FROM reports.n_shield_rejection_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND limited > 0
GROUP BY client_addr
ORDER BY limited desc"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                host = r[0]
                num = r[1]

                lks.append(KeyStatistic(host, num, _('Limited'),
                                        link_type=reports.HNAME_LINK))
                pds[host] = num
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=_('Top Limited Hosts'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class ShieldDetail(DetailSection):

    def __init__(self):
        DetailSection.__init__(self, 'attack-events', _('Attack Events'))

    def get_columns(self, host=None, user=None, email=None):
        if user or email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        rv = rv + [ColumnDesc('client_addr', _('Client')),
                   ColumnDesc('limited', _('Limited')),
                   ColumnDesc('dropped', _('Dropped')),
                   ColumnDesc('rejected', _('Rejected'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if user or email:
            return None

        sql = "SELECT time_stamp, "

        sql = sql + ("""host(client_addr), limited, dropped, rejected
FROM reports.n_shield_rejection_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
      AND (limited + dropped + rejected) > 0""" % (DateFromMx(start_date),
                                                   DateFromMx(end_date)))

        if host:
            sql += " AND client_addr = %s" % (QuotedString(host),)

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(Shield())

