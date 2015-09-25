import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
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
from reports import TIMESTAMP_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN

_ = uvm.i18n_helper.get_translation('untangle').lgettext

def N_(message): return message

class SslInspector(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-casing-ssl-inspector','SSL')

    def parents(self):
        return ['untangle-vm']

    @sql_helper.print_timing
    def setup(self):
        ft = reports.engine.get_fact_table('reports.session_totals')
        ft.measures.append(Column('ssl_abandoned', 'integer', """count(CASE WHEN ssl_inspector_status = 'ABANDONED' THEN 1 ELSE null END)"""))
        ft.measures.append(Column('ssl_ignored', 'integer', """count(CASE WHEN ssl_inspector_status = 'IGNORED' THEN 1 ELSE null END)"""))
        ft.measures.append(Column('ssl_untrusted', 'integer', """count(CASE WHEN ssl_inspector_status = 'UNTRUSTED' THEN 1 ELSE null END)"""))
        ft.measures.append(Column('ssl_inspected', 'integer', """count(CASE WHEN ssl_inspector_status = 'INSPECTED' THEN 1 ELSE null END)"""))
        ft.dimensions.append(Column('ssl_inspector_detail', 'text'))

    def create_tables(self):
        return

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [SslHighlight(self.name),
                            TopInspected(),
                            TopUntrusted(),
                            SslStatusChart()])
        sections.append(s)

        return Report(self, sections)

    def reports_cleanup(self, cutoff):
        pass

class SslHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("inspected") + " " + "%(inspected)s" + " " + 
                           _("sites, ignored") + " " + "%(ignored)s" + " " + 
                           _("sites, abandoned") + " " + "%(abandoned)s" + " " +
                           _("sites and found ") + " " + "%(untrusted)s" + " " +
                           _("untrusted sites."))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))
        
        query = """
SELECT COALESCE(sum(ssl_inspected), 0)::int AS inspected,
       COALESCE(sum(ssl_abandoned), 0) AS abandoned,
       COALESCE(sum(ssl_ignored), 0) AS ignored,
       COALESCE(sum(ssl_untrusted), 0) AS untrusted
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
"""
        if host:
            query = query + " AND hostname = %s"
        elif user:
            query = query + " AND username = %s"

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h


class TopInspected(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ssl-inspected',
                       _('Top Inspected Sites'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT ssl_inspector_detail, COALESCE(sum(ssl_inspected), 0)::int AS hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND ssl_inspector_detail IS NOT NULL and ssl_inspected > 0
""" 
        if host:
            query = query + " AND hostname = %s"
        elif user:
            query = query + " AND username = %s"
        query += """
GROUP BY ssl_inspector_detail ORDER BY hits_sum DESC
"""
        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                name = r[0] if r[0] is not None else "none"
                ks = KeyStatistic(name, r[1], _('Hits'), link_type=reports.URL_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)
    
class TopUntrusted(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ssl-untrusted',
                       _('Top Untrusted Sites'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT ssl_inspector_detail, COALESCE(sum(ssl_untrusted), 0)::int AS hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND ssl_inspector_detail IS NOT NULL AND ssl_untrusted > 0
""" 
        if host:
            query = query + " AND hostname = %s"
        elif user:
            query = query + " AND username = %s"
        query += """
GROUP BY ssl_inspector_detail ORDER BY hits_sum DESC
"""
        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                name = r[0] if r[0] is not None else "none"
                ks = KeyStatistic(name, r[1], _('Hits'), link_type=reports.URL_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)    

class SslStatusChart(Graph):
    def __init__(self):
        Graph.__init__(self, 'ssl-status-chart',
                       _('Inspected Vs Ignored Vs Abandoned'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT COALESCE(sum(ssl_inspected), 0)::int AS inspected,
       COALESCE(sum(ssl_abandoned), 0) AS abandoned,
       COALESCE(sum(ssl_ignored), 0) AS ignored
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
"""
        if host:
            query = query + " AND hostname = %s"
        elif user:
            query = query + " AND username = %s"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                ks = KeyStatistic('INSPECTED', r[0], _('Hosts'))
                lks.append(ks)
                dataset['INSPECTED'] = r[0]
                
                ks = KeyStatistic('ABANDONED', r[1], _('Hosts'))
                lks.append(ks)
                dataset['ABANDONED'] = r[1]
                
                ks = KeyStatistic('IGNORED', r[2], _('Hosts'))
                lks.append(ks)
                dataset['IGNORED'] = r[2]
                
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Status'),
                     ylabel=_('Hosts'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

reports.engine.register_node(SslInspector())
