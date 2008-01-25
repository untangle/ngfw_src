def update_schema()
  [
   'ALTER TABLE settings.n_firewall_rule ADD COLUMN src_intf_matcher text',
   'ALTER TABLE settings.n_firewall_rule ADD COLUMN dst_intf_matcher text',
   "UPDATE settings.n_firewall_rule SET live = false, src_intf_matcher = 'any', dst_intf_matcher = 'any' WHERE inbound = false AND outbound = false",
   "UPDATE settings.n_firewall_rule SET src_intf_matcher = 'any', dst_intf_matcher = 'less_trusted' WHERE inbound = false AND outbound = true",
   "UPDATE settings.n_firewall_rule SET src_intf_matcher = 'more_trusted', dst_intf_matcher = 'any' WHERE inbound = true AND outbound = false",
   "UPDATE settings.n_firewall_rule SET src_intf_matcher = 'any', dst_intf_matcher = 'any' WHERE inbound = true AND outbound = true",
   'ALTER TABLE settings.n_firewall_rule DROP COLUMN inbound',
   'ALTER TABLE settings.n_firewall_rule DROP COLUMN outbound'
  ].each do |sql|
    begin
      @dbh.do(sql)
    rescue DBI::DatabaseError => e
      SqlHelper.log_sql_error("could not run: #{sql}", e)
    end
  end
end
