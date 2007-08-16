/*
 * Copyright 2007 Open Source Applications Foundation
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
package org.osaf.cosmo.model.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;

import org.osaf.cosmo.calendar.RecurrenceExpander;
import org.osaf.cosmo.calendar.util.Dates;
import org.osaf.cosmo.model.EventExceptionStamp;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.ModificationUid;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.NoteOccurrence;

/**
 * Helper class to handle breaking a recurring series to
 * provide "This And Future Items" support.  A "This And Future"
 * change to a recurring event is handled by creating a 
 * new recurring series starting on the selected occurrence
 * date (this) and modifying the existing recurring series to
 * end before the selected occurrence.  If the existing series
 * contains modifications, any modification that occurs after the
 * break has to be removed and added to the new series.
 */
public class ThisAndFutureHelper {
    
    private static final long TIME_OFFSET = 1000;
    
    /**
     * Given an existing recurring series and new series, break the
     * existing series at the given occurrence and move all modifications
     * from the existing series that apply to the new series to the 
     * new series.
     * @param oldSeries note representing recurring series to break
     * @param newSeries note representing new series
     * @param occurrence occurrence of old series 
     *        (NoteOccurrence or NoteItem modification) to break old 
     *        series at.
     * @return Set of modifications that need to be removed and added.  
     *         Removals are indicated with isActive==false.
     *         All other active NoteItems are considered additions.
     */
    public Set<NoteItem> breakRecurringEvent(NoteItem oldSeries, NoteItem newSeries, NoteItem occurrence) {
        Date lastRid = null;
        if(occurrence instanceof NoteOccurrence)
            lastRid = ((NoteOccurrence) occurrence).getOccurrenceDate();
        else {
            EventExceptionStamp ees = EventExceptionStamp.getStamp(occurrence);
            lastRid = ees.getRecurrenceId();
        }
        
        return breakRecurringEvent(oldSeries, newSeries, lastRid);
    }
    
    /**
     * Given an existing recurring series and new series, break the
     * existing series at the given date and move all modifications
     * from the existing series that apply to the new series to the 
     * new series.
     * @param oldSeries note representing recurring series to break
     * @param newSeries note representing new series
     * @param lastRecurrenceId date to break the old series at
     * @return Set of modifications that need to be removed and added.  
     *         Removals are indicated with isActive==false.
     *         All other active NoteItems are considered additions.
     */
    public Set<NoteItem> breakRecurringEvent(NoteItem oldSeries, NoteItem newSeries, Date lastRecurrenceId) {
        
        LinkedHashSet<NoteItem> results = new LinkedHashSet<NoteItem>();
        
        HashSet<NoteItem> toRemove = new HashSet<NoteItem>();
        HashSet<NoteItem> toAdd = new HashSet<NoteItem>();
        
        // first break old series by setting UNTIL on RECURs
        modifyOldSeries(oldSeries, lastRecurrenceId);
        
        // get list of modifications that need to be moved
        List<NoteItem> modsToMove = getModifiationsToMove(oldSeries, newSeries, lastRecurrenceId);
        
        // move modifications by creating copy
        for(NoteItem modToMove: modsToMove) {
            
            // copy modification
            NoteItem copy = (NoteItem) modToMove.copy();
            copy.setModifies(newSeries);
            copy.setIcalUid(null);
            
            EventExceptionStamp ees =
                EventExceptionStamp.getStamp(copy);
           
            ees.setIcalUid(newSeries.getIcalUid());
            
            Date recurrenceId = ees.getRecurrenceId();
           
            copy.setUid(new ModificationUid(newSeries, recurrenceId).toString());
            
            // delete old
            modToMove.setIsActive(false);
            toRemove.add(modToMove);
            
            // add new
            toAdd.add(copy);
        }
        
        // add removals first
        results.addAll(toRemove);
        
        // then additions
        results.addAll(toAdd);
        
        return results;
    }
    
    private void modifyOldSeries(NoteItem oldSeries, Date lastRecurrenceId) {
        EventStamp event = EventStamp.getStamp(oldSeries);
        // UNTIL should be just before lastRecurrence (don't include lastRecurrence)
        Date untilDate = Dates.getInstance(new java.util.Date(lastRecurrenceId.getTime()
                - TIME_OFFSET), lastRecurrenceId);
        
        List<Recur> recurs = event.getRecurrenceRules();
        for (Recur recur : recurs)
            recur.setUntil(untilDate);
        
        // TODO: Figure out what to do with RDATEs
    }
    
    private List<NoteItem> getModifiationsToMove(NoteItem oldSeries, NoteItem newSeries, Date lastRecurrenceId) {
        ArrayList<NoteItem> mods = new ArrayList<NoteItem>();
        RecurrenceExpander expander = new RecurrenceExpander();
        EventStamp newEvent = EventStamp.getStamp(newSeries);
        Calendar newEventCal = newEvent.getEventCalendar();
        
        Date newStartDate = newEvent.getStartDate();
        long delta = 0;
        
        if(!newStartDate.equals(lastRecurrenceId))
            delta = newStartDate.getTime() - lastRecurrenceId.getTime();
           
        // Find all modifications with a recurrenceId that is in the set of
        // recurrenceIds for the new series
        for(NoteItem mod: oldSeries.getModifications()) {
            EventExceptionStamp event = EventExceptionStamp.getStamp(mod);
            Date recurrenceId = event.getRecurrenceId();
            
            // Account for shift in startDate by calculating a new
            // recurrenceId based on the shift.
            if(delta!=0) {
                java.util.Date newRidTime =
                    new java.util.Date(recurrenceId.getTime() + delta);
                recurrenceId = (DateTime)
                    Dates.getInstance(newRidTime, recurrenceId);
            }
            
            // If modification matches an occurrence in the new series
            // then add it to the list
            if(expander.isOccurrence(newEventCal, recurrenceId)) {
                event.setRecurrenceId(recurrenceId);
                
                // If modification is the start of the series and there 
                // was a time change, then match up the startDate
                if(recurrenceId.equals(newStartDate) && delta!=0)
                    event.setStartDate(newStartDate);
                
                mods.add(mod);
            } 
        }
        
        return  mods;
    }
    
    
    
    
}