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
package org.osaf.cosmo.dav.caldav.property;

import org.osaf.cosmo.dav.caldav.CaldavConstants;
import org.osaf.cosmo.dav.property.StandardDavProperty;

/**
 * Represents the <code>CALDAV:calendar-description</code> property.
 */
public class CalendarDescription extends StandardDavProperty
    implements CaldavConstants {

    public CalendarDescription(String description,
                               String language) {
        super(CALENDARDESCRIPTION, description, language, true);
    }
}
