/*
 * Copyright 2006 Open Source Applications Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osaf.cosmo.dao.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.osaf.cosmo.calendar.util.CalendarFlattener;
import org.osaf.cosmo.model.CalendarPropertyIndex;
import org.osaf.cosmo.model.CalendarTimeRangeIndex;
import org.osaf.cosmo.model.CalendarItem;

public class DefaultCalendarIndexer implements CalendarIndexer {

    private static final Log log = LogFactory
            .getLog(DefaultCalendarIndexer.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.hibernate.CalendarIndexer#indexCalendarEvent(org.hibernate.Session,
     *      org.osaf.cosmo.model.persistence.DbItem,
     *      net.fortuna.ical4j.model.Calendar)
     */
    public void indexCalendarEvent(Session session, CalendarItem item,
            Calendar calendar) {
        HashMap timeRangeMap = new HashMap();
        Map propertyMap = new HashMap();
        Collection indices = new ArrayList();
        CalendarFlattener flattener = new CalendarFlattener();
        propertyMap = flattener.flattenCalendarObject(calendar);
        flattener.doTimeRange(calendar, timeRangeMap);
        
        // remove previous indexes
        item.getTimeRangeIndexes().removeAll(item.getTimeRangeIndexes());
        item.getPropertyIndexes().removeAll(item.getPropertyIndexes());
        
        for (Iterator it = propertyMap.entrySet().iterator(); it.hasNext();) {
            Entry nextEntry = (Entry) it.next();
            CalendarPropertyIndex index = new CalendarPropertyIndex();
            index.setName((String) nextEntry.getKey());
            index.setValue((String) nextEntry.getValue());
            item.addPropertyIndex(index);
//             if (log.isDebugEnabled())
//                 log.debug("creating calendar property index: " + index.toString());
        }
        
        for (Iterator it = timeRangeMap.entrySet().iterator(); it.hasNext();) {
            Entry entry = (Entry) it.next();
            addIndicesForTerm(indices, (String) entry.getKey(), (String) entry
                    .getValue());
        }

        for (Iterator it = indices.iterator(); it.hasNext();) {
            CalendarTimeRangeIndex index = (CalendarTimeRangeIndex) it.next();
            item.addTimeRangeIndex(index);
//             if (log.isDebugEnabled())
//                 log.debug("creating calendar timerange index: " + index.toString());
        }
        
        session.update(item);
    }

    private void addIndicesForTerm(Collection indices, String key, String value) {
        StringTokenizer periodTokens = new StringTokenizer(value, ",");
        boolean recurring = value.indexOf(',') > 0;

        while (periodTokens.hasMoreTokens()) {
            // Try to parse term data into start/end period items, or just a
            // start (which may happen if querying a single date property)
            String token = periodTokens.nextToken().toUpperCase();
            int slashPos = token.indexOf('/');
            String testStart = (slashPos != -1) ? token.substring(0, slashPos)
                    : token;
            String testEnd = (slashPos != -1) ? token.substring(slashPos + 1)
                    : null;

            // Check whether floating or fixed test required
            boolean fixed = (testStart.indexOf('Z') != -1);

            indices
                    .add(createIndex(key, testStart, testEnd, !fixed, recurring));
        }
    }

    private CalendarTimeRangeIndex createIndex(String type, String start, String end,
            boolean isFloating, boolean isRecurring) {
        CalendarTimeRangeIndex index = new CalendarTimeRangeIndex();
        index.setType(type);
        index.setEndDate(end);
        index.setStartDate(start);
        index.setIsFloating(isFloating);
        index.setIsRecurring(isRecurring);
        return index;
    }

}
