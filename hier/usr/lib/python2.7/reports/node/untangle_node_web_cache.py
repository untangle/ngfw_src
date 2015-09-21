import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import sys
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
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle-node-web-cache').lgettext

class WebCache(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-web-cache','Web Cache')

    @sql_helper.print_timing
    def setup(self):
        return

    def create_tables(self):
        self.__create_web_cache_stats()

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []
        s = reports.SummarySection('summary', _('Summary Report'),
                                   [WebCacheHighlight(self.name),
                                    WebCacheUsage(),
                                    WebCacheUsageBySize()])
        sections.append(s)

        sections.append(WebCacheDetail())

        return reports.Report(self, sections)

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("web_cache_stats", cutoff)

    def __create_web_cache_stats( self ):
        sql_helper.create_table("""\
CREATE TABLE reports.web_cache_stats (
    time_stamp timestamp without time zone,
    hits bigint,
    misses bigint,
    bypasses bigint,
    systems bigint,
    hit_bytes bigint,
    miss_bytes bigint,
    event_id bigserial)""",["event_id"],["time_stamp"])

        sql_helper.rename_table("webcache_stats","web_cache_stats") # 12.0

class WebCacheHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("handled") + " " + "%(total)s" + " " +
                           _("MB") + " " + _("of traffic, of which ") + " " +
                           "%(cached)s" + " " + _("MB") + " " +
                           _("were obtained directly from the cache"))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if email or user or host:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT ROUND((COALESCE(SUM(hit_bytes+miss_bytes),0)/1000000)::numeric, 2) AS total,
       ROUND((COALESCE(SUM(hit_bytes), 0)/1000000)::numeric, 2) AS cached
FROM reports.web_cache_stats
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

class WebCacheUsage(reports.Graph):
    def __init__(self):
        reports.Graph.__init__(self, 'usage', _('Usage'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER

            sums = ["SUM(hits)", "SUM(misses)", "SUM(bypasses+systems)"]

            extra_where = []

            q, h = sql_helper.get_averaged_query(sums, "reports.web_cache_stats",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval,
                                                 time_field = 'time_stamp')
            curs.execute(q, h)

            dates = []
            hits = []
            misses = []
            bypasses = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                hits.append(r[1])
                misses.append(r[2])
                bypasses.append(r[3])

            if not hits:
                hits = [0,]
            if not misses:
                misses = [0,]
            if not bypasses:
                bypasses = [0,]
                
            rp = sql_helper.get_required_points(start_date, end_date,
                                                mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = reports.KeyStatistic(_('Avg Cached'), int(sum(hits)/len(rp)),
                                      _('Hits')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Cached'), max(hits),
                                      _('Hits')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Avg Not Cached'), int(sum(misses)/len(rp)),
                                      _('Hits')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Not Cached'), max(misses),
                                      _('Hits')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Avg Bypassed'), int(sum(bypasses)/len(rp)),
                                      _('Hits')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Bypassed'), max(bypasses),
                                      _('Hits')+'/'+_(unit))
            lks.append(ks)


            plot = reports.Chart(type=reports.STACKED_BAR_CHART,
                                 title=_('Hits'),
                                 xlabel=_(unit),
                                 ylabel=_('Hits'),
                                 major_formatter=formatter,
                                 required_points=rp)

            plot.add_dataset(dates, hits, label=_('Cached'),
                             color=colors.goodness)
            plot.add_dataset(dates, misses, label=_('Not Cached'),
                             color=colors.badness)
            plot.add_dataset(dates, bypasses, label=_('Bypassed'),
                             color=colors.detected)

        finally:
            conn.commit()

        return (lks, plot)

class WebCacheUsageBySize(reports.Graph):
    def __init__(self):
        reports.Graph.__init__(self, 'usage-by-size', _('Usage by size'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER

            sums = ["ROUND((COALESCE(SUM(hit_bytes), 0) / 1000000)::numeric, 2)",
                    "ROUND((COALESCE(SUM(miss_bytes), 0) / 1000000)::numeric, 2)"]

            extra_where = []

            q, h = sql_helper.get_averaged_query(sums, "reports.web_cache_stats",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval,
                                                 time_field = 'time_stamp')
            curs.execute(q, h)

            dates = []
            hits = []
            misses = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                hits.append(r[1])
                misses.append(r[2])

            if not hits:
                hits = [0,]
            if not misses:
                misses = [0,]
                
            rp = sql_helper.get_required_points(start_date, end_date,
                                                mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = reports.KeyStatistic(_('Avg Cached'), round(sum(hits)/len(rp), 2),
                                      _('MB')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Cached'), max(hits),
                                      _('MB')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Avg Not Cached'), round(sum(misses)/len(rp), 2),
                                      _('MB')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Not Cached'), max(misses),
                                      _('MB')+'/'+_(unit))
            lks.append(ks)

            plot = reports.Chart(type=reports.STACKED_BAR_CHART,
                                 title=_('Size'),
                                 xlabel=_(unit),
                                 ylabel=_('MB'),
                                 major_formatter=formatter,
                                 required_points=rp)

            plot.add_dataset(dates, hits, label=_('Cached'),
                             color=colors.goodness)
            plot.add_dataset(dates, misses, label=_('Not Cached'),
                             color=colors.badness)

        finally:
            conn.commit()

        return (lks, plot)

class WebCacheDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'web-cache-events', _('Web Cache Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email or host or user:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        rv = rv + [ColumnDesc('hits', _('Hits')),
                   ColumnDesc('misses', _('Misses')),
                   ColumnDesc('bypasses', _('Bypasses')),
                   ColumnDesc('systems', _('Systems')),
                   ColumnDesc('hit_bytes', _('Hit Bytes')),
                   ColumnDesc('miss_bytes', _('Miss Bytes'))]

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email or host or user:
            return None

        sql = "SELECT time_stamp,"

        sql = sql + ("""hits, misses, bypasses, systems, hit_bytes, miss_bytes
FROM reports.web_cache_stats
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone""" % (DateFromMx(start_date),
                                                 DateFromMx(end_date)))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(WebCache())
