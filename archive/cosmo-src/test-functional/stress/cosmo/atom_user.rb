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

require "cosmo/morse_code_client"
require "cosmo/atom_client"
require "cosmo/cmp_client"

module Cosmo

  class AtomUser < CosmoUser
    
    MAX_COL_SIZE = 3000
    MEAN_COL_SIZE = 50
    SECONDS_IN_WEEK = 60*60*24*7
    
    OPERATIONS = [:rangeQuery, :fullFeed, :createEvent, :updateEvent, :dashBoardQuery, :createCollection, :deleteEvent, :deleteCollection, :updateCollection]
    PROBS = [0.59, 0.045, 0.15, 0.05, 0.15, 0.005, 0.005, 0.0025, 0.0025]
    
    class CollectionHolder
      def initialize(uid, itemUids)
        # array of item uids
        @itemUids = itemUids
        # uid of collection
        @uid = uid
      end
    
      def randomItemUid
        @itemUids[rand(@itemUids.size)]
      end
      
      attr_accessor :itemUids
      attr_reader :uid
    end 
    
    
    def initialize(server, port, context, user, pass, iterations=1, timeBased=false, stats=nil)
      super(server,port,context,user,pass,iterations,timeBased,stats)
      @mcClient = MorseCodeClient.new(server, port,context, @user, @pass)
      @atomClient = AtomClient.new(server,port,context, @user, @pass)
    end
    
    def registerStats
      @stats.registerStatMap(:atomCreateCollection, "atom create collection")
      @stats.registerStatMap(:atomDeleteCollection, "atom delete collection")
      @stats.registerStatMap(:atomUpdateCollection, "atom update collection")
      @stats.registerStatMap(:atomRangeQuery, "atom range query")
      @stats.registerStatMap(:atomFullFeed, "atom full feed")
      @stats.registerStatMap(:atomCreateEntry, "atom create entry")
      @stats.registerStatMap(:atomDeleteEntry, "atom delete entry")
      @stats.registerStatMap(:atomUpdateEntry, "atom update entry")
      @stats.registerStatMap(:dashboardQuery, "atom dashboard query")
    end
    
    
    def preRun
      # set of all collections published by current user
      @collections = []
    end
    
    def runIteration
      # wait a random time before continuing with the next operation
        randomWait
        # must have a collection, so if there isn't one...that is our operation
        if(@collections.size==0)
          collection = createCollection
          # only add collection to current set if publish succeeded
          if(!collection.nil?)
            @collections << collection
          end
        else
          # otherwise figure out what operation to perform
          operation = getNextOperation(OPERATIONS, PROBS)
          case operation
            when :rangeQuery
              collection = @collections[rand(@collections.size)]
              rangeQuery(collection)
            when :fullFeed
              collection = @collections[rand(@collections.size)]
              fullFeed(collection)
            when :createEvent
              collection = @collections[rand(@collections.size)]
              createEvent(collection)
            when :updateEvent
              collection = @collections[rand(@collections.size)]
              if(collection.itemUids.size==0)
                createEvent(collection)
              else
                updateEvent(collection)
              end
            when :dashBoardQuery
              collection = @collections[rand(@collections.size)]
              dashboardQuery(collection)
            when :createCollection
                collection = createCollection
                # only add collection to current set if publish succeeded
                if(!collection.nil?)
                  @collections << collection
                end
            when :deleteEvent
              collection = @collections[rand(@collections.size)]
              if(collection.itemUids.size==0)
                createEvent(collection)
              else
                deleteEvent(collection)
              end
            when :deleteCollection
              collection = @collections[rand(@collections.size)]
              deleted = deleteCollection(collection)
              @collections.delete(collection) if deleted==true
            when :updateCollection
              collection = @collections[rand(@collections.size)]
              updateCollection(collection)
          end
        end
    end
    
    def randomWait
      sleep(rand/2.to_f)
    end
    
    def createCollection
      colUid = random_string(40)
      colName = random_string(64)
      
      colXml = generateCollectionXml(colUid, colName)
      resp = @atomClient.createCollection(colXml)
      if(resp.code==201)
        @stats.reportStat(:atomCreateCollection, true, resp.time, colXml.length, nil, 201)
        return CollectionHolder.new(colUid, [])
      else
        @stats.reportStat(:atomCreateCollection, false, nil, nil, nil, resp.code)
        return nil
      end
    end
    
    def fullFeed(collection)
      resp = @atomClient.getFullFeed(collection.uid)
      if(resp.code==200)
        @stats.reportStat(:atomFullFeed, true, resp.time, nil, resp.data.length, 200)
      else
        @stats.reportStat(:atomFullFeed, false, nil, nil, nil, resp.code)
      end
    end
    
    def rangeQuery(collection)
      startDate, endDate = get_date_range
      resp = @atomClient.getFullFeed(collection.uid, "eim-json", startDate, endDate)
      if(resp.code==200)
        @stats.reportStat(:atomRangeQuery, true, resp.time, nil, resp.data.length, 200)
      else
        @stats.reportStat(:atomRangeQuery, false, nil, nil, nil, resp.code)
      end
    end
    
    def dashboardQuery(collection)
      triage_status = get_triage_status
      resp = @atomClient.getDashboardFeed(collection.uid, triage_status, "eim-json")
      if(resp.code==200)
        @stats.reportStat(:dashboardQuery, true, resp.time, nil, resp.data.length, 200)
      else
        @stats.reportStat(:dashboardQuery, false, nil, nil, nil, resp.code)
      end
    end
    
    def createEvent(collection)
      eventUid = random_string(40)
      eventEntry = generateItemEntry(eventUid) 
      resp = @atomClient.createEntry(collection.uid, eventEntry)
      if(resp.code==201)
        @stats.reportStat(:atomCreateEntry, true, resp.time, eventEntry.length, resp.data.length, 201)
        collection.itemUids << eventUid
      else
        @stats.reportStat(:atomCreateEntry, false, nil, nil, nil, resp.code)
      end
    end
    
    def updateEvent(collection)
      eventUid = collection.randomItemUid
      eventEntry = generateItemEntry(eventUid) 
      resp = @atomClient.updateEntry(eventUid, eventEntry)
      if(resp.code==200)
        @stats.reportStat(:atomUpdateEntry, true, resp.time, eventEntry.length, resp.data.length, 200)
      else
        @stats.reportStat(:atomUpdateEntry, false, nil, nil, nil, resp.code)
      end
    end
    
    def deleteEvent(collection)
      eventUid = collection.randomItemUid
      eventEntry = generateItemEntry(eventUid) 
      resp = @atomClient.deleteItem(eventUid)
      if(resp.code==204)
        @stats.reportStat(:atomDeleteEntry, true, resp.time, nil, nil, 204)
        collection.itemUids.delete(eventUid)
      else
        @stats.reportStat(:atomDeleteEntry, false, nil, nil, nil, resp.code)
      end
    end
    
    def updateCollection(collection)
      colXml = generateUpdateCollectionXml(random_string(64))
      resp = @atomClient.updateCollection(collection.uid,colXml)
      if(resp.code==204)
        @stats.reportStat(:atomUpdateCollection, true, resp.time, colXml.length, nil, 204)
      else
        @stats.reportStat(:atomUpdateCollection, false, nil, nil, nil, resp.code)
      end
    end
    
    def deleteCollection(collection)
      resp = @atomClient.deleteCollection(collection.uid)
      if(resp.code==204)
        @stats.reportStat(:atomDeleteCollection, true, resp.time, nil, nil, 204)
      else
        @stats.reportStat(:atomDeleteCollection, false, nil, nil, nil, resp.code)
      end
      return (resp.code==204)
    end
   
    def generateCollectionXml(uid, name)
      xml =<<EOF
      <div class="collection">
        <span class="name">#{name}</span>
        <span class="uuid">#{uid}</span>
      </div>
EOF
      return xml
    end
  
    def generateUpdateCollectionXml(name)
      xml =<<EOF
      <div class="collection">
        <span class="name">#{name}</span>
      </div>
EOF
      return xml
    end
  
    def generateItemEntry(uid)
      entry = <<EOF
      <entry xmlns="http://www.w3.org/2005/Atom">
      <title>#{random_string}</title>
      <id>urn:uuid:#{uid}</id>
      <updated>2007-06-05T10:58:15-07:00</updated>
      <author><name>#{random_string}</name></author>
      <content type="text/eim+json">{"uuid":"#{uid}",
      "records":{"item":{"prefix":"item","ns":"http://osafoundation.org/eim/item/0",
      "key":{"uuid":["text","#{uid}"]},"fields":{"title":["text","#{random_string}"]}},
      "note":{"prefix":"note","ns":"http://osafoundation.org/eim/note/0",
      "key":{"uuid":["text","#{uid}"]},"fields":{}},
      "modby":{"prefix":"modby","ns":"http://osafoundation.org/eim/modby/0",
      "key":{"uuid":["text","#{uid}"],"userid":["text",""],
      "action":["integer",100],"timestamp":["decimal",1181066295836]}},
      #{generateEventEntry(uid)}}}</content></entry>
EOF

    end
  
    def generateEventEntry(uid)
      prob = rand
      if prob < 0.05
        generateRecurringEventEntry(uid)
      elsif prob <0.07
        generateRegularAllDayEventEntry(uid)
      else
        generateRegularEventEntry(uid)
      end
    end
  
    def generateRegularEventEntry(uid)
      entry = <<EOF
      "event":{"prefix":"event","ns":"http://osafoundation.org/eim/event/0",
      "key":{"uuid":["text","#{uid}"]},"fields":{"dtstart":["text","#{random_datetime}"],
      "duration":["text","#{random_duration}"]}}
EOF
  
    end
    
     def generateRegularAllDayEventEntry(uid)
      entry = <<EOF
      "event":{"prefix":"event","ns":"http://osafoundation.org/eim/event/0",
      "key":{"uuid":["text","#{uid}"]},"fields":{"dtstart":["text","#{random_date}"],
      "duration":["text","#{random_day_duration}"]}}
EOF
  
    end
  
    def generateRecurringEventEntry(uid)
      entry = <<EOF
      "event":{"prefix":"event","ns":"http://osafoundation.org/eim/event/0",
      "key":{"uuid":["text","#{uid}"]},"fields":{"dtstart":["text","#{random_date}"],
      "rrule":["text",";FREQ=WEEKLY"],
      "duration":["text","#{random_duration}"]}}
EOF
  
    end
   
    def random_collection_size
      return 20
      # generate exponentially distributed number
      #result = (-Math.log(1.0 - rand)/(0.02)).to_i
      #while(result==0 || result > MAX_COL_SIZE)
      #  result = (-Math.log(1.0 - rand)/(0.02)).to_i
      #end
      #return result
    end
    
    def get_date_range
      startTime = Time.gm(2007,"jan",1,0,0,0)
      startTime += rand(51) * SECONDS_IN_WEEK
      endTime = startTime + SECONDS_IN_WEEK
      
      return format_date_range(startTime), format_date_range(endTime)
    end
    
    def get_triage_status
      case rand
        when 0..0.333 
          "dashboard-now"
        when 0.333..0.666
          "dashboard-later"
        else
          "dashboard-done"     
      end
    end
    
    def format_date_range(date)
      return date.strftime("%Y-%m-%dT%H:%M:%SZ")
    end
    
  end
end
