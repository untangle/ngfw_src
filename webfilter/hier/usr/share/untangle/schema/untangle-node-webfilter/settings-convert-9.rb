def update_schema()
  sql_helper = SqlHelper.new(@dbh)

  sql_helper.add_column('settings.n_webfilter_settings',
                        'user_whitelist_mode',
                        'text',
                        :not_null => true) do
    @dbh.do(<<SQL)
UPDATE settings.n_webfilter_settings set user_whitelist_mode = 'USER_ONLY'
SQL
  end

end
