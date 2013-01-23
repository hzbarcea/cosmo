# Copyright 2007 Open Source Applications Foundation
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

require "cosmo/cosmo_user"
require 'log4r'

include Log4r

module Cosmo
  
  class AtomResponse < BaseServerResponse
    def initialize(resp, data=nil, time=0)
      super(resp, data, time)
    end
  end
  
  class AtomClient < BaseHttpClient
    @@log = Logger.new 'AtomClient'
    
    ATOM_PATH = "/atom/"
    
    def initialize(server, port, context, user, pass)
      super(server,port,context,user,pass)
    end
    
    def getFullFeed(collection, format=nil, startRange=nil, endRange=nil)
      @@log.debug "getFullFeed #{collection} begin"
      @http.start do |http|
        
        if(format.nil?)
          strRequest = "#{@context}#{ATOM_PATH}collection/#{collection}/full"
        else
          strRequest = "#{@context}#{ATOM_PATH}collection/#{collection}/full/#{format}"
        end
        
        strRequest << "?start=#{startRange}&end=#{endRange}" if !startRange.nil?
        req = Net::HTTP::Get.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        resp, data = time_block { http.request(req) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "getFullFeed (#{format}) for #{collection} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def getDashboardFeed(collection, triage_status="dashboard-now", format=nil)
      @@log.debug "getDashboardFeed #{collection} begin"
      @http.start do |http|
        
        if(format.nil?)
          strRequest = "#{@context}#{ATOM_PATH}collection/#{collection}/#{triage_status}"
        else
          strRequest = "#{@context}#{ATOM_PATH}collection/#{collection}/#{triage_status}/#{format}"
        end
        
        req = Net::HTTP::Get.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        resp, data = time_block { http.request(req) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "getDashboardFeed (#{triage_status}) for #{collection} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def createCollection(body, user=nil)
      @@log.debug "post collection to #{user.nil? ? @user : user} begin"
      @http.start do |http|
        
        strRequest = "#{@context}#{ATOM_PATH}user/#{user.nil? ? @user : user}"
        
        req = Net::HTTP::Post.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        req['Content-Type'] = 'application/xhtml+xml'
        resp, data = time_block { http.request(req, body) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "post collection to user #{user.nil? ? @user : user} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def updateCollection(collection, body)
      @@log.debug "put to collection #{collection} begin"
      @http.start do |http|
        
        strRequest = "#{@context}#{COL_PATH}collection/#{collection}"
        
        req = Net::HTTP::Put.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        req['Content-Type'] = 'application/xhtml+xml'
        resp, data = time_block { http.request(req, body) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "put to collection #{collection} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def deleteCollection(collection)
      @@log.debug "delete collection #{collection} begin"
      @http.start do |http|
        req = Net::HTTP::Delete.new("#{@context}#{ATOM_PATH}collection/#{collection}")
        init_req(req)
        http.read_timeout=600
        
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        resp, data = time_block { http.request(req) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "delete collection #{collection} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def deleteItem(item)
      @@log.debug "delete item #{item} begin"
      @http.start do |http|
        req = Net::HTTP::Delete.new("#{@context}#{ATOM_PATH}item/#{item}")
        init_req(req)
        http.read_timeout=600
        
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        resp, data = time_block { http.request(req) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "delete item #{item} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def createEntry(collection, body)
      @@log.debug "post #{collection} begin"
      @http.start do |http|
      
        strRequest = "#{@context}#{ATOM_PATH}collection/#{collection}"
       
        req = Net::HTTP::Post.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        req['Content-Type'] = 'application/atom+xml'
        resp, data = time_block { http.request(req, body) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "post for #{collection} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def updateEntry(item,  body)
        @@log.debug "put #{item} begin"
        @http.start do |http|
      
        strRequest = "#{@context}#{ATOM_PATH}item/#{item}"
       
        req = Net::HTTP::Put.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        req['Content-Type'] = 'application/atom+xml'
        resp, data = time_block { http.request(req, body) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "put for #{item} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def createPreference(key, value)
        @@log.debug "create preference #{key}=#{value} begin"
        
        xml =<<EOF
<entry xmlns="http://www.w3.org/2005/Atom">
  <content type="xhtml">
    <div xmlns="http://www.w3.org/1999/xhtml">
      <div class="preference">Preference:
        <span class="key">#{key}</span> =
        <span class="value">#{value}</span>
      </div>
    </div>
  </content>
</entry>
EOF
        
        @http.start do |http|
          
        strRequest = "#{@context}#{ATOM_PATH}user/#{@user}/preferences"
        req = Net::HTTP::Post.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        req['Content-Type'] = 'application/atom+xml'
        resp, data = time_block { http.request(req, xml) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "create preference #{key}=#{value} end(#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
        
      end
    end
    
    def updatePreference(key, value)
        @@log.debug "update preference #{key}=#{value} begin"
        
        xml =<<EOF
<entry xmlns="http://www.w3.org/2005/Atom">
  <content type="xhtml">
    <div xmlns="http://www.w3.org/1999/xhtml">
      <div class="preference">Preference:
        <span class="key">#{key}</span> =
        <span class="value">#{value}</span>
      </div>
    </div>
  </content>
</entry>
EOF
        
        @http.start do |http|
          
        strRequest = "#{@context}#{ATOM_PATH}user/#{@user}/preference/#{key}"
        req = Net::HTTP::Put.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        req['Content-Type'] = 'application/atom+xml'
        resp, data = time_block { http.request(req, xml) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "update preference #{key}=#{value} end(#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
        
      end
    end
    
    def deletePreference(key)
      @@log.debug "delete preference #{key} begin"
      @http.start do |http|
        req = Net::HTTP::Delete.new("#{@context}#{ATOM_PATH}user/#{user}/preference/#{key}")
        init_req(req)
        http.read_timeout=600
        
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        resp, data = time_block { http.request(req) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "delete preference #{key} end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
    def getPreferencesFeed()
      @@log.debug "getPreferencesFeed #{user} begin"
      @http.start do |http|
        
        strRequest = "#{@context}#{ATOM_PATH}user/#{@user}/preferences"
        
        req = Net::HTTP::Get.new(strRequest)
        init_req(req)
        http.read_timeout=600
        # we make an HTTP basic auth by passing the
        # username and password
        req.basic_auth @user, @pass
        resp, data = time_block { http.request(req) }
        @@log.debug "received code #{resp.code}"
        @@log.debug "getPreferencesFeed end (#{@reqTime}ms)"
        return AtomResponse.new(resp, data, @reqTime)
      end
    end
    
  end
end
