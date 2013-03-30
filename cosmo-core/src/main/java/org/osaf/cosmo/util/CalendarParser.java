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
/**
 * 
 */
package org.osaf.cosmo.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

/**
 * Utility for managing a per-thread singleton for
 * CalendarBuilder.  Constructing a CalendarBuilder every
 * time one is needed can be expensive if a lot of 
 * time zones are involved in the calendar data.
 */
public class CalendarParser {

    /**
     * Parse icalendar data from Reader into Calendar object.
     * @param reader icalendar data reader
     * @return Calendar object
     */
    public static Calendar parseCalendar(Reader reader) throws ParserException, IOException {
        return reader == null ? null : CalendarBuilderDispenser.getCalendarBuilder(true).build(reader);
    }

    /**
     * Parse icalendar string into Calendar object.
     * @param calendar icalendar string
     * @return Calendar object
     */
    public static Calendar parseCalendar(String calendar) throws ParserException, IOException {
        return calendar == null ? null : parseCalendar(new StringReader(calendar));
    }
    
    /**
     * Parse icalendar data from InputStream
     * @param is icalendar data inputstream
     * @return Calendar object
     * @throws Exception
     */
    public static Calendar parseCalendar(InputStream is) throws ParserException, IOException {
        return parseCalendar(new InputStreamReader(is, Charset.forName("UTF-8")));
    }
    
    /**
     * Parse icalendar data from byte[] into Calendar object.
     * @param content icalendar data
     * @return Calendar object
     * @throws Exception
     */
    public static Calendar parseCalendar(byte[] content) throws ParserException, IOException {
        // TODO: Better handle charset instead of hardcode to UTF-8
        return parseCalendar(new ByteArrayInputStream(content));
    }

}
