def update_schema()
  sql_helper = SqlHelper.new(@dbh)

  sql_helper.rename_column('settings.n_mail_settings', 'smtp_inbound_timeout',
                           'smtp_timeout')
  sql_helper.rename_column('settings.n_mail_settings', 'imap_inbound_timeout',
                           'imap_timeout')
  sql_helper.rename_column('settings.n_mail_settings', 'pop_inbound_timeout',
                           'pop_timeout')
  sql_helper.remove_columns('settings.n_mail_settings',
                            ['smtp_outbound_timeout', 'imap_outbound_timeout',
                             'pop_outbound_timeout'])

  sql_helper.add_column('settings.n_mail_quarantine_settings',
                        'quarantine_external_mail',
                        'bool',
                        :not_null => true) do
    @dbh.do(<<SQL)
UPDATE settings.n_mail_quarantine_settings set quarantine_external_mail = true
SQL

  @dbh.do(<<SQL)
UPDATE settings.n_mail_settings set smtp_timeout = 240000
SQL

  end
end
