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
package org.osaf.cosmo.dav.impl;

import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.DavResourceLocator;
import org.osaf.cosmo.model.FreeBusyItem;

/**
 * Extends <code>DavCalendarResource</code> to adapt the Cosmo
 * <code>FreeBusyItem</code> to the DAV resource model.
 *
 * This class does not define any live properties.
 *
 * @see DavFile
 */
public class DavFreeBusy extends DavCalendarResource {
    private static final Log log = LogFactory.getLog(DavFreeBusy.class);
    
    /** */
    public DavFreeBusy(DavResourceLocator locator,
                      DavResourceFactory factory)
        throws DavException {
        this(new FreeBusyItem(), locator, factory);
    }

    /** */
    public DavFreeBusy(FreeBusyItem item,
                      DavResourceLocator locator,
                      DavResourceFactory factory)
        throws DavException {
        super(item, locator, factory);
    }

    // our methods

    /**
     * <p>
     * Exports the item as a calendar object containing a single VFREEBUSY
     * </p>
     */
    public Calendar getCalendar() {
        FreeBusyItem freeBusy = (FreeBusyItem) getItem();
        return freeBusy.getFreeBusyCalendar();
    }

    /**
     * <p>
     * Imports a calendar object containing a VFREEBUSY. 
     * </p>
     */
    public void setCalendar(Calendar cal)
        throws DavException {
        FreeBusyItem freeBusy = (FreeBusyItem) getItem();
        
        freeBusy.setFreeBusyCalendar(cal);
    }
}
