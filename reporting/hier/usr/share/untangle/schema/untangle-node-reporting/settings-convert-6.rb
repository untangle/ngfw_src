def update_schema()
  sql_helper = SqlHelper.new(@dbh)

  sql_helper.add_column('settings.n_reporting_settings',
                        'days_to_keep',
                        'int4',
                        :not_null => true) do
    @dbh.do(<<SQL)
UPDATE settings.n_reporting_settings SET days_to_keep = 33
SQL
  end

end
