# -*-ruby-*-
# $Id$

#++
#
# = StampTask
#
# This is a task that keeps track of the last time that it was
# executed.  This is useful if you have operations that perform
# a lot of tasks, but can't be defined by exactly one file.
# For example, if you have a task that Generates a number of
# source files, it is easy to depend for another task to depend
# on that task, but difficult to depend on the files that it
# creates.

require 'thread'

module Rake
  ## This is a special task that keeps track of the last time it was run
  ## Therefore it can depend on files, and only run if the files have changed.
  class StampTask < Task
    def timestamp
      StampHash.instance[name]
    end

    # Is this file task needed?  Yes if it doesn't exist, or if its time
    # stamp is out of date.
    def needed?
      return true if out_of_date?(timestamp)
      false
    end

    # Yo rake peeps: changing method signatures between
    # version 0.7 and 0.8 ain't kewl.
    rake_package_version = `dpkg -l rake | tail -1 | awk '{print $3}'`
    if rake_package_version.to_f >= 0.8
      def execute(task_args)
        super(task_args)
        ## After the task has executed, set the stamp hash,
        ## If execute fails, then the timestamp will not be set
        Rake::StampHash.instance.set(name)
      end
    else
      def execute
        super
        ## After the task has executed, set the stamp hash,
        ## If execute fails, then the timestamp will not be set
        Rake::StampHash.instance.set(name)
      end
    end

    def print_needed
      prereq = @prerequisites.find { |n| Rake::Task[n].timestamp > timestamp()}
      return if prereq == nil
      print "needed:"
      puts prereq
      prereq.each { |n| if Rake::Task[n].class.name == "Rake::StampTask" then Rake::Task[n].print_needed end }
    end      

    private

    # Are there any prerequisites with a later time than the given
    # time stamp?
    def out_of_date?(stamp)
      @prerequisites.any? { |n| Rake::Task[n].timestamp > stamp}
    end
  end

  ## A way to save the state of all of the tasks into a file
  class StampHash
    include Singleton
    def initialize
      @stampHash = {}
      @save = false
      @clear = false
      @lock = Mutex.new

      return unless File.exist? Rake::StampFile

      open(Rake::StampFile,"r") do |input|
        while line = input.gets
          params = line.split(';')
          ## puts params
          next if ( params.length != 3 )
          @stampHash[params[0]]=Time.at(params[1].to_i,params[2].to_i)
        end
      end
    end

    ## Lookup a task
    def [](name)
      value = @stampHash[name]
      return Rake::EARLY if value.nil?
      return value
    end

    ## Save the time when a task was run
    def set(name)
      registerExitHandler
      @stampHash[name] = Time.now
    end

    ## Clear all of the values at save time (This should be used in clean tasks)
    def clear
      registerExitHandler
      @clear = true
    end

    def registerExitHandler
      @lock.synchronize do
        # Register an exit handler if one hasn't been yet
        at_exit { save } if ( @save == false )

        @save = true
      end
    end

    ## Save the state back to a file
    def save
      ## If you are clearing all of the state, save an empty hash
      @stampHash = [] if @clear
      open(Rake::StampFile,"w") do |input|
        @stampHash.each { |k,v| input.puts "#{k};#{v.tv_sec};#{v.tv_usec}" }
      end
    end

    private :save, :registerExitHandler
  end
end


def stamptask(args,&block)
  Rake::StampTask.define_task(args,&block)
end
