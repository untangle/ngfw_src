import gettext
import logging
import mx
import re
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
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

_ = uvm.i18n_helper.get_translation('untangle-base-wan-failover').lgettext

def key_statistic_sort_by_value(a, b):
    return cmp(a.value, b.value)

class WanFailover(reports.engine.Node):
    def __init__(self):
        reports.engine.Node.__init__(self, 'untangle-node-faild','WAN Failover')

    @sql_helper.print_timing
    def setup(self):
        return

    def create_tables(self):
        self.__create_wan_failover_test_events()
        self.__create_wan_failover_action_events()

    def post_facttable_setup(self, start_date, end_date):
        self.__find_disconnects(end_date)
        self.__num_disconnects()

    def parents(self):
        return ['untangle-vm']

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [WanHighlight(self.name),
                            WanAvailability(self),
                            LessReliableWanInterfaces(self),
                            FailuresByWanInterface(self)])

        sections.append(s)

        return Report(self, sections)

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table('faild_disconnects', cutoff)
        sql_helper.clean_table('faild_num_disconnects', cutoff)
        sql_helper.clean_table('wan_failover_test_events', cutoff)
        sql_helper.clean_table('wan_failover_action_events', cutoff)

    @property
    def num_wan_interfaces(self):
        return reports.engine.get_number_wan_interfaces()

    @property
    def wan_interfaces(self):
        a = []

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

    @sql_helper.print_timing
    def __create_wan_failover_action_events( self ):
        # rename old table if exists
        sql_helper.rename_table("faild_action_events","wan_failover_action_events") #11.2

        sql_helper.create_table("""\
CREATE TABLE reports.wan_failover_action_events (
    time_stamp timestamp without time zone,
    interface_id int,
    action text,
    os_name text,
    name text,
    event_id bigserial)""", ["event_id"], ["time_stamp"])


    @sql_helper.print_timing
    def __create_wan_failover_test_events( self ):
        # rename old table if exists
        sql_helper.rename_table("faild_test_events","wan_failover_test_events") #11.2

        sql_helper.create_table("""\
CREATE TABLE reports.wan_failover_test_events (
    time_stamp timestamp without time zone,
    interface_id int,
    name text,
    description text,
    success bool,
    event_id bigserial)""", ["event_id"], ["time_stamp"])

    def __find_disconnects(self, end_date):
        sql_helper.create_table("""\
CREATE TABLE reports.faild_disconnects (
        interface_id int,
        time_stamp timestamp,
        start_time timestamp,
        end_time timestamp,
        event_id bigint)""", ["event_id"], ["time_stamp"])

        q = """\
SELECT time_stamp, interface_id, action FROM reports.wan_failover_action_events
ORDER BY time_stamp ASC
"""
        d = {}
        ed = DateFromMx(end_date)

        disconnects = []

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()
            curs.execute(q, ())

            for r in curs.fetchall():
                time_stamp = r[0]
                interface_id = r[1]
                action = r[2]

                last_disconnect = d.get(interface_id, None)

                if action == 'DISCONNECTED' and not last_disconnect:
                    d[interface_id] = time_stamp
                elif action == 'CONNECTED' and last_disconnect:
                    disconnects.append((interface_id, time_stamp, last_disconnect, time_stamp))
                    d[interface_id] = None

            for interface_id, last_disconnect in d.iteritems():
                if last_disconnect:
                    disconnects.append((interface_id, time_stamp, last_disconnect, ed))

            #print "DISCONNECTS:"
            #for disconnect in disconnects:
            #    print disconnect[0],  ": ", disconnect[2], " -> ", disconnect[3]

            curs = conn.cursor()
            # we will rebuild the whole table, so flush all the data
            sql_helper.run_sql("""\
TRUNCATE TABLE reports.faild_disconnects;
""");
            curs.executemany("""\
INSERT INTO reports.faild_disconnects (interface_id, time_stamp, start_time, end_time)
VALUES (%s, %s, %s, %s)""", disconnects)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def __num_disconnects(self):
        sql_helper.create_table("""\
CREATE TABLE reports.faild_num_disconnects (
        time_stamp timestamp,
        ifaces_down int)""",[],["time_stamp"])

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.faild_num_disconnects (time_stamp, ifaces_down)
SELECT date_trunc('second', start_time)
       + (generate_series(0, EXTRACT('seconds' FROM end_time - start_time)::bigint)
          || ' seconds')::interval AS time,
       count(interface_id)
FROM reports.faild_disconnects
GROUP BY time
ORDER BY time""", ())
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class WanHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("detected") + " " +
                           "%(failures)s" + " " + _("WAN failures and saved the network from") +
                           " " + "%(downtime)s" + " " + _("seconds of downtime"))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT count(*) AS failures,
       round(COALESCE(EXTRACT('epoch' FROM sum(end_time - start_time)::interval),0)::numeric,1) AS downtime
FROM reports.faild_disconnects
WHERE start_time >= %s AND start_time < %s"""

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h

class WanAvailability(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'wan-availability',
                       _('WAN Availability'))
        self.__node = node

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        sd = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        
        lks = []

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            ks_query = """\
SELECT count(CASE WHEN ifaces_down = %s THEN 1 ELSE NULL END) AS down,
       count(CASE WHEN ifaces_down > 0 AND  ifaces_down < %s
                  THEN 1 ELSE NULL END) AS partial
FROM reports.faild_num_disconnects
WHERE time_stamp >= %s::timestamp without time zone and time_stamp < %s::timestamp without time zone
"""
            curs = conn.cursor()

            ni = self.__node.num_wan_interfaces
            curs.execute(ks_query, (ni, ni, sd, ed))

            r = curs.fetchone()

            dataset = {}

            one_day = float(report_days * 24 * 60 * 60)
            down = (r[0] / one_day) * 100
            partial = (r[1] / one_day) * 100
            full = 100 - (down + partial)

            ks = KeyStatistic(_('Full Connectivity'), full, '%')
            lks.append(ks)
            dataset[_('Full Connectivity')] = full
            ks = KeyStatistic(_('Partial Connectivity'), partial, '%')
            lks.append(ks)
            dataset[_('Partial Connectivity')] = partial
            ks = KeyStatistic(_('No Connectivity'), down, '%')
            lks.append(ks)
            dataset[_('No Connectivity')] = down
            
            plot = Chart(type=PIE_CHART,
                         title=self.title,
                         xlabel=_('Connectivity'),
                         ylabel=_('%'))

            plot.add_pie_dataset(dataset)
            
        finally:
            conn.commit()

        return (lks, plot)

class LessReliableWanInterfaces(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'less-reliable-wan-interfaces',
                       _('WAN Interfaces With Downtime'))
        self.__node = node
        self.__wan_interfaces = set(node.wan_interfaces)
        
    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        iface_names = self.__node.interface_names

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []
        plot_data = {}
        
        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["EXTRACT('epoch' FROM SUM(end_time - start_time)::interval)",]
            extra_fields = ["interface_id",]
            
            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER
                
            q, h = sql_helper.get_averaged_query(sums, "reports.faild_disconnects",
                                                 start_date,
                                                 end_date,
                                                 extra_fields = extra_fields,
                                                 time_interval = time_interval,
                                                 time_field = 'start_time')
            curs.execute(q, h)

            dates = []
            for r in curs.fetchall():
                dates.append(r[0])
                interface_id = r[1]
                
                times, values = plot_data.get(interface_id, ([], []))
                secs = dates[-1]
                if len(times) > 0 and times[-1] == secs:
                    values[-1] += r[2]
                else:
                    times.append(secs)
                    values.append(r[2])
                plot_data[interface_id] = (times, values)                            

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            for intf in self.__wan_interfaces:
                if not intf in plot_data:
                    plot_data[intf] = (rp, [0,]*len(rp))

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Seconds'),
                         major_formatter=formatter,
                         required_points=rp)

            for k, v in plot_data.iteritems():
                time, downtime = v
                name =  iface_names.get(k, str(k))
                plot.add_dataset(time, downtime, name)

                ks = KeyStatistic(_('Downtime For') + ' ' + name, sum(downtime), 'Seconds')

                lks.append(ks)

        finally:
            conn.commit()
             
        return (lks, plot)

class FailuresByWanInterface(Graph):
    def __init__(self, node):
        Graph.__init__(self, 'wan-interfaces-failures',
                       _('WAN Interface Failures'))
        self.__node = node
        self.__wan_interfaces = set(node.wan_interfaces)
        
    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        iface_names = self.__node.interface_names

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []
        plot_data = {}
        
        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["COUNT(*)",]
            extra_fields = ["interface_id",]
            
            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER
                
            q, h = sql_helper.get_averaged_query(sums, "reports.faild_disconnects",
                                                 start_date,
                                                 end_date,
                                                 extra_fields = extra_fields,
                                                 time_interval = time_interval,
                                                 time_field = 'start_time')
            curs.execute(q, h)

            dates = []
            for r in curs.fetchall():
                dates.append(r[0])
                interface_id = r[1]
                
                times, values = plot_data.get(interface_id, ([], []))
                secs = dates[-1]
                if len(times) > 0 and times[-1] == secs:
                    values[-1] += r[2]
                else:
                    times.append(secs)
                    values.append(r[2])
                plot_data[interface_id] = (times, values)                            

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            for intf in self.__wan_interfaces:
                if not intf in plot_data:
                    plot_data[intf] = (rp, [0,]*len(rp))

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Failures'),
                         major_formatter=formatter,
                         required_points=rp)

            for k, v in plot_data.iteritems():
                time, failures = v
                name =  iface_names.get(k, str(k))
                plot.add_dataset(time, failures, name)

                ks = KeyStatistic(_('Failure For') + ' ' + name, sum(failures), 'Failures')

                lks.append(ks)

        finally:
            conn.commit()
             
        return (lks, plot)

reports.engine.register_node(WanFailover())
