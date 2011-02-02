from untangle.ats.reports_setup import ReportsSetup, PipelineEndpoint, PipelineStats

import datetime

class TestReportsBasic(ReportsSetup):
    @classmethod
    def setup_class( cls ):
        ReportsSetup.setup_class.im_func(cls)
        cls.create_session_event()
        cls.create_session_event(PipelineEndpoint( c_server_port = PipelineEndpoint.DEFAULT_SERVER_PORT + 1 ))
        cls.create_session_event(PipelineEndpoint( c_server_port = PipelineEndpoint.DEFAULT_SERVER_PORT + 2 ))
        cls.create_session_event(PipelineEndpoint( c_client_addr = "10.0.0.1" ))
        cls.create_session_event(PipelineEndpoint( c_client_addr = "10.0.0.2" ))
        cls.create_session_event(PipelineEndpoint( s_client_addr = "10.0.0.2" ))

        timestamp = datetime.datetime.now().replace( hour = 3, minute = 10 ) - datetime.timedelta( days = 1 )
        end_timestamp = timestamp + datetime.timedelta( minutes = 2 )
        cls.create_session_event(PipelineEndpoint( time_stamp = timestamp ),
                                 PipelineStats( time_stamp = end_timestamp ))
        
        timestamp = timestamp.replace( hour = 3, minute = 11 )
        end_timestamp = timestamp + datetime.timedelta( minutes = 2 )
        cls.create_session_event(PipelineEndpoint( time_stamp = timestamp ),
                                 PipelineStats( time_stamp = end_timestamp ))

        timestamp = timestamp.replace( hour = 3, minute = 11, second = ((timestamp.second + 1 ) % 60))
        end_timestamp = timestamp + datetime.timedelta( minutes = 2 )
        cls.create_session_event(PipelineEndpoint( time_stamp = timestamp ),
                                 PipelineStats( time_stamp = end_timestamp ))

        timestamp = timestamp.replace( hour = 3, minute = 14 )
        end_timestamp = timestamp + datetime.timedelta( minutes = 2 )
        cls.create_session_event(PipelineEndpoint( time_stamp = timestamp ),
                                 PipelineStats( time_stamp = end_timestamp ))

        ## This one should not be in todays report
        timestamp = timestamp.replace( hour = 3, minute = 14 ) - datetime.timedelta( days = 2 )
        end_timestamp = timestamp + datetime.timedelta( minutes = 2 )
        cls.create_session_event(PipelineEndpoint( time_stamp = timestamp ),
                                 PipelineStats( time_stamp = end_timestamp ))

        ## This one should be pruned
        timestamp = timestamp - datetime.timedelta( days = 40 )
        end_timestamp = timestamp + datetime.timedelta( minutes = 2 )
        cls.create_session_event(PipelineEndpoint( time_stamp = timestamp ),
                                 PipelineStats( time_stamp = end_timestamp ))

        ## This one should not be in this report (it occurs today.)
        timestamp = datetime.datetime.now()
        end_timestamp = timestamp + datetime.timedelta( minutes = 2 )
        cls.create_session_event(PipelineEndpoint( time_stamp = timestamp ),
                                 PipelineStats( time_stamp = end_timestamp ))

        
        cls.run_reports()

    def test_session_totals_time( self ):
        yield self.check_session_totals, "", 10
        ## Truncate the time
        timestamp = PipelineEndpoint.DEFAULT_TIMESTAMP.replace( second = 0, microsecond = 0 )
        yield self.check_session_totals, "trunc_time=timestamp '%s'" % timestamp, 6
        timestamp = datetime.datetime.now().replace( hour = 3, minute = 10, second = 0, microsecond = 0 ) - datetime.timedelta( days = 1 )
        yield self.check_session_totals, "trunc_time=timestamp '%s'" % timestamp, 1
        timestamp = timestamp.replace( hour = 3, minute = 11 )
        yield self.check_session_totals, "trunc_time=timestamp '%s'" % timestamp, 2
        timestamp = timestamp.replace( hour = 3, minute = 14 )
        yield self.check_session_totals, "trunc_time=timestamp '%s'" % timestamp, 1

    def test_session_totals_address( self ):
        yield self.check_session_totals, "hname='%s'" % PipelineEndpoint.DEFAULT_CLIENT_ADDR, 8
        yield self.check_session_totals, "hname='%s'" % "10.0.0.1", 1
        yield self.check_session_totals, "hname='%s'" % "10.0.0.2", 1

    def test_session_totals_port( self ):
        yield self.check_session_totals, "c_server_port=%d" % PipelineEndpoint.DEFAULT_SERVER_PORT, 8
        yield self.check_session_totals, "c_server_port=%d" % (PipelineEndpoint.DEFAULT_SERVER_PORT + 1), 1
        yield self.check_session_totals, "c_server_port=%d" % (PipelineEndpoint.DEFAULT_SERVER_PORT + 2), 1
        
    def check_session_totals( self, params, count ):
        curs = self.get_conn().cursor()
        if ( params != None ) and ( len( params ) > 0):
            params = "WHERE %s" % params
        else:
            params = ""

        query = "SELECT SUM(new_sessions) FROM reports.session_totals %s" % params
        print "execute: %s" % query
            
        curs.execute( query )

        num_sessions = curs.fetchone()[0]
        if ( num_sessions == None ):
            num_sessions = 0
            
        num_sessions = int(num_sessions)
        assert num_sessions == count
