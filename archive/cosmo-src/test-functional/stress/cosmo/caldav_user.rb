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

require "cosmo/caldav_client"
require "cosmo/cmp_client"

module Cosmo

  class CalDAVUser < CosmoUser
  
    SECONDS_IN_WEEK = 60*60*24*7
    OPERATIONS = [:makeCalendar, :putItem, :deleteItem, :rangeQuery, :getItem, :deleteCollection, :propfindCollection]
    PROBS = [0.005, 0.25, 0.005, 0.6375, 0.05, 0.0025, 0.05]
    
    class CollectionHolder
      def initialize(uid)
        # array of item uids
        @itemUids = []
        # uid of collection
        @uid = uid
      end
      
      def randomItemUid
        @itemUids[rand(@itemUids.size)]
      end
      
      attr_accessor :itemUids
      attr_reader :uid
    end
      
    def initialize(server, port,context, user, pass, iterations=1, timeBased=false, stats=nil)
      super(server,port,context,user,pass,iterations,timeBased,stats)
      @calDavClient = CalDAVClient.new(server,port,context, @user, @pass)
    end
    
    def registerStats
      @stats.registerStatMap(:calDavMkCalendar, "CalDAV MKCALENDAR")
      @stats.registerStatMap(:calDavPut, "CalDAV PUT")
      @stats.registerStatMap(:calDavGet, "CalDAV GET")
      @stats.registerStatMap(:calDavDeleteItem, "CalDAV DELETE Resource")
      @stats.registerStatMap(:calDavDeleteCollection, "CalDAV DELETE Collection")
      @stats.registerStatMap(:calDavRangeQuery, "CalDAV time-range REPORT")
      @stats.registerStatMap(:calDavPropfindCollection, "CalDAV propfind collection")
    end
    
    def preRun
      # set of all collections published by current user
      @collections = {}
    end
    
    def runIteration
      # wait a random time before continuing with the next operation
        randomWait
        # must have a collection, so if there isn't one...that is our operation
        if(@collections.size==0)
          collection = createCollection
          # only add collection to current set if publish succeeded
          if(!collection.nil?)
            @collections[collection.uid] = collection
          end
        else
          # otherwise figure out what operation to perform
          operation = getNextOperation(OPERATIONS, PROBS)
          case operation
            when :makeCalendar
              collection = createCollection
              if(!collection.nil?)
                @collections[collection.uid] = collection
              end
            when :putItem
              collection = @collections.to_a[rand(@collections.size)][1]
              putItem(collection)
            when :deleteItem
              collection = @collections.to_a[rand(@collections.size)][1]
              deleteItem(collection)
            when :getItem
              collection = @collections.to_a[rand(@collections.size)][1]
              getItem(collection)
            when :rangeQuery
              collection = @collections.to_a[rand(@collections.size)][1]
              rangeQueryCalendar(collection)
            when :deleteCollection
              collection = @collections.to_a[rand(@collections.size)][1]
              deleted = deleteCollection(collection)
              @collections.delete(collection.uid) if deleted==true
            when :propfindCollection
              collection = @collections.to_a[rand(@collections.size)][1]
              propfindCollection(collection)
          end
        end
    end
    
    def randomWait
      sleep(rand/2.to_f)
    end
    
    def createCollection
      colUid = random_string(40)
      itemUids = []
      
      colXml = generateMkCalendarXml(colUid)
      
      resp = @calDavClient.makeCalendar("#{@user}/#{colUid}" , colXml)
      if(resp.code==201)
        @stats.reportStat(:calDavMkCalendar, true, resp.time, colXml.length, nil, 201)
        return CollectionHolder.new(colUid)
      else
        @stats.reportStat(:calDavMkCalendar, false, nil, nil, nil, resp.code)
        return nil
      end
    end
    
    def propfindCollection(collection)
      xml = getPropFindXml
      resp = @calDavClient.propfind("#{@user}/#{collection.uid}", xml, "1")
      if(resp.code==207)
        @stats.reportStat(:calDavPropfindCollection, true, resp.time, xml.length, resp.data.length, 207)
      else
        @stats.reportStat(:calDavPropfindCollection, false, nil, nil, nil, resp.code)
      end
    end
    
    def deleteCollection(collection)
      resp = @calDavClient.delete("#{@user}/#{collection.uid}")
      if(resp.code==204)
        @stats.reportStat(:calDavDeleteCollection, true, resp.time, nil, nil, 204)
      else
        @stats.reportStat(:calDavDeleteCollection, false, nil, nil, nil, resp.code)
      end
      return (resp.code==204)
    end
    
    
    
    def putItem(collection)
      
      itemUid = random_string(40)
      ics = rand < 0.90 ?  generateEventIcs(itemUid) :  generateTaskIcs(itemUid)
      
      resp = @calDavClient.put("#{@user}/#{collection.uid}/#{itemUid}.ics", ics)
      
      if(resp.code==201)
        @stats.reportStat(:calDavPut, true, resp.time, ics.length, nil, 201)
        collection.itemUids << itemUid
      else
        @stats.reportStat(:calDavPut, false, nil, nil, nil, resp.code)
      end
    end
    
    def deleteItem(collection)
      return if(collection.itemUids.size==0)
      itemUid = collection.randomItemUid
      
      resp = @calDavClient.delete("#{@user}/#{collection.uid}/#{itemUid}.ics")
      
      if(resp.code==204)
        @stats.reportStat(:calDavDeleteItem, true, resp.time, nil, nil, 204)
        collection.itemUids.delete(itemUid)
      else
        @stats.reportStat(:calDavDeleteItem, false, nil, nil, nil, resp.code)
      end
    end
    
    def getItem(collection)
      return if(collection.itemUids.size==0)
      itemUid = collection.randomItemUid
      
      resp = @calDavClient.get("#{@user}/#{collection.uid}/#{itemUid}.ics")
      
      if(resp.code==200)
        @stats.reportStat(:calDavGet, true, resp.time, resp.data.length, nil, 200)
      else
        @stats.reportStat(:calDavGet, false, nil, nil, nil, resp.code)
      end
    end
    
    def rangeQueryCalendar(collection)
      
      queryXml = generateQueryXml
      
      resp = @calDavClient.report("#{@user}/#{collection.uid}", queryXml)
      
      if(resp.code==207)
        @stats.reportStat(:calDavRangeQuery, true, resp.time, resp.data.length, nil, 207)
      else
        @stats.reportStat(:calDavRangeQuery, false, nil, nil, nil, resp.code)
      end
    end
     
    def generateMkCalendarXml(uid)
      xml =<<EOF
     <C:mkcalendar xmlns:D="DAV:"
                   xmlns:C="urn:ietf:params:xml:ns:caldav">
       <D:set>
         <D:prop>
           <D:displayname>Someone's Events</D:displayname>
           <C:calendar-description xml:lang="en">Calendar restricted to events.</C:calendar-description>
           <C:calendar-timezone><![CDATA[BEGIN:VCALENDAR
PRODID:-//Example Corp.//CalDAV Client//EN
VERSION:2.0
BEGIN:VTIMEZONE
TZID:US-Eastern
LAST-MODIFIED:19870101T000000Z
BEGIN:STANDARD
DTSTART:19671029T020000
RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10
TZOFFSETFROM:-0400
TZOFFSETTO:-0500
TZNAME:Eastern Standard Time (US & Canada)
END:STANDARD
BEGIN:DAYLIGHT
DTSTART:19870405T020000
RRULE:FREQ=YEARLY;BYDAY=1SU;BYMONTH=4
TZOFFSETFROM:-0500
TZOFFSETTO:-0400
TZNAME:Eastern Daylight Time (US & Canada)
END:DAYLIGHT
END:VTIMEZONE
END:VCALENDAR]]></C:calendar-timezone>
         </D:prop>
       </D:set>
     </C:mkcalendar>
EOF
      return xml
    end
    
    def getPropFindXml
       xml =<<EOF
<D:propfind xmlns:D="DAV:">
<D:prop>
<D:getcontentlength/>
<D:getcontenttype/>
<D:resourcetype/>
<D:getetag/>
</D:prop>
</D:propfind>
EOF
    end
    
    def generateQueryXml
      startRange, endRange = get_date_range
      xml =<<EOF
     <C:calendar-query xmlns:C="urn:ietf:params:xml:ns:caldav">
       <D:prop xmlns:D="DAV:">
         <D:getetag/>
         <C:calendar-data/>
       </D:prop>
       <C:filter>
         <C:comp-filter name="VCALENDAR">
           <C:comp-filter name="VEVENT">
               <C:time-range start="#{startRange}"
                               end="#{endRange}"/>
           </C:comp-filter>
         </C:comp-filter>
       </C:filter>
     </C:calendar-query>
EOF
      return xml
    end
    
    def generateEventIcs(uid)
      if((rand < 0.05))
        generateRecurringEventIcs(uid)
      else
        generateRegularEventIcs(uid)
      end
    end
    
    def generateRegularEventIcs(uid)
      recurring = (rand < 0.05)
      ics = <<EOF
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Example Corp.//CalDAV Client//EN
BEGIN:VEVENT
UID:#{uid}
DTSTAMP:20070712T182145Z
DTSTART:#{random_date}
DURATION:#{random_duration}
SUMMARY:#{random_string(50)}
END:VEVENT
END:VCALENDAR
EOF
      return ics
    end
    
    def generateRecurringEventIcs(uid)
      ics = <<EOF
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Example Corp.//CalDAV Client//EN
BEGIN:VEVENT
UID:#{uid}
DTSTAMP:20070712T182145Z
DTSTART:#{random_date}
DURATION:#{random_duration}
RRULE:FREQ=WEEKLY
SUMMARY:#{random_string(50)}
DESCRIPTION:#{random_string(100)}
END:VEVENT
END:VCALENDAR
EOF
      return ics
    end
     
    def generateTaskIcs(uid)
      ics = <<EOF
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Example Corp.//CalDAV Client//EN
BEGIN:VTODO
UID:#{uid}
DTSTAMP:19970901T130000Z
SUMMARY:#{random_string(50)}
DESCRIPTION:#{random_string(100)}
STATUS:NEEDS-ACTION
END:VTODO
END:VCALENDAR
EOF
      return ics
    end
     
    def random_date
      date = "2007" + random_integer_string(12) + random_integer_string(28) +
        "T" + random_integer_string(23) + random_integer_string(59) + "00Z"
    end
    
    def random_duration
      durs = ["PT30M", "PT60M", "PT90M"]
      return durs[rand(durs.size)]
    end
    
    def random_integer_string(max)
      randInt = rand(max)
      while randInt==0
        randInt = rand(max)
      end
      
      if(randInt < 10)
        randInt = "0" + randInt.to_s
      else
        randInt = randInt.to_s
      end
    
      return randInt  
    end
    
    def get_date_range
      startTime = Time.gm(2007,"jan",1,0,0,0)
      startTime += rand(51) * SECONDS_IN_WEEK
      endTime = startTime + SECONDS_IN_WEEK
      
      return format_date_range(startTime), format_date_range(endTime)
    end
    
    def format_date_range(date)
      return date.strftime("%Y%m%dT%H%M%SZ")
    end
    
  end
end
