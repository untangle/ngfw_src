require 'logger'
require 'dbi'
require 'date'
require 'rexml/document'
require 'json'
require 'fileutils'

require '@PREFIX@/usr/lib/ruby/1.8/sql-helper'
require '@PREFIX@/usr/lib/ruby/1.8/reports/report-helper'

LOG = Logger.new(STDOUT)

REPORT_DIR = "@PREFIX@/var/lib/untangle/reports/data"

class ReportEngine
  attr_reader :fact_tables, :dbh, :sql_helper

  def initialize(end_time, periods)
    @end_time = Date.new(end_time.year, end_time.month, end_time.day)
    @nodes = {}
    @node_reporters = {}
    @fact_tables = {}

    @dbh = DBI.connect('DBI:Pg:uvm', 'postgres')
    @sql_helper = SqlHelper.new(@dbh)

    Dir.entries('@UVM_TOOLBOX@').each do |e|
      fn = "@UVM_TOOLBOX@/#{e}/META-INF/uvm-node.xml"
      if File.exist?(fn)
        name = e.split('-')[0..2].join('-')

        doc = REXML::Document.new(File.new(fn))
        parents = []
        doc.elements.each('uvm-node/parent') do |p|
          parents << p.text
        end

        @nodes[name] = parents
        @node_reporters[name] = NodeReportRunner.new(self, name, @end_time,
                                                     periods)
      end
    end
  end

  def setup()
    each_node do |n|
      t0 = Time.now
      n.setup_detail()
      t1 = Time.now

      LOG.info "#{n.node}.setup_detail() took: #{t1 - t0} seconds"
    end

    process_fact_tables()

    each_node do |n|
      t0 = Time.now
      n.setup()
      t1 = Time.now

      LOG.info "#{n.node}.setup() took: #{t1 - t0} seconds"
    end
  end

  def process_reports()
    @loaded.each do |n|
      LOG.info "doing process_reports for node: #{n}"
      @node_reporters[n].process_reports()
    end
  end

  def teardown()
    @loaded.reverse.each do |n|
      LOG.info "doing teardown for node: #{n}"
      @node_reporters[n].teardown()
      @loaded.delete(n)
    end
  end

  def register_fact_table(ft)
    @fact_tables[ft.name] = ft
  end

  def get_fact_table(name)
    @fact_tables[name]
  end

  private

  def each_node()
    @loaded = []
    unloaded = @nodes.keys

    l = loadable(unloaded, @loaded)

    until (l.empty?)
      l.each do |n|
        LOG.info "doing setup for node: #{n}"
        yield @node_reporters[n]
        @loaded << n
        unloaded.delete(n)
      end
      l = loadable(unloaded, @loaded)
    end
  end

  def loadable(unloaded, loaded)
    puts "UNLOADED #{unloaded} LOADED: #{loaded}"

    unloaded.select { |e| @nodes[e].all? { |f| loaded.member?(f) } }
  end

  def process_fact_tables()
    t0 = Time.now
    @fact_tables.values.each do |ft|
      @sql_helper.create_partitioned_table(ft.ddl, 'trunc_time', @end_time) do |st, et|
        ft.update_table(@dbh, st, et)
      end
    end
    t1 = Time.now
    LOG.info "fact tables took: #{t1 - t0} seconds"
  end
end

class NodeReportRunner
  attr_reader :node

  def initialize(report_engine, node, end_time, periods)
    @report_engine = report_engine
    @node = node
    @end_time = end_time
    @parameters = []
    @reports = []
    @master_reports = []

    f = "@UVM_SCHEMA@/#{node}/report.xml"
    process_xml(f) if File.exist?(f)

    @start_times = periods.inject({}) do |m, o|
      case o
      when :daily
        m[o] = @end_time - 1
      when :weekly
        m[o] = @end_time - 7
      when :monthly
        m[o] = Date.new(@end_time.year, ((@end_time.month - 2) % 12) + 1,
                        @end_time.day)
      else
        LOG.warn("unknown period: #{m}")
      end

      m
    end

    file = "@UVM_SCHEMA@/#{@node}/report.rb"
    if File.exist?(file)
      begin
        self.instance_eval(File.read(file), file)
      rescue
        LOG.warn "could not eval #{file}: #{$!}"
        LOG.warn $!.backtrace
      end
    end
  end

  def dbh()
    @report_engine.dbh()
  end

  def sql_helper()
    @report_engine.sql_helper()
  end

  def process_reports()
    p = {}
    p[:end_time] = @end_time

    d = "#{REPORT_DIR}/%d-%02d-%02d" % [@end_time.year, @end_time.month,
                                        @end_time.day]

    @start_times.each_pair do |period, start_time|
      p[:start_time] = start_time

      outdir = "#{d}/#{period}/#{@node}"

      @master_reports.each do |mr|
        process_master_report(mr, p, outdir)
      end

      @reports.each do |r|
        process_report(r, p, outdir)
      end
    end
  end

  def setup()
  end

  def setup_detail()
  end

  def teardown()
  end

  private

  def process_master_report(mr, p, outdir)
    mr[:reports].each do |r|
      process_report(r, p, "#{outdir}/#{mr[:name]}")
    end
  end

  def process_report(r, p, outdir)
    name = r[:name]
    if self.methods.member?(name)
      begin
        json = JSON.pretty_unparse(send(name, p))

        FileUtils.mkdir_p(outdir) unless File.exist?(outdir)

        File.open("#{outdir}/#{r[:name]}", 'w') do |io|
          io.puts json
        end
      rescue
        puts "could not process report: #{name} reason: #{$!}"
        puts $!.backtrace
      end
    else
      LOG.warn("no such method: #{name}")
    end
  end

  def process_xml(f)
    file = File.new(f)
    doc = REXML::Document.new(file)

    doc.elements.each('reports/parameter') do |e|
      @parameters << process_parameter_xml(e)
    end

    doc.elements.each('reports/report') do |e|
      @reports << process_report_xml(e)
    end

    doc.elements.each('reports/master-report') do |e|
      @master_reports << process_master_report_xml(e)
    end
  end

  def process_master_report_xml(e)
    a = e.attributes

    reports = []
    e.each_element('report') do |e|
      reports << process_report_xml(e)
    end

    { :name => a['name'], :template => a['template'], :reports => reports }
  end

  def process_report_xml(e)
    a = e.attributes
    { :name => a['name'], :template => a['template'] }
  end

  def process_parameter_xml(e)
    a = e.attributes
    { :name => a['name'], :type => a['value'], :value => a['value'] }
  end
end

class Column
  attr_reader :name, :type, :value_expression

  def initialize(name, type, value_expression = nil)
    @name = name
    @type = type
    @value_expression = value_expression || name
  end
end

class FactTable
  attr_reader :name, :detail_table, :time_column, :dimensions, :measures
  attr_writer :dimensions, :measures

  def initialize(name, detail_table, time_column='time_stamp',
                 dimensions=[], measures=[])
    @name = name
    @detail_table = detail_table
    @time_column = time_column
    @dimensions = dimensions
    @measures = measures
  end

  def ddl()
    <<DDL
CREATE TABLE #{@name}
    (trunc_time timestamp without time zone, #{(dimensions + measures).map { |c| "#{c.name} #{c.type}" }.join(',')})
DDL
  end

  def update_table(dbh, st, et)
    insert_strs = ['trunc_time']
    select_strs = ["date_trunc('minute', #{@time_column})"]
    group_strs = ["date_trunc('minute', #{@time_column})"]

    dimensions.each do |c|
      insert_strs << c.name
      select_strs << c.value_expression
      group_strs << c.name
    end

    measures.each do |c|
      insert_strs << c.name
      select_strs << c.value_expression
    end

    dbh.prepare(<<SQL) do |ps|
INSERT INTO #{@name} (#{insert_strs.join(',')})
    SELECT #{select_strs.join(',')} FROM #{@detail_table}
    WHERE #{@time_column} >= ? AND #{@time_column} < ?
    GROUP BY #{group_strs.join(',')}
SQL
      ps.execute(st, et)
    end
  end

  private
end
