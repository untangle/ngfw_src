
def rollup()

end

def setup()
  @start_times.each_pair do |period, start_time|
    @sql_helper.create_table_from_query("reports.#{period.to_s}_users", <<SQL, start_time, @end_time)
SELECT DISTINCT username FROM events.u_lookup_evt
WHERE time_stamp >= ? AND time_stamp < ?
SQL

    @sql_helper.create_table_from_query("reports.#{period.to_s}_hnames", <<SQL, start_time, @end_time)
SELECT DISTINCT hname FROM reports.sessions
WHERE time_stamp >= ? AND time_stamp < ? AND client_intf=1
SQL
  end
end

def teardown()

end
