/*
 * Copyright 2006-2007 Open Source Applications Foundation
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
package org.osaf.cosmo.webcal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.model.Calendar;

import org.apache.abdera.util.EntityTag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.calendar.EntityConverter;
import org.osaf.cosmo.icalendar.ICalendarClientFilterManager;
import org.osaf.cosmo.icalendar.ICalendarConstants;
import org.osaf.cosmo.icalendar.ICalendarOutputter;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.StampUtils;
import org.osaf.cosmo.security.CosmoSecurityException;
import org.osaf.cosmo.server.CollectionPath;
import org.osaf.cosmo.service.ContentService;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A simple servlet that provides a "webcal"-style iCalendar
 * representation of a calendar collection.
 */
public class WebcalServlet extends HttpServlet implements ICalendarConstants {
    private static final Log log = LogFactory.getLog(WebcalServlet.class);
    private static final String BEAN_CONTENT_SERVICE = "contentService";
    private static final String BEAN_CLIENT_FILTER_MANAGER = "iCalendarClientFilterManager";

    
    private WebApplicationContext wac;
    private ContentService contentService;
    private ICalendarClientFilterManager clientFilterManager;
    private EntityConverter entityConverter = new EntityConverter(null);

    // HttpServlet methods

    /**
     * Handles GET requests for calendar collections. Returns a 200
     * <code>text/calendar</code> response containing an iCalendar
     * representation of all of the calendar items within the
     * collection.
     *
     * Returns 404 if the request's path info does not specify a
     * collection path or if the identified collection is not found.
     *
     * Returns 405 if the item with the identified uid is not a
     * calendar collection.
     */
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
        throws ServletException, IOException {
        if (log.isDebugEnabled())
            log.debug("handling GET for " + req.getPathInfo());

        // requests will usually come in with the collection's display
        // name appended to the collection path so that clients will
        // save the file with that name
        CollectionPath cp = CollectionPath.parse(req.getPathInfo(), true);
        if (cp == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Item item;
        
        try {
            item = contentService.findItemByUid(cp.getUid());
        } 
        // handle security errors by returing 403
        catch (CosmoSecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            return;
        }
        
        if (item == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (! (item instanceof CollectionItem)) {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                           "Requested item not a collection");
            return;
        }

        CollectionItem collection = (CollectionItem) item;
        if (StampUtils.getCalendarCollectionStamp(collection) == null) {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                           "Requested item not a calendar collection");
            return;
        }

        EntityTag etag = new EntityTag(collection.getEntityTag());
        
        // set ETag
        resp.setHeader("ETag", etag.toString());
        
        // check for If-None-Match
        EntityTag[] requestEtags = getIfNoneMatch(req);
        if (requestEtags!=null && requestEtags.length != 0) {
            if (EntityTag.matchesAny(etag, requestEtags)) {
                resp.setStatus(304);
                return;
            }
        }
        
        // check for If-Modified-Since
        long since = req.getDateHeader("If-Modified-Since");
        
        // If present and if collection's modified date not more recent,
        // return 304 not modified
        if (since != -1) {
            long lastModified = collection.getModifiedDate().getTime() / 1000 * 1000;

            if (lastModified <= since) {
                resp.setStatus(304);
                return;
            }
        }
            
        // set Last-Modified
        resp.setDateHeader("Last-Modified", collection.getModifiedDate().getTime());
        
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(ICALENDAR_MEDIA_TYPE);
        resp.setCharacterEncoding("UTF-8");

        // send Content-Disposition to provide another hint to clients
        // on how to save and name the downloaded file
        String filename =
            collection.getDisplayName() + "." + ICALENDAR_FILE_EXTENSION;
        resp.setHeader("Content-Disposition",
                       "attachment; filename=\"" + filename + "\"");

        // get icalendar
        Calendar calendar = entityConverter.convertCollection(collection);
        
        // Filter if necessary so we play nicely with clients
        // that don't adhere to spec
        if(clientFilterManager!=null)
            clientFilterManager.filterCalendar(calendar);
        
        // spool
        ICalendarOutputter.output(calendar, resp.getOutputStream());
    }

    protected EntityTag[] getIfNoneMatch(HttpServletRequest request) {
        try {
            return EntityTag.parseTags(request.getHeader("If-None-Match"));
        } catch (RuntimeException e) {
           return null;
        }
    }
    
    // GenericServlet methods

    /**
     * Loads the servlet context's <code>WebApplicationContext</code>
     * and wires up dependencies. If no
     * <code>WebApplicationContext</code> is found, dependencies must
     * be set manually (useful for testing).
     *
     * @throws ServletException if required dependencies are not found
     */
    public void init() throws ServletException {
        super.init();

        wac = WebApplicationContextUtils.
            getWebApplicationContext(getServletContext());

        if (wac != null) {
            if (contentService == null)
                contentService = (ContentService)
                    getBean(BEAN_CONTENT_SERVICE, ContentService.class);
            if (clientFilterManager == null)
                clientFilterManager = (ICalendarClientFilterManager) getBean(
                        BEAN_CLIENT_FILTER_MANAGER,
                        ICalendarClientFilterManager.class);
        }
        
        if (contentService == null)
            throw new ServletException("content service must not be null");
    }

    // our methods

    /** */
    public ContentService getContentService() {
        return contentService;
    }

    /** */
    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }
    
    public ICalendarClientFilterManager getClientFilterManager() {
        return clientFilterManager;
    }

    public void setClientFilterManager(
            ICalendarClientFilterManager clientFilterManager) {
        this.clientFilterManager = clientFilterManager;
    }

    // private methods

    private Object getBean(String name, Class clazz)
        throws ServletException {
        try {
            return wac.getBean(name, clazz);
        } catch (BeansException e) {
            throw new ServletException("Error retrieving bean " + name + " of type " + clazz + " from web application context", e);
        }
    }
    
}
