def update_schema()
  sql_helper = SqlHelper.new(@dbh)

  sql_helper.add_column('settings.n_mail_settings',
                        'smtp_allow_tls',
                        'bool',
                        :not_null => true) do
    @dbh.do(<<SQL)
UPDATE settings.n_mail_settings set smtp_allow_tls = false
SQL

  end
end
