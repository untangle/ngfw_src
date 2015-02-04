"""
Intrusion Detection & Prevention Reports
"""
import mx
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
from reports import Chart
from reports import ColumnDesc
from reports import DetailSection
from reports import Graph
from reports import Highlight
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import STACKED_BAR_CHART
from reports import SummarySection
from reports import TIMESTAMP_FORMATTER
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle-node-idps').lgettext

class Idps(Node):
    """
    IDPS
    """
    def __init__(
        self, node_name, title, vendor_name
        ):
        Node.__init__(
            self, 
            node_name,
            _('Intrusion Prevention')
        )

        self.__title = title
        self.__vendor_name = vendor_name

    @print_timing
    def setup(self):
        self.__create_idps_events()
        
    @print_timing
    def __create_idps_events(self):
        """
        Generate table and indices
        """
        sql_helper.create_fact_table("""\
CREATE TABLE reports.idps_events (
        time_stamp timestamp NOT NULL,
        sig_id int8,
        gen_id int8,
        class_id int8,
        source_addr inet,
        source_port int4,
        dest_addr inet,
        dest_port int4,
        protocol int4,
        blocked boolean,
        category text,
        classtype text,
        msg text)""")
        
        if not sql_helper.index_exists("reports","idps_events","time_stamp"):
            sql_helper.create_index("reports", "idps_events", "time_stamp")
        if not sql_helper.index_exists("reports", "idps_events", "blocked"):
            sql_helper.create_index("reports", "idps_events", "blocked")
        if not sql_helper.index_exists("reports", "idps_events", "msg"):
            sql_helper.create_index("reports", "idps_events", "msg")
        if not sql_helper.index_exists("reports", "idps_events", "category"):
            sql_helper.create_index("reports", "idps_events", "category")
        if not sql_helper.index_exists("reports", "idps_events", "classtype"):
            sql_helper.create_index("reports", "idps_events", "classtype")

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        return Report( 
            self, [
                SummarySection(
                    'summary', 
                    _('Summary Report'), [
                        IdpsHighlight(self.name),
                        DailyLogUsage(self.__vendor_name),
                        DailyBlockUsage(self.__vendor_name),
                        TopTenRulesByHits(self.__vendor_name),
                        TopTenClasstypesByHits(self.__vendor_name),
                        TopTenCategoriesByHits(self.__vendor_name)
                    ]),
                IdpsDetail()
            ])

    def parents(self):
        return ['untangle-vm']

    def reports_cleanup(self, cutoff):
        pass

class IdpsHighlight(Highlight):
    """
    IDPS overview
    """
    def __init__(self, name):
        Highlight.__init__(
            self, 
            name,
            _(name) + " " +
                _("detected") + " " +
                "%(attacks)s" + " " + _("attacks of which") +
                " " + "%(blocks)s" + " " + _("were blocked")
        )

    @print_timing
    def get_highlights(
        self, end_date, report_days,
        host=None, user=None, email=None):
        """
        Get overview
        """
        if email:
            return None

        date_end_date = DateFromMx( end_date )
        one_week = DateFromMx( 
            end_date - mx.DateTime.DateTimeDelta(report_days) 
        )

        query = """
SELECT count(*)::int AS attacks,
       count(case when blocked = true then 1 end)::int AS blocks
 FROM reports.idps_events
WHERE time_stamp >= %s::timestamp without time zone 
  AND time_stamp < %s::timestamp without time zone"""

        if host:
            query = query + " AND source_addr = %s"

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        highlights = {}
        try:
            if host:
                curs.execute(query, (one_week, date_end_date, host))
            else:
                curs.execute(query, (one_week, date_end_date))

            highlights = sql_helper.get_result_dictionary(curs)
            
        finally:
            conn.commit()

        return highlights

class TopTenRulesByHits(Graph):
    """
    Top rules detected
    """
    def __init__(self, vendor_name):
        Graph.__init__( 
            self, 
            'top-rules-by-hits', 
            _('Top Detected Rules (by Hits)') 
        )

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(
        self, end_date, report_days, 
        host=None, user=None, email=None
        ):
        if email:
            return None

        date_end_date = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT msg, count(*) as hits_sum
  FROM reports.idps_events
 WHERE time_stamp >= %s::timestamp without time zone 
   AND time_stamp < %s::timestamp without time zone
   AND msg != ''"""

        if host:
            query += " AND source_addr = %s"

        query += " GROUP BY msg ORDER BY hits_sum DESC"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, date_end_date, host))
            elif user:
                curs.execute(query, (one_week, date_end_date, user))
            else:
                curs.execute(query, (one_week, date_end_date))

            for record in curs.fetchall():
                lks.append( 
                    KeyStatistic( record[0], record[1], _('Hits') ) 
                    )
                dataset[record[0]] = record[1]
        finally:
            conn.commit()

        plot = Chart(
            type=PIE_CHART,
            title=self.title,
            xlabel=_('Rules'),
            ylabel=_('Hits Per Day')
            )

        plot.add_pie_dataset(dataset, display_limit=10)

        return(lks, plot, 10)

class TopTenCategoriesByHits(Graph):
    """
    Top categories detected
    """
    def __init__(self, vendor_name):
        Graph.__init__(
            self, 
            'top-categories-by-hits', 
            _('Top Detected Categories (by Hits)')
            )

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(
        self, end_date, report_days, 
        host=None, user=None, email=None 
        ):
        if email:
            return None

        date_end_date = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT category, count(*) as hits_sum
  FROM reports.idps_events
 WHERE time_stamp >= %s::timestamp without time zone 
   AND time_stamp < %s::timestamp without time zone
   AND msg != ''"""

        if host:
            query += " AND source_addr = %s"

        query += " GROUP BY category ORDER BY hits_sum DESC"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, date_end_date, host))
            elif user:
                curs.execute(query, (one_week, date_end_date, user))
            else:
                curs.execute(query, (one_week, date_end_date))

            for record in curs.fetchall():
                lks.append( 
                    KeyStatistic( record[0], record[1], _('Hits') ) 
                    )
                dataset[record[0]] = record[1]
        finally:
            conn.commit()

        plot = Chart(
            type=PIE_CHART,
            title=self.title, 
            xlabel=_('Categories'),
            ylabel=_('Hits Per Day')
            )

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenClasstypesByHits(Graph):
    """
    Top classtypes detected
    """
    def __init__(self, vendor_name):
        Graph.__init__(
            self, 
            'top-classtypes-by-hits', 
            _('Top Detected Classtypes (by Hits)')
        )

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(
        self, end_date, report_days, 
        host=None, user=None, email=None 
        ):
        if email:
            return None

        date_end_date = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT classtype, count(*) as hits_sum
  FROM reports.idps_events
 WHERE time_stamp >= %s::timestamp without time zone 
   AND time_stamp < %s::timestamp without time zone
   AND msg != ''"""

        if host:
            query += " AND source_addr = %s"

        query += " GROUP BY classtype ORDER BY hits_sum DESC"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, date_end_date, host))
            elif user:
                curs.execute(query, (one_week, date_end_date, user))
            else:
                curs.execute(query, (one_week, date_end_date))

            for record in curs.fetchall():
                lks.append(
                    KeyStatistic( record[0], record[1], _('Hits') )
                )
                dataset[record[0]] = record[1]
        finally:
            conn.commit()

        plot = Chart(
            type=PIE_CHART,
            title=self.title, 
            xlabel=_('Classtypes'),
            ylabel=_('Hits Per Day')
            )

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class DailyLogUsage(Graph):
    """
    Daily detected, logged
    """
    def __init__(self, vendor_name):
        Graph.__init__(
            self, 
            'detectedlogged', 
            _('Attacks Detected, Logged')
        )

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(
        self, end_date, report_days, 
        host=None, user=None, email=None 
        ):
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
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"

            extra_where = [("msg != ''", {})]
            if host:
                extra_where.append( ( 
                    "source_addr = %(host)s", 
                    { 'host' : host } 
                ) )

            query, params = sql_helper.get_averaged_query(
                ["COUNT(*)"],
                "reports.idps_events",
                start_date,
                end_date,
                extra_where = extra_where,
                time_interval = time_interval
            )
            curs.execute( query, params )

            dates = []
            attacks = []
            
            for record in curs.fetchall():
                dates.append( record[0] )
                attacks.append( record[1] )

            if not attacks:
                attacks = [0,]

            required_points = sql_helper.get_required_points(
                start_date, 
                end_date,
                mx.DateTime.DateTimeDeltaFromSeconds( time_interval )
            )

            lks.append( 
                KeyStatistic(
                    _('Avg Attacks Detected'),
                    sum(attacks) / len(required_points),
                    _('Logged') + '/' + _(unit)
                ))
            lks.append(
                KeyStatistic(
                    _('Max Attacks Detected'), 
                    max(attacks),
                    _('Logged') + '/' + _(unit)
                ))

            plot = Chart(
                type=STACKED_BAR_CHART,
                title=self.title, 
                xlabel=_(unit),
                ylabel=_('Attacks'),
                major_formatter=TIMESTAMP_FORMATTER,
                required_points=required_points
                )

            plot.add_dataset(dates, attacks, label=_('Attacks Detected'))

        finally:
            conn.commit()

        return (lks, plot)

class DailyBlockUsage(Graph):
    """
    Daily detected, blocked
    """
    def __init__(self, vendor_name):
        Graph.__init__(
            self, 
            'detectedblocked', 
            _('Attacks Detected, Blocked')
            )

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(
        self, end_date, report_days, 
        host=None, user=None, email=None 
        ):
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
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"

            extra_where = [("msg != ''", {})]
            if host:
                extra_where.append(( 
                    "source_addr = %(host)s", 
                    { 'host' : host }
                ))

            query, params = sql_helper.get_averaged_query(
                ["count(case when blocked = true then 1 end)"], 
                "reports.idps_events",
                start_date,
                end_date,
                extra_where = extra_where,
                time_interval = time_interval
            )
            curs.execute( query, params )

            dates = []
            attacks = []
            
            for record in curs.fetchall():
                dates.append(record[0])
                attacks.append(record[1])

            if not attacks:
                attacks = [0,]

            required_points = sql_helper.get_required_points(
                start_date, 
                end_date,
                mx.DateTime.DateTimeDeltaFromSeconds( time_interval )
            )

            lks.append(
                KeyStatistic(
                    _('Avg Attacks Detected'),
                    sum(attacks) / len(required_points),
                    _('Blocked') + '/' + _(unit)
                ))
            lks.append(
                KeyStatistic(
                    _('Max Attacks Detected'), 
                    max(attacks),
                    _('Blocked') + '/' + _(unit)
                ))

            plot = Chart(
                type=STACKED_BAR_CHART,
                title=self.title, xlabel=_(unit),
                ylabel=_('Attacks'),
                major_formatter=TIMESTAMP_FORMATTER,
                required_points=required_points
                )

            plot.add_dataset(dates, attacks, label=_('Attacks Detected'))

        finally:
            conn.commit()

        return (lks, plot)

class IdpsDetail(DetailSection):
    """
    Event log
    """
    def __init__(self):
        DetailSection.__init__(
            self, 
            'idps-events', 
            _('Intrusion Events')
        )

    def get_columns(
        self, host=None, user=None, email=None
        ):
        if email:
            return None

        columns = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if not host:
            columns.append(ColumnDesc('hostname', _('Client'), 'HostLink'))
        if not user:
            columns.append(ColumnDesc('username', _('User'), 'UserLink'))

        columns = columns + [
            ColumnDesc('protocol', _('Protocol')),
            ColumnDesc('source_addr', _('Source IP Address')),
            ColumnDesc('source_port', _('Source Port')),
            ColumnDesc('dest_addr', _('Destination IP Address')),
            ColumnDesc('dest_port', _('Source Port')),
            ColumnDesc('blocked', _('Blocked')),
            ColumnDesc('sig_id', _('Sid')),
            ColumnDesc('classtype', _('Classtype')),
            ColumnDesc('category', _('Category')),
            ColumnDesc('msg', _('Msg'))
            ]

        return columns

    def get_all_columns(
        self, host=None, user=None, email=None):
        return [
            ColumnDesc('time_stamp', _('Time'), 'Date'),
            ColumnDesc('protocol', _('Protocol'), 'Numeric' ),
            ColumnDesc('source_addr', _('Source IP Address') ),
            ColumnDesc('source_port', _('Source Port'), 'Numeric' ),
            ColumnDesc('dest_addr', _('Destination IP Address') ),
            ColumnDesc('dest_port', _('Destination Port'), 'Numeric' ),
            ColumnDesc('blocked', _('Blocked'), 'Boolean' ),
            ColumnDesc('sig_id', _('Sid'), 'Numeric'),
            ColumnDesc('gen_id', _('Gid'), 'Numeric'),
            ColumnDesc('class_id', _('Cid'), 'Numeric'),
            ColumnDesc('classtype', _('Classtype') ),
            ColumnDesc('category', _('Category') ),
            ColumnDesc('msg', _('Msg') )
        ]
 
    def get_sql(
        self, start_date, end_date, 
        host=None, user=None, email=None):
        if email:
            return None

        sql = ("""\
SELECT * 
  FROM reports.idps_events
 WHERE time_stamp >= %s::timestamp without time zone 
   AND time_stamp < %s::timestamp without time zone
   AND NOT msg ISNULL
   AND msg != '' """ % (
        DateFromMx(start_date),
        DateFromMx(end_date)
        ) )

        if host:
            sql = sql + (" AND source_addr = %s" % QuotedString(host))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(Idps('untangle-node-idps', 'IDPS', 'idps'))
