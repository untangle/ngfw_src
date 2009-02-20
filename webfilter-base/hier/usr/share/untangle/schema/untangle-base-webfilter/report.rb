def setup_detail()
  sql_helper().do_update('n_webfilter_http_events', @end_time) do |new, st, et|
    if new
      ["ALTER TABLE reports.n_http_events ADD COLUMN webfilter_action character(1)",
       "ALTER TABLE reports.n_http_events ADD COLUMN webfilter_reason character(1)",
       "ALTER TABLE reports.n_http_events ADD COLUMN webfilter_category text"].each do |sql|
        begin
          dbh().do(sql)
        rescue DBI::DatabaseError
          # already exists
        end
      end
    end

    dbh().prepare(<<SQL) do |ps|
UPDATE reports.n_http_events
SET webfilter_action = action,
    webfilter_reason = reason,
    webfilter_category = category
FROM events.n_webfilter_evt_blk
WHERE reports.n_http_events.time_stamp >= ?
      AND reports.n_http_events.time_stamp < ?
      AND reports.n_http_events.request_id = events.n_webfilter_evt_blk.request_id
SQL
      ps.execute(st, et)
    end
  end

  ft = @report_engine.get_fact_table('reports.n_http_totals')
  ft.measures << Column.new('blocks', 'integer',
                            "count(CASE WHEN webfilter_action = 'P' THEN 1 ELSE NULL END)")
end

def setup()
end

def teardown()
end

def http_domains(p)
  ReportHelper.top_ten(dbh(), 'Http Domains', ['Domain', 'Hits'],
                       'reports.n_http_totals',
                       ['host', 'sum(hits)'],
                       p[:start_time], p[:end_time])
end

def http_user_hits(p)
  ReportHelper.top_ten(dbh(), 'Http User Domains', ['User', 'Hits'],
                       'reports.n_http_totals',
                       ['hname', 'sum(hits)'],
                       p[:start_time], p[:end_time])
end

def http_violators(p)
  ReportHelper.top_ten(dbh(), 'Http Violators', ['User', 'Hits'],
                       'reports.n_http_totals',
                       ['host', 'sum(blocks)'],
                       p[:start_time], p[:end_time])
end

def http_user_sizes(p)
  ReportHelper.top_ten(dbh(), 'Http User Sizes', ['User', 'Size (MB)'],
                       'reports.n_http_totals',
                       ['host', 'sum(s2c_content_length)'],
                       p[:start_time], p[:end_time])
end
