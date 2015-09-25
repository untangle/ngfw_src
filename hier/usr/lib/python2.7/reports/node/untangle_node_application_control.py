# $HeadURL: https://untangle.svn.beanstalkapp.com/ngfw/hades/src/application-control/hier/usr/lib/python2.7/reports/node/untangle_node_application_control.py $
import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import sys
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
from reports import TIMESTAMP_FORMATTER
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle').lgettext

class ApplicationControl(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-application-control', 'Application Control')

    @sql_helper.print_timing
    def setup(self):
        ft = reports.engine.get_fact_table('reports.session_totals')

        ft.measures.append(Column('application_control_blocks', 'integer', "count(CASE WHEN application_control_blocked THEN 1 ELSE null END)"))
        ft.measures.append(Column('application_control_flags', 'integer', "count(CASE WHEN application_control_flagged THEN 1 ELSE null END)"))

        ft.dimensions.append(Column('application_control_application', 'text'))
        ft.dimensions.append(Column('application_control_protochain', 'text'))

    def create_tables(self):
        return

    def parents(self):
        return ['untangle-vm',]

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []
        s = reports.SummarySection('summary', _('Summary Report'),
                                   [ApplicationControlHighlight(self.name),
                                    DailyUsage(),
                                    TopTenBlockedByHits(type="application"),
                                    TopTenFlaggedByHits(type="application"),
                                    TopTenDetectedByHits(type="application"),
                                    TopTenBlockedByHits(type="protochain"),
                                    TopTenFlaggedByHits(type="protochain"),
                                    TopTenDetectedByHits(type="protochain"),
                                    TopTenBlockedHostsByHits(),
                                    TopTenFlaggedHostsByHits(),
                                    TopTenDetectedHostsByHits(),
                                    TopTenBlockedUsersByHits(),
                                    TopTenFlaggedUsersByHits(),
                                    TopTenDetectedUsersByHits(),
                                    TopTenApplicationsByBandwidth(),
                                    TopTenProtochainByBandwidth(),
                                    ])
        sections.append(s)

        sections.append(ApplicationControlDetail())

        return reports.Report(self, sections)

    def reports_cleanup(self, cutoff):
        pass

class ApplicationControlHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("identified") + " " + "%(applications)s" + " " +
                           _("of") + " " +
                           "%(sessions)s" + " " + _("sessions of which") +
                           " " + "%(flags)s" + " " + _("were flagged and") +
                           " " + "%(blocks)s" + " " + _("were blocked"))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(SUM(new_sessions),0)::int AS sessions,
       COALESCE(sum(CASE WHEN NULLIF(application_control_application,'') IS NULL THEN 0 ELSE 1 END), 0)::int AS applications,
       COALESCE(sum(application_control_flags), 0)::int AS flags,
       COALESCE(sum(application_control_blocks), 0)::int AS blocks
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

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

class DailyUsage(reports.Graph):
    def __init__(self):
        reports.Graph.__init__(self, 'sessions', _('Sessions'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
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

            sums = ["count(CASE WHEN application_control_application is not null AND NOT application_control_flagged AND NOT application_control_blocked THEN 1 ELSE null END)",
                    "count(CASE WHEN application_control_flagged AND NOT application_control_blocked THEN 1 ELSE null END)",
                    "count(CASE WHEN application_control_blocked THEN 1 ELSE null END)"]

            extra_where = []
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.sessions",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval,
                                                 time_field = 'time_stamp')
            curs.execute(q, h)

            dates = []
            detections = []
            flags = []
            blocks = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                detections.append(r[1])
                flags.append(r[2])
                blocks.append(r[3])

            if not detections:
                detections = [0,]
            if not flags:
                flags = [0,]
            if not blocks:
                blocks = [0,]
                
            rp = sql_helper.get_required_points(start_date, end_date,
                                                mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = reports.KeyStatistic(_('Avg Detected'), sum(detections + flags + blocks)/len(rp), _('Detections')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Detected'), max(detections + flags + blocks), _('Detections')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Avg Flagged'), sum(flags + blocks)/len(rp), _('Flags')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Flagged'), max(flags + blocks), _('Flags')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Avg Blocked'), sum(blocks)/len(rp), _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Blocked'), max(blocks), _('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = reports.Chart(type=reports.STACKED_BAR_CHART,
                                 title=_('Sessions'),
                                 xlabel=_(unit),
                                 ylabel=_('Sessions'),
                                 major_formatter=formatter,
                                 required_points=rp)

            plot.add_dataset(dates, blocks, label=_('Blocked'), color=colors.badness)
            plot.add_dataset(dates, flags, label=_('Flagged'), color=colors.detected)
            plot.add_dataset(dates, detections, label=_('Detections'), color=colors.goodness)

        finally:
            conn.commit()

        return (lks, plot)

class TopTenBlockedByHits(Graph):
    def __init__(self, type="application"):
        self.type = type
        self.typeName = _(string.capitalize(self.type))
        Graph.__init__(self, 'top-blocked-' + self.type + '-by-hits', _('Top Blocked ') + self.typeName + _('By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT application_control_%s, COALESCE(sum(application_control_blocks), 0)::int as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %%s AND time_stamp < %%s""" % self.type

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY application_control_%s ORDER BY hits_sum DESC" % self.type

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'))
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Blocked') + ' ' + self.typeName + ' ' + _('(by Hits)'),
                     xlabel=_(self.typeName),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenFlaggedByHits(Graph):
    def __init__(self, type="application"):
        self.type = type
        self.typeName = _(string.capitalize(self.type))
        Graph.__init__(self, 'top-flagged-' + self.type + '-by-hits', _('Top Flagged') + ' ' + self.typeName + ' ' + _('By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT application_control_%s, COALESCE(sum(application_control_flags), 0)::int as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %%s AND time_stamp < %%s""" % self.type

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY application_control_%s ORDER BY hits_sum DESC" % self.type

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'))
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Flagged') + ' ' + self.typeName + ' ' + _('(by Hits)'),
                     xlabel=_(self.typeName),
                     ylabel=_('Flags Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)


class TopTenDetectedByHits(Graph):
    def __init__(self, type="application"):
        self.type = type
        self.typeName = _(string.capitalize(self.type))
        Graph.__init__(self, 'top-detected-' + self.type + '-by-hits', _('Top Detected') + ' ' + self.typeName + ' ' + _('By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT application_control_%s, count(*) as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %%s AND time_stamp < %%s
      AND application_control_application != ''
""" % self.type

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query = query + " GROUP BY application_control_%s ORDER BY hits_sum DESC" % self.type

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'))
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Detected') + ' ' + self.typeName + ' ' + _('(by Hits)'),
                     xlabel=_(self.typeName),
                     ylabel=_('Detections Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-blocked-hosts-by-hits', _('Top Blocked Hosts By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hostname, COALESCE(sum(application_control_blocks), 0)::int as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND NOT application_control_application IS NULL
AND application_control_application != ''
"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query = query + " GROUP BY hostname ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'),
                                      link_type=reports.HNAME_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Blocked Hosts (by Hits)'),
                     xlabel=_('Host'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenFlaggedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-flagged-hosts-by-hits',
                       _('Top Flagged Hosts By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hostname, COALESCE(sum(application_control_flags), 0)::int as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND application_control_application != ''
"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query +=" GROUP BY hostname ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.HNAME_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Flagged Hosts (by Hits)'),
                     xlabel=_('Host'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenDetectedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-detected-hosts-by-hits',
                       _('Top Hosts By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hostname, count(*) as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND application_control_application != ''
"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query +=" GROUP BY hostname ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.HNAME_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Detected Hosts (by Hits)'),
                     xlabel=_('Host'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockedUsersByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-blocked-users-by-hits', _('Top Blocked Users By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT username, COALESCE(sum(application_control_blocks),0)::int as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND username != ''
AND NOT application_control_application IS NULL
AND application_control_application != ''
"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query = query + " GROUP BY username ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.USER_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Blocked Users (by Hits)'),
                     xlabel=_('User'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenFlaggedUsersByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-flagged-users-by-hits', _('Top Flagged Users By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT username, COALESCE(sum(application_control_flags), 0)::int as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND username != ''
AND application_control_application != ''
"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY username ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.USER_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Flagged Users (by Hits)'),
                     xlabel=_('User'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenDetectedUsersByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-detected-users-by-hits', _('Top Users By Hits'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT username, count(*) as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND username != ''
AND application_control_application != ''
"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY username ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.USER_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Detected Users (by Hits)'),
                     xlabel=_('User'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenApplicationsByBandwidth(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-by-application', _('Top Bandwidth Applications'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT application_control_application, ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000000)::numeric, 2) as bw
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY application_control_application ORDER BY bw DESC"


        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                port = r[0]
                bw = r[1]
                ks = KeyStatistic(str(port), bw, _('MB'))
                lks.append(ks)
                pds[port] = bw
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Host'), ylabel=_('Application'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class TopTenProtochainByBandwidth(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-by-protochain', _('Top Bandwidth Protochains'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT application_control_protochain, ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000000)::numeric, 2) as bw
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY application_control_protochain ORDER BY bw DESC"


        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                port = r[0]
                bw = r[1]
                ks = KeyStatistic(str(port), bw, _('MB'))
                lks.append(ks)
                pds[port] = bw
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Host'), ylabel=_('Protochain'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class ApplicationControlDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'detection-events', _('Detection Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if not host:
            rv.append(ColumnDesc('hostname', _('Client'), 'HostLink'))
        if not user:
            rv.append(ColumnDesc('username', _('User'), 'UserLink'))

        rv = rv + [ColumnDesc('c_server_addr', _('Server')),
                   ColumnDesc('c_server_port', _('Port')),
                   ColumnDesc('application_control_application', _('Application')),
                   ColumnDesc('application_control_protochain', _('ProtoChain')),
                   ColumnDesc('application_control_blocked', _('Blocked')),
                   ColumnDesc('application_control_flagged', _('Flagged')),
                   ColumnDesc('application_control_detail', _('Detail')),
                   ColumnDesc('application_control_rule', _('Rule'))]

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_session_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = "SELECT * "

        sql = sql + ("""FROM reports.sessions
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND application_control_application IS NOT NULL
AND application_control_application != ''""" % (DateFromMx(start_date),
                                   DateFromMx(end_date)))

        if host:
            sql = sql + (" AND hostname = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND username = %s" % QuotedString(user))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(ApplicationControl())
