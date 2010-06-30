module ReportHelper
  def ReportHelper.top_ten(dbh, title, column_names, fact_table,
              column_expressions, start_time, end_time, limit=15)
    r = {}

    r['title'] = title

    r['columns'] = column_names + ['%']

    r['data'] = []

    dbh.prepare(<<SQL) do |ps|
SELECT #{column_expressions[0]}, #{column_expressions[1]} AS c
FROM #{fact_table}
WHERE trunc_time >= ? AND trunc_time < ?
GROUP BY #{column_expressions[0]}
ORDER BY c DESC LIMIT #{limit};
SQL
      ps.execute(start_time, end_time)
      ps.fetch do |row|
        r['data'] << row[0..1]
      end
    end

    dbh.prepare(<<SQL) do |ps|
SELECT #{column_expressions[0]}
FROM #{fact_table}
WHERE trunc_time >= ? AND trunc_time < ?
SQL
      ps.execute(start_time, end_time)
      ps.fetch do |row|
        r['total'] = row[0]
      end
    end

    r
  end
end
