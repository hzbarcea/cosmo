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
package org.osaf.cosmo.eim.schema.event.alarm;

import java.text.ParseException;

import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.property.Trigger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.schema.BaseStampApplicator;
import org.osaf.cosmo.eim.schema.EimFieldValidator;
import org.osaf.cosmo.eim.schema.EimSchemaException;
import org.osaf.cosmo.eim.schema.EimValidationException;
import org.osaf.cosmo.eim.schema.EimValueConverter;
import org.osaf.cosmo.eim.schema.text.DurationFormat;
import org.osaf.cosmo.model.BaseEventStamp;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.Stamp;

/**
 * Applies display alarm EIM records to an EventStamp.
 *
 * @see EventStamp
 */
public class DisplayAlarmApplicator extends BaseStampApplicator
    implements DisplayAlarmConstants {
    private static final Log log =
        LogFactory.getLog(DisplayAlarmApplicator.class);

    /** */
    public DisplayAlarmApplicator(Item item) {
        super(PREFIX_DISPLAY_ALARM, NS_DISPLAY_ALARM, item);
        setStamp(BaseEventStamp.getStamp(item));
    }
    
    @Override
    protected void applyDeletion(EimRecord record) throws EimSchemaException {
        BaseEventStamp eventStamp = getEventStamp();    
        
        // Require event to continue
        if(eventStamp==null)
            throw new EimSchemaException("No alarm to delete");
        
        VAlarm alarm = eventStamp.getDisplayAlarm();
        
        // remove alarm
        if(alarm != null) {
            eventStamp.getEvent().getAlarms().remove(alarm);
        }
        
        // remove reminder on NoteItem
        applyDeletionNonEvent(record);
    }
    
    protected void applyDeletionNonEvent(EimRecord record) throws EimSchemaException {
        NoteItem note = (NoteItem) getItem();
        note.setReminderTime(null);
    }

    @Override
    public void applyRecord(EimRecord record) throws EimSchemaException {
        
        // If item is not an event, then process differently
        if(getEventStamp()==null) {
            applyRecordNonEvent(record);
            return;
        }
        
        // handle deletion
        if (record.isDeleted()) {
            applyDeletion(record);
            return;
        }
        
        BaseEventStamp eventStamp = getEventStamp();
        
        // create display alarm if it doesn't exist
        if(eventStamp.getDisplayAlarm()==null)
            eventStamp.creatDisplayAlarm();
            
        for (EimRecordField field : record.getFields()) {
            if(field.getName().equals(FIELD_DESCRIPTION)) {
                if(field.isMissing()) {
                    handleMissingAttribute("displayAlarmDescription");
                }
                else {
                    String value = EimFieldValidator.validateText(field, MAXLEN_DESCRIPTION);
                    eventStamp.setDisplayAlarmDescription(value);
                }
            }
            else if(field.getName().equals(FIELD_TRIGGER)) {
                if(field.isMissing()) {
                    handleMissingAttribute("displayAlarmTrigger");
                }
                else {
                    String value = EimFieldValidator.validateText(field, MAXLEN_TRIGGER);
                    Trigger newTrigger = EimValueConverter.toIcalTrigger(value);
                    eventStamp.setDisplayAlarmTrigger(newTrigger);
                }
            }
            else if (field.getName().equals(FIELD_DURATION)) {
                if(field.isMissing()) {
                    handleMissingAttribute("displayAlarmDuration");
                }
                else {
                    String value = EimFieldValidator.validateText(field, MAXLEN_DURATION);
                    try {
                        Dur dur = DurationFormat.getInstance().parse(value);
                        eventStamp.setDisplayAlarmDuration(dur);
                    } catch (ParseException e) {
                        throw new EimValidationException("Illegal duration", e);
                    }
                }
            }
            else if(field.getName().equals(FIELD_REPEAT)) {
                if(field.isMissing()) {
                    handleMissingAttribute("displayAlarmRepeat");
                }
                else {
                    Integer value = EimFieldValidator.validateInteger(field);
                    if(value!=null && value.intValue()==0)
                        value = null;
                    eventStamp.setDisplayAlarmRepeat(value);
                }
            }
            else
                log.warn("usupported eim field " + field.getName()
                        + " found in " + record.getNamespace());
        }
    }
    
    public void applyRecordNonEvent(EimRecord record) throws EimSchemaException {
        
        // handle deletion
        if (record.isDeleted()) {
            applyDeletionNonEvent(record);
            return;
        }
        
        NoteItem note = (NoteItem) getItem();
            
        for (EimRecordField field : record.getFields()) {
            if(field.getName().equals(FIELD_DESCRIPTION)) {
                // ignore, don't support
            }
            else if(field.getName().equals(FIELD_TRIGGER)) {
                if(field.isMissing()) {
                    handleMissingAttribute("reminderTime");
                }
                else {
                    String value = EimFieldValidator.validateText(field, MAXLEN_TRIGGER);
                    Trigger trigger = EimValueConverter.toIcalTrigger(value);
                    
                    // for non-events, the trigger has to be absolute
                    if(trigger.getDuration()!=null)
                        throw new EimSchemaException("non-absolute triggers not supported on non-events");
                    note.setReminderTime(trigger.getDate());
                }
            }
            else if (field.getName().equals(FIELD_DURATION)) {
                // ignore, don't support
            }
            else if(field.getName().equals(FIELD_REPEAT)) {
                // ignore, don't support
            }
            else
                log.warn("usupported eim field " + field.getName()
                        + " found in " + record.getNamespace());
        }
    }
    
    @Override
    protected void applyField(EimRecordField field) throws EimSchemaException {
        // do nothing beause we override applyRecord()
    }
    
    @Override
    protected Stamp createStamp(EimRecord record) throws EimSchemaException {
        // do nothing as the stamp should already be created
        return null;
    }
        
    
    private BaseEventStamp getEventStamp() {
        return BaseEventStamp.getStamp(getItem());
    }
    
    @Override
    protected Stamp getParentStamp() {
        NoteItem noteMod = (NoteItem) getItem();
        NoteItem parentNote = noteMod.getModifies();
        
        if(parentNote!=null)
            return parentNote.getStamp(BaseEventStamp.class);
        else
            return null;
    }
}
