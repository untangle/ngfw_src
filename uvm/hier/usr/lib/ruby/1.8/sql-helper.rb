require 'set'

class SqlHelper
  def SqlHelper.log_sql_error(msg, e)
    LOG.warn <<MSG
#{msg}
Error code: #{e.err}
Error message: #{e.errstr}
Error SQLSTATE: #{e.state}
MSG
  end

  def initialize(dbh)
    @dbh = dbh
  end

  def next_id()
    @dbh.select_one(<<SQL).first
SELECT nextval('hibernate_sequence')
SQL
  end

  def add_column(table, column, type, options)
    log_sql(<<SQL)
ALTER TABLE #{table} ADD COLUMN #{column} #{type}
SQL
    yield

    if options[:not_null]
      log_sql(<<SQL)
ALTER TABLE #{table} ALTER COLUMN #{column} SET NOT NULL
SQL
    end
  end

  def remove_columns(table, columns, cascade = true)
    [columns].flatten.each do |c|
      begin
      log_sql(<<SQL)
ALTER TABLE #{table} DROP COLUMN #{c}#{' CASCADE' if cascade}
SQL
      rescue DBI::DatabaseError => e
        SqlHelper.log_sql_error("Could not remove column #{c}", e)
      end
    end
  end

  def rename_column(table, old_name, new_name)
    log_sql(<<SQL)
ALTER TABLE #{table} RENAME COLUMN #{old_name} TO #{new_name}
SQL
  end

  def drop_tables(tables, cascade = true)
    begin
      log_sql(<<SQL)
DROP TABLE #{[tables].flatten.join(', ')}#{' CASCADE' if cascade}
SQL
    rescue DBI::DatabaseError => e
      SqlHelper.log_sql_error("Could not remove tables #{[tables].flatten.join(', ')}", e)
    end
  end

  def has_trigger?(schema, table, trigger_name)
    @dbh.prepare(<<SQL) do |ps|
SELECT 1 FROM information_schema.triggers
WHERE trigger_schema = ? AND event_object_table = ? AND trigger_name = ?
SQL
      ps.execute(schema, table, trigger_name)
      l = ps.fetch()
      !l.nil?
    end
  end

  def get_col_type(schema, table, column)
    @dbh.prepare(<<SQL) do |ps|
SELECT data_type
FROM information_schema.columns
WHERE table_schema = ? AND table_name = ? AND column_name = ?
SQL
      ps.execute(schema, table, column)
      l = ps.fetch()
      l.nil? ? nil : l[0]
    end
  end

  def synchronize_table(table_name, primary_key, list)
    keys = get_keys(primary_key, list)
    bad_keys = [ ]
    update_row_keys = Set.new

    @dbh.execute("SELECT #{primary_key.to_s} FROM #{table_name}") do |r|
      r.fetch do |e|
        k = e.first
        if keys.member?(k)
          update_row_keys << k
        else
          bad_keys << k
        end
      end
    end

    new_row_keys = keys - update_row_keys.to_a
    new_rows = list.select { |e| new_row_keys.member?(e[primary_key]) }
    insert_rows(table_name, new_rows)

    update_rows = list.select { |e| update_row_keys.member?(e[primary_key]) }
    update_rows(table_name, primary_key, update_rows)

    @dbh.prepare("DELETE FROM #{table_name} WHERE #{primary_key} = ?") do |ps|
      bad_keys.each do |e|
        ps.execute(e)
      end
    end
  end

  def create_table_from_query(table, sql, *params)
    begin
      log_sql("DROP TABLE #{table}")
    rescue
      # fine if no table
    end

    begin
      @dbh.prepare("CREATE TABLE #{table} AS #{sql}") do |ps|
        ps.execute(*params)
      end
    rescue DBI::DatabaseError => e
      SqlHelper.log_sql_error("Could not create table from #{sql} (#{params.join(', ')})", e)
    end
  end

  def get_short_table_name(table)
    /\.(.*)$/ =~ table ? $1 : table
  end

  def get_schema(table)
    /^([^.]+)\./ =~ table ? $1 : 'public'
  end

  def table_exist?(table)
    @dbh.tables.member?(get_short_table_name(table))
  end

  def do_update(name, end_date, interval=30)
    @dbh['AutoCommit'] = false
    @dbh.transaction do |dbh|
      last_update = @dbh.prepare(<<SQL) do |ps|
SELECT last_update FROM reports.updates WHERE name = ?
SQL
        ps.execute(name)
        l = ps.fetch()
        l.nil? ? nil : l[0]
      end

      if (last_update.nil?)
        yield(true, end_date - interval, end_date)
        @dbh.prepare(<<SQL) do |ps|
INSERT INTO reports.updates VALUES (?, ?)
SQL
          ps.execute(name, end_date)
        end
      else
        sd = if (end_date - interval > last_update.to_date)
               end_date - interval
             else
               last_update
             end
        yield(false, sd, end_date)
        @dbh.prepare(<<SQL) do |ps|
UPDATE reports.updates SET last_update = ? WHERE name = ?
SQL
          ps.execute(end_date, name)
        end
      end
    end
    @dbh['AutoCommit'] = true
  end

  def create_partitioned_table(table_ddl, timestamp_column,
                               end_date, interval=30)
    t0 = Time.now
    if interval <= 0
      raise "Bad interval: #{interval}"
    end

    start_date = end_date - interval;

    if /create\s+table\s+(\S+)/im =~ table_ddl
      table_name = $1
      short_table_name = get_short_table_name(table_name)
      schema = get_schema(table_name)
    else
      raise "Cannot find table in: #{table_ddl}"
    end

    dates = Set.new
    start_date.upto(end_date - 1) {|d| dates << d}

    @dbh['AutoCommit'] = false
    @dbh.transaction do |dbh|

      existing_dates = Set.new

      @dbh.do(table_ddl) unless table_exist?(table_name)

      @dbh.tables.each do |t|
        if /^#{short_table_name}_(.*)$/ =~ t
          suffix = $1
          date = Date.parse(suffix.gsub('_', '-'))
          if dates.member?(date)
            existing_dates << date
          else
            @dbh.do("DROP TABLE #{table_name}_#{suffix}")
          end
        end
      end

      query_start_date = nil

      (dates - existing_dates).each do |d|
        @dbh.prepare(<<SQL) do |ps|
CREATE TABLE #{table_name}_#{d.to_s.gsub('-', '_')}
(CHECK (#{timestamp_column} >= ? AND #{timestamp_column} < ?))
INHERITS (#{table_name})
SQL
          ps.execute(d, d.succ)
        end

        if query_start_date.nil? or query_start_date > d
          query_start_date = d
        end
      end

      trigger_function = <<SQL
CREATE OR REPLACE FUNCTION #{short_table_name}_insert_trigger()
RETURNS TRIGGER AS $$
BEGIN
SQL

      first = true
      dates.each do |d|
        trigger_function += <<SQL
    #{first ? 'IF' : 'ELSIF'} (NEW.#{timestamp_column} >= '#{d}'
          AND NEW.#{timestamp_column} < '#{d.succ}') THEN
        INSERT INTO #{table_name}_#{d.to_s.gsub('-', '_')} VALUES (NEW.*);
SQL
        first = false
      end

      trigger_function += <<SQL
    ELSE
        RAISE NOTICE 'Date out of range: %', NEW.#{timestamp_column};
    END IF;
    RETURN NULL;
END;
$$
LANGUAGE plpgsql;
SQL

      @dbh.do(trigger_function)

      unless has_trigger?(schema, short_table_name,
                          "insert_#{short_table_name}_trigger")
        @dbh.do <<SQL
CREATE TRIGGER insert_#{short_table_name}_trigger
    BEFORE INSERT ON #{table_name}
    FOR EACH ROW EXECUTE PROCEDURE #{short_table_name}_insert_trigger()
SQL
      end

      unless query_start_date.nil?
        query_start_date.upto(end_date - 1) do |d|
          @dbh.do("TRUNCATE TABLE #{table_name}_#{d.to_s.gsub('-', '_')}")
        end

        yield(query_start_date, end_date)
      end

    end
    @dbh['AutoCommit'] = true
    t1 = Time.now
    LOG.info "#{table_name} took: #{t1 - t0} seconds"
  end

  def log_sql(sql)
    t0 = Time.now
    @dbh.do(sql)
    t1 = Time.now

    LOG.debug "#{sql} took: #{t1 - t0} seconds"
  end

  private

  def get_keys(primary_key, list)
     list.inject(Set.new) { |m, o| m << o[primary_key] }.to_a
  end

  def get_all_columns(rows)
    rows.inject(Set.new) do |m, o|
      m += o.instance_of?(Hash) ? o.keys : o.field_names
    end.to_a
  end

  def bind_values(columns, row)
    columns.map { |c| row[c] }
  end

  def get_insert_statement(table, columns)
    <<SQL
INSERT INTO #{table} (#{columns.join(', ')})
VALUES (#{columns.map {|e| '?'}.join(', ') })
SQL
  end

  def get_update_statement(table, primary_key, columns)
    <<SQL
UPDATE #{table}
SET #{columns.map { |c| "#{c} = ?"}.join(', ')}
WHERE #{primary_key} = ?
SQL
  end

  def insert_rows(table, rows)
    columns = get_all_columns(rows)
    @dbh.prepare(get_insert_statement(table, columns)) do |ps|
      rows.each do |r|
        ps.execute(*bind_values(columns, r))
      end
    end
  end

  def update_rows(table, primary_key, rows)
    columns = get_all_columns(rows).reject { |c| primary_key == c }
    @dbh.prepare(get_update_statement(table, primary_key, columns)) do |ps|
      rows.each do |r|
        ps.execute(*bind_values(columns + [primary_key], r))
      end
    end
  end
end

