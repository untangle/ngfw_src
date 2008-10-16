require "json"
require "net/http"
require "mechanize"
require "singleton"

class WWW::Mechanize
  def post_raw(url, data, content_type = "text/plain" )
    cur_page = Page.new( nil, { 'content-type' => 'text/html' })

    abs_url = to_absolute_uri( url, cur_page )
    request = fetch_request( abs_url, :post )
    request.add_field( 'Content-Type', content_type )
    request.add_field( 'Content-Length', data.size.to_s )
    
    page = fetch_page( abs_url, request, cur_page, [data] )
    add_to_history( page )
    page
  end
end

module Untangle
  class RequestHandler
    def initialize( read_timeout = 30, open_timeout = 10 )
      @opener = WWW::Mechanize.new
      @opener.keep_alive = true
      @opener.redirect_ok = false
      @opener.read_timeout = read_timeout
      @opener.open_timeout = open_timeout
    end
    
    def make_request( url, postdata )
      page = @opener.post_raw( url, postdata )
      page.body
    end

    def to_s
      "RequestHandler"
    end

    alias :inspect :to_s
  end

  ## This will take an object an automatically convert it to its more basic type.
  class ResultHandler
    @@handlers = {}

    def self.register( java_classes, handler )
      java_classes.each { |clz| @@handlers[clz] = handler }
    end
    
    def self.fix_result( result )
      return result unless result.is_a? Hash

      handler = @@handlers[result["javaClass"]]
      
      return result if handler.nil?
      
      return handler.fix( result )
    end
  end

  class ListHandler < ResultHandler
    include Singleton

    def fix( response )
      list = response["list"]
      return response if list.nil?
      return list
    end
  end
  
  ResultHandler.register( [ "java.util.ArrayList", "java.util.LinkedList", 
                            "java.util.Collections$UnmodifiableList" ], ListHandler.instance )

  class ServiceProxy
    @@request_id = 1

    def initialize( service_url, service_name = nil, handler = nil )
      @service_url, @service_name, @handler  = service_url, service_name, handler
      @handler = RequestHandler.new if @handler.nil?
    end

    def method_missing( method_id, *args )
      return ServiceProxy.new( @service_url, method_id, @handler ) if ( @service_name.nil? )

      r( method_id, *args )
    end

    def to_s
      "#{@service_url} : #{@service_name}"
    end

    alias :inspect :to_s

    private
    def r( method_id, *args )
      postdata = { "method" => "#{@service_name}.#{method_id}", 
        'params' => args, 'id' => ServiceProxy.get_request_id() }.to_json
      respdata = @handler.make_request( @service_url, postdata )
      response = JSON::parse( respdata )
      error = response["error"]
      unless ( error.nil?  )
        raise "Unable to execute method #{@service_name}.#{method_id}, #{error}"
      end

      result = response["result"]
      ## Handle callable references
      if ( result.is_a? Hash and ( result['JSONRPCType'] == "CallableReference" ))
        object_id = result["objectID"]
        return ServiceProxy.new( @service_url, ".obj##{object_id}", @handler ) unless object_id.nil?
      end

      ## Handle fixups.
      ServiceProxy.handle_fixups( result, response["fixups"] )

      return ResultHandler.fix_result( result )
    end

    def ServiceProxy.get_request_id()
      @@request_id += 1
      @@request_id
    end

    def ServiceProxy.handle_fixups( result, fixups )
      return if fixups.nil?
      
      return unless fixups.is_a? Array
      
      ## fixups are of the form [ path_a, path_b ], where path_a defines the item to fix, and path_b
      ## defines where to copy it.
      fixups.each do |destination,source|
        original = ServiceProxy.find_object( result, source )
        next if original.nil?

        copy = ServiceProxy.find_object( result, destination[0,destination.length - 1])
        next if copy.nil?
        copy[destination[-1]] = original
      end
    end

    def ServiceProxy.find_object( base, path )
      object = base
      path.each { |key| object = object[key] }
      return object
    end
  end

  RemoteUvmContext = ServiceProxy.new( "http://localhost/webui/JSON-RPC", "RemoteUvmContext" )
end

