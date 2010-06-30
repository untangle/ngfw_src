def update_schema()
  sql_helper = SqlHelper.new(@dbh)

  user_policy_rule_sets = @dbh.select_all(<<SQL)
SELECT set_id, count(*)
FROM u_user_policy_rules
JOIN u_user_policy_rule USING (set_id)
GROUP BY set_id ORDER BY count DESC
SQL

  @dbh.prepare('DELETE FROM settings.u_user_policy_rule WHERE set_id = ?') do |upr_ps|
    @dbh.prepare('DELETE FROM settings.u_user_policy_rules WHERE set_id = ?') do |uprs_ps|
      (user_policy_rule_sets[1..-1] || []).each do |s|
        id = s['set_id']
        upr_ps.execute(id)
        uprs_ps.execute(id)
      end
    end
  end

  sql_helper.remove_columns('settings.u_user_policy_rule', 'is_inbound')

  set_id = get_set_id()

  if set_id.nil?
    set_id = sql_helper.next_id()

    @dbh.prepare('INSERT INTO settings.u_user_policy_rules VALUES (?)') do |ps|
      ps.execute(set_id);
    end
  end

  user_policy_rules = @dbh.select_all(<<SQL)
SELECT * FROM settings.u_user_policy_rule ORDER BY position ASC
SQL

  user_policy_rules << {
    'rule_id' => sql_helper.next_id(),
    'protocol_matcher' => 'TCP',
    'client_ip_matcher' => 'any',
    'server_ip_matcher' => 'any',
    'client_port_matcher' => 'any',
    'server_port_matcher' => '25',
    'client_intf_matcher' => 'any',
    'server_intf_matcher' => 'O',
    'policy_id' => nil,
    'name' => '[no name]',
    'category' => '[no category]',
    'description' => 'STMP outbound bypass',
    'live' => true,
    'alert' => false,
    'log' => false,
    'set_id' => set_id,
    'position' => nil,
    'start_time' => '00:00:00',
    'end_time' => '23:59:00',
    'day_of_week_matcher' => 'any',
    'user_matcher' => '[any]',
    'invert_entire_duration' => false
  }

  sprs = @dbh.select_all(<<SQL)
SELECT * FROM settings.u_system_policy_rule spr
JOIN settings.u_policy p ON (spr.policy_id = p.id)
WHERE NOT p.is_default
SQL

  sprs.each do |spr|
    user_policy_rules << {
      'rule_id' => spr['rule_id'],
      'protocol_matcher' => 'ANY',
      'client_ip_matcher' => 'any',
      'server_ip_matcher' => 'any',
      'client_port_matcher' => 'any',
      'server_port_matcher' => 'any',
      'client_intf_matcher' => convert_intf(spr['client_intf']),
      'server_intf_matcher' => convert_intf(spr['server_intf']),
      'policy_id' => spr['policy_id'],
      'name' => spr['name'],
      'category' => spr['category'],
      'description' => spr['description'],
      'live' => spr['live'],
      'alert' => spr['alert'],
      'log' => spr['log'],
      'set_id' => set_id,
      'position' => nil,
      'start_time' => '00:00:00',
      'end_time' => '23:59:00',
      'day_of_week_matcher' => 'any',
      'user_matcher' => '[any]',
      'invert_entire_duration' => false
    }
  end

  user_policy_rules.each_with_index {|e, i| e['position'] = i }
  sql_helper.synchronize_table('settings.u_user_policy_rule', 'rule_id',
                               user_policy_rules)

  sql_helper.drop_tables('settings.u_system_policy_rule')
end

def convert_intf(intf)
  case intf.to_i
  when 0
    'O'
  when 1
    'I'
  when 2
    'D'
  when 3
    'V'
  end
end

def get_set_id()
  user_policy_rule_sets = @dbh.select_one(<<SQL)
SELECT set_id FROM settings.u_user_policy_rules
SQL
  (user_policy_rule_sets || []).first
end
