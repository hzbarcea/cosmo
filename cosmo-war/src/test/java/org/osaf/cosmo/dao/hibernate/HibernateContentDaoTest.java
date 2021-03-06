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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.property.ProdId;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DOMWriter;
import org.hibernate.validator.InvalidStateException;
import org.osaf.cosmo.calendar.util.CalendarUtils;
import org.osaf.cosmo.dao.UserDao;
import org.osaf.cosmo.model.Attribute;
import org.osaf.cosmo.model.AvailabilityItem;
import org.osaf.cosmo.model.BooleanAttribute;
import org.osaf.cosmo.model.CalendarAttribute;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.DecimalAttribute;
import org.osaf.cosmo.model.DictionaryAttribute;
import org.osaf.cosmo.model.DuplicateItemNameException;
import org.osaf.cosmo.model.FileItem;
import org.osaf.cosmo.model.FreeBusyItem;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.ICalendarAttribute;
import org.osaf.cosmo.model.IcalUidInUseException;
import org.osaf.cosmo.model.IntegerAttribute;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ItemNotFoundException;
import org.osaf.cosmo.model.ItemTombstone;
import org.osaf.cosmo.model.ModelValidationException;
import org.osaf.cosmo.model.MultiValueStringAttribute;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.TimestampAttribute;
import org.osaf.cosmo.model.Tombstone;
import org.osaf.cosmo.model.TriageStatus;
import org.osaf.cosmo.model.TriageStatusUtil;
import org.osaf.cosmo.model.UidInUseException;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.model.XmlAttribute;
import org.osaf.cosmo.model.hibernate.HibAvailabilityItem;
import org.osaf.cosmo.model.hibernate.HibBooleanAttribute;
import org.osaf.cosmo.model.hibernate.HibCalendarAttribute;
import org.osaf.cosmo.model.hibernate.HibCollectionItem;
import org.osaf.cosmo.model.hibernate.HibContentItem;
import org.osaf.cosmo.model.hibernate.HibDecimalAttribute;
import org.osaf.cosmo.model.hibernate.HibDictionaryAttribute;
import org.osaf.cosmo.model.hibernate.HibFileItem;
import org.osaf.cosmo.model.hibernate.HibFreeBusyItem;
import org.osaf.cosmo.model.hibernate.HibICalendarAttribute;
import org.osaf.cosmo.model.hibernate.HibIntegerAttribute;
import org.osaf.cosmo.model.hibernate.HibItem;
import org.osaf.cosmo.model.hibernate.HibMultiValueStringAttribute;
import org.osaf.cosmo.model.hibernate.HibNoteItem;
import org.osaf.cosmo.model.hibernate.HibQName;
import org.osaf.cosmo.model.hibernate.HibStringAttribute;
import org.osaf.cosmo.model.hibernate.HibTicket;
import org.osaf.cosmo.model.hibernate.HibTimestampAttribute;
import org.osaf.cosmo.model.hibernate.HibTriageStatus;
import org.osaf.cosmo.model.hibernate.HibXmlAttribute;
import org.osaf.cosmo.xml.DomWriter;

public class HibernateContentDaoTest extends AbstractHibernateDaoTestCase {

    protected UserDaoImpl userDao = null;

    protected ContentDaoImpl contentDao = null;

    public HibernateContentDaoTest() {
        super();
    }

    public void testContentDaoCreateContent() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(root, item);

        Assert.assertTrue(getHibItem(newItem).getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        helper.verifyItem(newItem, queryItem);
    }
    
    public void testContentDaoLoadChildren() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(root, item);

        Assert.assertTrue(getHibItem(newItem).getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        Set<ContentItem> children = contentDao.loadChildren(root, null);
        Assert.assertEquals(1, children.size());
        
        children = contentDao.loadChildren(root, newItem.getModifiedDate());
        Assert.assertEquals(0, children.size());
        
        children = contentDao.loadChildren(root, new Date(newItem.getModifiedDate().getTime() -1));
        Assert.assertEquals(1, children.size());
    }
    
    public void testContentDaoCreateContentDuplicateUid() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item1 = generateTestContent();
        item1.setName("test");
        item1.setUid("uid");

        contentDao.createContent(root, item1);
        
        ContentItem item2 = generateTestContent();
        item2.setName("test2");
        item2.setUid("uid");

        try {
            contentDao.createContent(root, item2);
            clearSession();
            Assert.fail("able to create duplicate uid");
        } catch (UidInUseException e) {
        }
    }

    public void testContentDaoCreateNoteDuplicateIcalUid() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        NoteItem note1 = generateTestNote("note1", "testuser");
        note1.setIcalUid("icaluid");

        contentDao.createContent(root, note1);
        
        NoteItem note2 = generateTestNote("note2", "testuser");
        note2.setIcalUid("icaluid");
         

        try {
            contentDao.createContent(root, note2);
            Assert.fail("able to create duplicate icaluid");
        } catch (IcalUidInUseException e) {}
    
    }

    public void testContentDaoInvalidContentEmptyName() throws Exception {
        
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);
        ContentItem item = generateTestContent();
        item.setName("");

        try {
            contentDao.createContent(root, item);
            Assert.fail("able to create invalid content.");
        } catch (InvalidStateException e) {
            Assert.assertEquals("name", e.getInvalidValues()[0].getPropertyName());
        }
    }

    public void testContentAttributes() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        IntegerAttribute ia = new HibIntegerAttribute(new HibQName("intattribute"), new Long(22));
        item.addAttribute(ia);
        BooleanAttribute ba = new HibBooleanAttribute(new HibQName("booleanattribute"), Boolean.TRUE);
        item.addAttribute(ba);
        
        DecimalAttribute decAttr = 
            new HibDecimalAttribute(new HibQName("decimalattribute"),new BigDecimal("1.234567"));
        item.addAttribute(decAttr);
        
        // TODO: figure out db date type is handled because i'm seeing
        // issues with accuracy
        // item.addAttribute(new DateAttribute("dateattribute", new Date()));

        HashSet<String> values = new HashSet<String>();
        values.add("value1");
        values.add("value2");
        MultiValueStringAttribute mvs = new HibMultiValueStringAttribute(new HibQName("multistringattribute"), values);
        item.addAttribute(mvs);

        HashMap<String, String> dictionary = new HashMap<String, String>();
        dictionary.put("key1", "value1");
        dictionary.put("key2", "value2");
        DictionaryAttribute da = new HibDictionaryAttribute(new HibQName("dictionaryattribute"), dictionary);
        item.addAttribute(da);

        ContentItem newItem = contentDao.createContent(root, item);

        Assert.assertTrue(getHibItem(newItem).getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        Attribute attr = queryItem.getAttribute(new HibQName("decimalattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof DecimalAttribute);
        Assert.assertEquals(attr.getValue().toString(),"1.234567");
        
        Set<String> querySet = (Set<String>) queryItem
                .getAttributeValue("multistringattribute");
        Assert.assertTrue(querySet.contains("value1"));
        Assert.assertTrue(querySet.contains("value2"));

        Map<String, String> queryDictionary = (Map<String, String>) queryItem
                .getAttributeValue("dictionaryattribute");
        Assert.assertEquals("value1", queryDictionary.get("key1"));
        Assert.assertEquals("value2", queryDictionary.get("key2"));

        Attribute custom = queryItem.getAttribute("customattribute");
        Assert.assertEquals("customattributevalue", custom.getValue());

        helper.verifyItem(newItem, queryItem);

        // set attribute value to null
        custom.setValue(null);

        querySet.add("value3");
        queryDictionary.put("key3", "value3");

        queryItem.removeAttribute("intattribute");

        contentDao.updateContent(queryItem);

        clearSession();

        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        querySet = (Set) queryItem.getAttributeValue("multistringattribute");
        queryDictionary = (Map) queryItem
                .getAttributeValue("dictionaryattribute");
        Attribute queryAttribute = queryItem.getAttribute("customattribute");
       
        Assert.assertTrue(querySet.contains("value3"));
        Assert.assertEquals("value3", queryDictionary.get("key3"));
        Assert.assertNotNull(queryAttribute);
        Assert.assertNull(queryAttribute.getValue());
        Assert.assertNull(queryItem.getAttribute("intattribute"));
    }
    
    public void testCalendarAttribute() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        
        CalendarAttribute calAttr = 
            new HibCalendarAttribute(new HibQName("calendarattribute"), "2002-10-10T00:00:00+05:00"); 
        item.addAttribute(calAttr);
        
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        Attribute attr = queryItem.getAttribute(new HibQName("calendarattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof CalendarAttribute);
        
        Calendar cal = (Calendar) attr.getValue();
        Assert.assertEquals(cal.getTimeZone().getID(), "GMT+05:00");
        Assert.assertTrue(cal.equals(calAttr.getValue()));
        
        attr.setValue("2003-10-10T00:00:00+02:00");

        contentDao.updateContent(queryItem);

        clearSession();

        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Attribute queryAttr = queryItem.getAttribute(new HibQName("calendarattribute"));
        Assert.assertNotNull(queryAttr);
        Assert.assertTrue(queryAttr instanceof CalendarAttribute);
        
        cal = (Calendar) queryAttr.getValue();
        Assert.assertEquals(cal.getTimeZone().getID(), "GMT+02:00");
        Assert.assertTrue(cal.equals(attr.getValue()));
    }
    
    public void testTimestampAttribute() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        Date dateVal = new Date();
        TimestampAttribute tsAttr = 
            new HibTimestampAttribute(new HibQName("timestampattribute"), dateVal); 
        item.addAttribute(tsAttr);
        
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        Attribute attr = queryItem.getAttribute(new HibQName("timestampattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof TimestampAttribute);
        
        Date val = (Date) attr.getValue();
        Assert.assertTrue(dateVal.equals(val));
        
        dateVal.setTime(dateVal.getTime() + 101);
        attr.setValue(dateVal);

        contentDao.updateContent(queryItem);

        clearSession();

        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Attribute queryAttr = queryItem.getAttribute(new HibQName("timestampattribute"));
        Assert.assertNotNull(queryAttr);
        Assert.assertTrue(queryAttr instanceof TimestampAttribute);
        
        val = (Date) queryAttr.getValue();
        Assert.assertTrue(dateVal.equals(val));
    }
    
    public void testXmlAttribute() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        
        org.w3c.dom.Element testElement = createTestElement();
        org.w3c.dom.Element testElement2 = createTestElement();
        
        testElement2.setAttribute("foo", "bar");
        
        Assert.assertFalse(testElement.isEqualNode(testElement2));
        
        XmlAttribute xmlAttr = 
            new HibXmlAttribute(new HibQName("xmlattribute"), testElement ); 
        item.addAttribute(xmlAttr);
        
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        Attribute attr = queryItem.getAttribute(new HibQName("xmlattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof XmlAttribute);
        
        org.w3c.dom.Element element = (org.w3c.dom.Element) attr.getValue();
        
        Assert.assertEquals(DomWriter.write(testElement),DomWriter.write(element));

        Date modifyDate = attr.getModifiedDate();
        
        // Sleep a couple millis to make sure modifyDate doesn't change
        Thread.sleep(2);
        
        contentDao.updateContent(queryItem);

        clearSession();
        
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        attr = queryItem.getAttribute(new HibQName("xmlattribute"));
        
        // Attribute shouldn't have been updated
        Assert.assertEquals(modifyDate, attr.getModifiedDate());
        
        attr.setValue(testElement2);

        // Sleep a couple millis to make sure modifyDate doesn't change
        Thread.sleep(2);
        modifyDate = attr.getModifiedDate();
        
        contentDao.updateContent(queryItem);

        clearSession();
        
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        attr = queryItem.getAttribute(new HibQName("xmlattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof XmlAttribute);
        // Attribute should have been updated
        Assert.assertTrue(modifyDate.before(attr.getModifiedDate()));
        
        element = (org.w3c.dom.Element) attr.getValue();
        
        Assert.assertEquals(DomWriter.write(testElement2),DomWriter.write(element));
    }
    
    public void testICalendarAttribute() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
       
        ICalendarAttribute icalAttr = new HibICalendarAttribute(); 
        icalAttr.setQName(new HibQName("icalattribute"));
        icalAttr.setValue(helper.getInputStream("testdata/vjournal.ics"));
        item.addAttribute(icalAttr);
        
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        Attribute attr = queryItem.getAttribute(new HibQName("icalattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof ICalendarAttribute);
        
        net.fortuna.ical4j.model.Calendar calendar = (net.fortuna.ical4j.model.Calendar) attr.getValue();
        Assert.assertNotNull(calendar);
        
        net.fortuna.ical4j.model.Calendar expected = CalendarUtils.parseCalendar(helper.getInputStream("testdata/vjournal.ics"));
        
        Assert.assertEquals(expected.toString(),calendar.toString());
        
        calendar.getProperties().add(new ProdId("blah"));
        
        contentDao.updateContent(queryItem);
        
        clearSession();
        
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        
        ICalendarAttribute ica = (ICalendarAttribute) queryItem.getAttribute(new HibQName("icalattribute"));
        Assert.assertFalse(expected.toString().equals(ica.getValue().toString()));
    }

    public void testCreateDuplicateRootItem() throws Exception {
        User testuser = getUser(userDao, "testuser");
        try {
            contentDao.createRootItem(testuser);
            Assert.fail("able to create duplicate root item");
        } catch (RuntimeException re) {
        }
    }

    public void testFindItem() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");

        CollectionItem root = (CollectionItem) contentDao
                .getRootItem(testuser2);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(getUser(userDao, "testuser2"));

        a = contentDao.createCollection(root, a);

        clearSession();

        Item queryItem = contentDao.findItemByUid(a.getUid());
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof CollectionItem);

        queryItem = contentDao.findItemByPath("/testuser2/a");
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof CollectionItem);

        ContentItem item = generateTestContent();
        
        a = (CollectionItem) contentDao.findItemByUid(a.getUid());
        item = contentDao.createContent(a, item);

        clearSession();

        queryItem = contentDao.findItemByPath("/testuser2/a/test");
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof ContentItem);

        clearSession();

        queryItem = contentDao.findItemParentByPath("/testuser2/a/test");
        Assert.assertNotNull(queryItem);
        Assert.assertEquals(a.getUid(), queryItem.getUid());
    }

    public void testContentDaoUpdateContent() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        FileItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);
        Date newItemModifyDate = newItem.getModifiedDate();
        
        clearSession();

        HibFileItem queryItem = (HibFileItem) contentDao.findItemByUid(newItem.getUid());

        helper.verifyItem(newItem, queryItem);
        Assert.assertEquals(0, queryItem.getVersion().intValue());

        queryItem.setName("test2");
        queryItem.setDisplayName("this is a test item2");
        queryItem.removeAttribute("customattribute");
        queryItem.setContentLanguage("es");
        queryItem.setContent(helper.getBytes("testdata/testdata2.txt"));

        // Make sure modified date changes
        Thread.sleep(1000);
        
        queryItem = (HibFileItem) contentDao.updateContent(queryItem);
        
        clearSession();
        Thread.sleep(200);
        HibContentItem queryItem2 = (HibContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertTrue(queryItem2.getVersion().intValue() > 0);
        
        helper.verifyItem(queryItem, queryItem2);

        Assert.assertTrue(newItemModifyDate.before(
                queryItem2.getModifiedDate()));
    }

    public void testContentDaoDeleteContent() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        helper.verifyItem(newItem, queryItem);

        contentDao.removeContent(queryItem);

        clearSession();

        queryItem = (ContentItem) contentDao.findItemByUid(queryItem.getUid());
        Assert.assertNull(queryItem);
        
        clearSession();
        
        root = (CollectionItem) contentDao.getRootItem(user);
        Assert.assertTrue(root.getChildren().size()==0);
        
    }
    
    public void testContentDaoDeleteUserContent() throws Exception {
        User user1 = getUser(userDao, "testuser1");
        User user2 = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user1);

        // Create test content, with owner of user2
        ContentItem item = generateTestContent();
        item.setOwner(user2);
        
        // create content in user1's home collection
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        user1 = getUser(userDao, "testuser1");
        user2 = getUser(userDao, "testuser2");
       
        // remove user2's content, which should include the item created
        // in user1's home collections
        contentDao.removeUserContent(user2);
        
        root = (CollectionItem) contentDao.getRootItem(user1);
        Assert.assertEquals(0, root.getChildren().size());
    }

    public void testDeleteContentByPath() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        helper.verifyItem(newItem, queryItem);

        contentDao.removeItemByPath("/testuser/test");

        clearSession();

        queryItem = (ContentItem) contentDao.findItemByUid(queryItem.getUid());
        Assert.assertNull(queryItem);
    }

    public void testDeleteContentByUid() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        helper.verifyItem(newItem, queryItem);

        contentDao.removeItemByUid(queryItem.getUid());

        clearSession();

        queryItem = (ContentItem) contentDao.findItemByUid(queryItem.getUid());
        Assert.assertNull(queryItem);
    }
    
    public void testTombstoneDeleteContent() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        helper.verifyItem(newItem, queryItem);
        
        Assert.assertTrue(((HibItem)queryItem).getVersion().equals(0));

        contentDao.removeContent(queryItem);

        clearSession();

        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertNull(queryItem);
        
        root = (CollectionItem) contentDao.getRootItem(user);
        Assert.assertEquals(root.getTombstones().size(), 1);
        
        Tombstone ts = root.getTombstones().iterator().next();
        
        Assert.assertTrue(ts instanceof ItemTombstone);
        Assert.assertEquals(((ItemTombstone) ts).getItemUid(), newItem.getUid());
        
        item = generateTestContent();
        item.setUid(newItem.getUid());
        
        contentDao.createContent(root, item);

        clearSession();
        
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        
        Assert.assertNotNull(queryItem);
        
        root = (CollectionItem) contentDao.getRootItem(user);
        Assert.assertEquals(root.getTombstones().size(), 0);
    }

    public void testContentDaoCreateCollection() throws Exception {
        User user = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(user);
        a.setHue(new Long(1));

        a = contentDao.createCollection(root, a);

        Assert.assertTrue(getHibItem(a).getId() > -1);
        Assert.assertNotNull(a.getUid());

        clearSession();

        CollectionItem queryItem = (CollectionItem) contentDao.findItemByUid(a.getUid());
        Assert.assertEquals(new Long(1), queryItem.getHue());
        helper.verifyItem(a, queryItem);
    }

    public void testContentDaoUpdateCollection() throws Exception {
        User user = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);

        clearSession();

        Assert.assertTrue(getHibItem(a).getId() > -1);
        Assert.assertNotNull(a.getUid());

        CollectionItem queryItem = (CollectionItem) contentDao.findItemByUid(a.getUid());
        helper.verifyItem(a, queryItem);

        queryItem.setName("b");
        contentDao.updateCollection(queryItem);

        clearSession();

        queryItem = (CollectionItem) contentDao.findItemByUid(a.getUid());
        Assert.assertEquals("b", queryItem.getName());
    }
    
    public void testContentDaoUpdateCollectionTimestamp() throws Exception {
        User user = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);
        Integer ver = ((HibItem) a).getVersion();
        Date timestamp = a.getModifiedDate();
        
        clearSession();
        Thread.sleep(1);
        
        a = contentDao.updateCollectionTimestamp(a);
        Assert.assertTrue(((HibItem) a).getVersion()==ver + 1);
        Assert.assertTrue(timestamp.before(a.getModifiedDate()));
    }

    public void testContentDaoDeleteCollection() throws Exception {
        User user = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);

        clearSession();

        CollectionItem queryItem = (CollectionItem) contentDao.findItemByUid(a.getUid());
        Assert.assertNotNull(queryItem);

        contentDao.removeCollection(queryItem);

        clearSession();

        queryItem = (CollectionItem) contentDao.findItemByUid(a.getUid());
        Assert.assertNull(queryItem);
    }

    public void testContentDaoAdvanced() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao
                .getRootItem(testuser2);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(getUser(userDao, "testuser2"));

        a = contentDao.createCollection(root, a);

        CollectionItem b = new HibCollectionItem();
        b.setName("b");
        b.setOwner(getUser(userDao, "testuser2"));

        b = contentDao.createCollection(a, b);

        ContentItem c = generateTestContent("c", "testuser2");

        c = contentDao.createContent(b, c);

        ContentItem d = generateTestContent("d", "testuser2");

        d = contentDao.createContent(a, d);

        clearSession();

        a = (CollectionItem) contentDao.findItemByUid(a.getUid());
        b = (CollectionItem) contentDao.findItemByUid(b.getUid());
        c = (ContentItem) contentDao.findItemByUid(c.getUid());
        d = (ContentItem) contentDao.findItemByUid(d.getUid());
        root = contentDao.getRootItem(testuser2);

        Assert.assertNotNull(a);
        Assert.assertNotNull(b);
        Assert.assertNotNull(d);
        Assert.assertNotNull(root);

        // test children
        Collection children = a.getChildren();
        Assert.assertEquals(2, children.size());
        verifyContains(children, b);
        verifyContains(children, d);

        children = root.getChildren();
        Assert.assertEquals(1, children.size());
        verifyContains(children, a);

        // test get by path
        ContentItem queryC = (ContentItem) contentDao.findItemByPath("/testuser2/a/b/c");
        Assert.assertNotNull(queryC);
        helper.verifyInputStream(
                helper.getInputStream("testdata/testdata1.txt"), ((FileItem) queryC)
                        .getContent());
        Assert.assertEquals("c", queryC.getName());

        // test get path/uid abstract
        Item queryItem = contentDao.findItemByPath("/testuser2/a/b/c");
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof ContentItem);

        queryItem = contentDao.findItemByUid(a.getUid());
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof CollectionItem);

        // test delete
        contentDao.removeContent(c);
        queryC = (ContentItem) contentDao.findItemByUid(c.getUid());
        Assert.assertNull(queryC);

        contentDao.removeCollection(a);

        CollectionItem queryA = (CollectionItem) contentDao.findItemByUid(a.getUid());
        Assert.assertNull(queryA);

        ContentItem queryD = (ContentItem) contentDao.findItemByUid(d.getUid());
        Assert.assertNull(queryD);
    }

  
    public void testHomeCollection() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");
        HomeCollectionItem root = contentDao.getRootItem(testuser2);

        Assert.assertNotNull(root);
        root.setName("alsfjal;skfjasd");
        Assert.assertEquals(root.getName(), "testuser2");

    }

    public void testItemDaoMove() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao
                .getRootItem(testuser2);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(getUser(userDao, "testuser2"));

        a = contentDao.createCollection(root, a);

        CollectionItem b = new HibCollectionItem();
        b.setName("b");
        b.setOwner(getUser(userDao, "testuser2"));

        b = contentDao.createCollection(a, b);

        CollectionItem c = new HibCollectionItem();
        c.setName("c");
        c.setOwner(getUser(userDao, "testuser2"));

        c = contentDao.createCollection(b, c);

        ContentItem d = generateTestContent("d", "testuser2");

        d = contentDao.createContent(c, d);

        CollectionItem e = new HibCollectionItem();
        e.setName("e");
        e.setOwner(getUser(userDao, "testuser2"));

        e = contentDao.createCollection(a, e);

        clearSession();

        root = (CollectionItem) contentDao.getRootItem(testuser2);
        e = (CollectionItem) contentDao.findItemByUid(e.getUid());
        b = (CollectionItem) contentDao.findItemByUid(b.getUid());

        // verify can't move root collection
        try {
            contentDao.moveItem("/testuser2", "/testuser2/a/blah");
            Assert.fail("able to move root collection");
        } catch (IllegalArgumentException iae) {
        }

        // verify can't move to root collection
        try {
            contentDao.moveItem("/testuser2/a/e", "/testuser2");
            Assert.fail("able to move to root collection");
        } catch (ItemNotFoundException infe) {
        }

        // verify can't create loop
        try {
            contentDao.moveItem("/testuser2/a/b", "/testuser2/a/b/c/new");
            Assert.fail("able to create loop");
        } catch (ModelValidationException iae) {
        }

        clearSession();

        // verify that move works
        b = (CollectionItem) contentDao.findItemByPath("/testuser2/a/b");

        contentDao.moveItem("/testuser2/a/b", "/testuser2/a/e/b");

        clearSession();

        CollectionItem queryCollection = (CollectionItem) contentDao
                .findItemByPath("/testuser2/a/e/b");
        Assert.assertNotNull(queryCollection);

        contentDao.moveItem("/testuser2/a/e/b", "/testuser2/a/e/bnew");

        clearSession();
        queryCollection = (CollectionItem) contentDao
                .findItemByPath("/testuser2/a/e/bnew");
        Assert.assertNotNull(queryCollection);

        Item queryItem = contentDao.findItemByPath("/testuser2/a/e/bnew/c/d");
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof ContentItem);
    }

    public void testItemDaoCopy() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao
                .getRootItem(testuser2);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(getUser(userDao, "testuser2"));

        a = contentDao.createCollection(root, a);

        CollectionItem b = new HibCollectionItem();
        b.setName("b");
        b.setOwner(getUser(userDao, "testuser2"));

        b = contentDao.createCollection(a, b);

        CollectionItem c = new HibCollectionItem();
        c.setName("c");
        c.setOwner(getUser(userDao, "testuser2"));

        c = contentDao.createCollection(b, c);

        ContentItem d = generateTestContent("d", "testuser2");

        d = contentDao.createContent(c, d);

        CollectionItem e = new HibCollectionItem();
        e.setName("e");
        e.setOwner(getUser(userDao, "testuser2"));

        e = contentDao.createCollection(a, e);

        clearSession();

        root = (CollectionItem) contentDao.getRootItem(testuser2);
        e = (CollectionItem) contentDao.findItemByUid(e.getUid());
        b = (CollectionItem) contentDao.findItemByUid(b.getUid());

        // verify can't copy root collection
        try {
            contentDao.copyItem(root, "/testuser2/a/blah", true);
            Assert.fail("able to copy root collection");
        } catch (IllegalArgumentException iae) {
        }

        // verify can't move to root collection
        try {
            contentDao.copyItem(e, "/testuser2", true);
            Assert.fail("able to move to root collection");
        } catch (ItemNotFoundException infe) {
        }

        // verify can't create loop
        try {
            contentDao.copyItem(b, "/testuser2/a/b/c/new", true);
            Assert.fail("able to create loop");
        } catch (ModelValidationException iae) {
        }

        clearSession();

        // verify that copy works
        b = (CollectionItem) contentDao.findItemByPath("/testuser2/a/b");

        contentDao.copyItem(b, "/testuser2/a/e/bcopy", true);

        clearSession();

        CollectionItem queryCollection = (CollectionItem) contentDao
                .findItemByPath("/testuser2/a/e/bcopy");
        Assert.assertNotNull(queryCollection);

        queryCollection = (CollectionItem) contentDao
                .findItemByPath("/testuser2/a/e/bcopy/c");
        Assert.assertNotNull(queryCollection);

        d = (ContentItem) contentDao.findItemByUid(d.getUid());
        ContentItem dcopy = (ContentItem) contentDao
                .findItemByPath("/testuser2/a/e/bcopy/c/d");
        Assert.assertNotNull(dcopy);
        Assert.assertEquals(d.getName(), dcopy.getName());
        Assert.assertNotSame(d.getUid(), dcopy.getUid());
        helper.verifyBytes(((FileItem) d).getContent(), ((FileItem) dcopy).getContent());

        clearSession();

        b = (CollectionItem) contentDao.findItemByPath("/testuser2/a/b");

        contentDao.copyItem(b,"/testuser2/a/e/bcopyshallow", false);

        clearSession();

        queryCollection = (CollectionItem) contentDao
                .findItemByPath("/testuser2/a/e/bcopyshallow");
        Assert.assertNotNull(queryCollection);

        queryCollection = (CollectionItem) contentDao
                .findItemByPath("/testuser2/a/e/bcopyshallow/c");
        Assert.assertNull(queryCollection);

        clearSession();
        d = (ContentItem) contentDao.findItemByUid(d.getUid());
        contentDao.copyItem(d,"/testuser2/dcopy", true);

        clearSession();

        dcopy = (ContentItem) contentDao.findItemByPath("/testuser2/dcopy");
        Assert.assertNotNull(dcopy);
    }

    public void testTickets() throws Exception {
        User testuser = getUser(userDao, "testuser");
        String name = "ticketable:" + System.currentTimeMillis();
        ContentItem item = generateTestContent(name, "testuser");

        CollectionItem root = (CollectionItem) contentDao.getRootItem(testuser);
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();
        newItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        Ticket ticket1 = new HibTicket();
        ticket1.setKey("ticket1");
        ticket1.setTimeout(10);
        ticket1.setOwner(testuser);
        HashSet privs = new HashSet();
        privs.add("priv1");
        privs.add("privs2");
        ticket1.setPrivileges(privs);

        contentDao.createTicket(newItem, ticket1);

        Ticket ticket2 = new HibTicket();
        ticket2.setKey("ticket2");
        ticket2.setTimeout(100);
        ticket2.setOwner(testuser);
        privs = new HashSet();
        privs.add("priv3");
        privs.add("priv4");
        ticket2.setPrivileges(privs);

        contentDao.createTicket(newItem, ticket2);

        clearSession();

        Ticket queryTicket1 = contentDao.findTicket("ticket1");
        Assert.assertNotNull(queryTicket1);
        Assert.assertNull(contentDao.findTicket("blah"));
        
        clearSession();
        
        newItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        
        queryTicket1 = contentDao.getTicket(newItem,"ticket1");
        Assert.assertNotNull(queryTicket1);
        verifyTicket(queryTicket1, ticket1);

        Collection tickets = contentDao.getTickets(newItem);
        Assert.assertEquals(2, tickets.size());
        verifyTicketInCollection(tickets, ticket1.getKey());
        verifyTicketInCollection(tickets, ticket2.getKey());

        contentDao.removeTicket(newItem, ticket1);
        clearSession();
        
        newItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        
        tickets = contentDao.getTickets(newItem);
        Assert.assertEquals(1, tickets.size());
        verifyTicketInCollection(tickets, ticket2.getKey());

        queryTicket1 = contentDao.getTicket(newItem, "ticket1");
        Assert.assertNull(queryTicket1);

        Ticket queryTicket2 = contentDao.getTicket(newItem, "ticket2");
        Assert.assertNotNull(queryTicket2);
        verifyTicket(queryTicket2, ticket2);

        contentDao.removeTicket(newItem, ticket2);
        
        clearSession();
        newItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        tickets = contentDao.getTickets(newItem);
        Assert.assertEquals(0, tickets.size());
    }
    
    public void testItemInMutipleCollections() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);
        
        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(a, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        CollectionItem b = new HibCollectionItem();
        b.setName("b");
        b.setOwner(user);
        
        b = contentDao.createCollection(root, b);
        
        contentDao.addItemToCollection(queryItem, b);
        
        clearSession();
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 2);
        
        b = (CollectionItem) contentDao.findItemByUid(b.getUid());
        contentDao.removeItemFromCollection(queryItem, b);
        clearSession();
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        a = (CollectionItem) contentDao.findItemByUid(a.getUid());
        contentDao.removeItemFromCollection(queryItem, a);
        clearSession();
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertNull(queryItem);
    }
    
    public void testItemInMutipleCollectionsError() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);
        
        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(a, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        CollectionItem b = new HibCollectionItem();
        b.setName("b");
        b.setOwner(user);
        
        b = contentDao.createCollection(root, b);
        
        ContentItem item2 = generateTestContent();
        item2.setName("test");
        contentDao.createContent(b, item2);
        
        // should get DuplicateItemName here
        try {
            contentDao.addItemToCollection(queryItem, b);
            Assert.fail("able to add item with same name to collection");
        } catch (DuplicateItemNameException e) {   
        }
    }
    
    public void testItemInMutipleCollectionsDeleteCollection() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new HibCollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);
        
        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(a, item);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        CollectionItem b = new HibCollectionItem();
        b.setName("b");
        b.setOwner(user);
        
        b = contentDao.createCollection(root, b);
        
        contentDao.addItemToCollection(queryItem, b);
        
        clearSession();
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 2);
        
        b = (CollectionItem) contentDao.findItemByUid(b.getUid());
        contentDao.removeCollection(b);
        
        clearSession();
        b = (CollectionItem) contentDao.findItemByUid(b.getUid());
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertNull(b);
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        a = (CollectionItem) contentDao.findItemByUid(a.getUid());
        contentDao.removeCollection(a);
        clearSession();
        
        a = (CollectionItem) contentDao.findItemByUid(a.getUid());
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        Assert.assertNull(a);
        Assert.assertNull(queryItem);
    }
    
    public void testContentDaoTriageStatus() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        item.setName("test");
        TriageStatus initialTriageStatus = new HibTriageStatus();
        TriageStatusUtil.initialize(initialTriageStatus);
        item.setTriageStatus(initialTriageStatus);

        ContentItem newItem = contentDao.createContent(root, item);

        Assert.assertTrue(getHibItem(newItem).getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        TriageStatus triageStatus = queryItem.getTriageStatus();
        Assert.assertEquals(initialTriageStatus, triageStatus);

        triageStatus.setCode(TriageStatus.CODE_LATER);
        triageStatus.setAutoTriage(false);
        BigDecimal rank = new BigDecimal("-98765.43");
        triageStatus.setRank(rank);
        
        contentDao.updateContent(queryItem);
        clearSession();
        
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        triageStatus = queryItem.getTriageStatus();
        Assert.assertEquals(triageStatus.getAutoTriage(), Boolean.FALSE);
        Assert.assertEquals(triageStatus.getCode(),
                            new Integer(TriageStatus.CODE_LATER));
        Assert.assertEquals(triageStatus.getRank(), rank);
        
        queryItem.setTriageStatus(null);
        contentDao.updateContent(queryItem);
        clearSession();
        // should be null triagestatus
        queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());
        triageStatus = queryItem.getTriageStatus();
        Assert.assertNull(triageStatus);
    }
    
    public void testContentDaoCreateFreeBusy() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        FreeBusyItem newItem = new HibFreeBusyItem();
        newItem.setOwner(user);
        newItem.setName("test");
        newItem.setIcalUid("icaluid");
        
        CalendarBuilder cb = new CalendarBuilder();
        net.fortuna.ical4j.model.Calendar calendar = cb.build(helper.getInputStream("testdata/vfreebusy.ics"));
        
        newItem.setFreeBusyCalendar(calendar);
        
        newItem = (FreeBusyItem) contentDao.createContent(root, newItem);

        Assert.assertTrue(getHibItem(newItem).getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        helper.verifyItem(newItem, queryItem);
    }
    
    public void testContentDaoCreateAvailability() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        AvailabilityItem newItem = new HibAvailabilityItem();
        newItem.setOwner(user);
        newItem.setName("test");
        newItem.setIcalUid("icaluid");
        
        CalendarBuilder cb = new CalendarBuilder();
        net.fortuna.ical4j.model.Calendar calendar = cb.build(helper.getInputStream("testdata/vavailability.ics"));
        
        newItem.setAvailabilityCalendar(calendar);
        
        newItem = (AvailabilityItem) contentDao.createContent(root, newItem);

        Assert.assertTrue(getHibItem(newItem).getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        ContentItem queryItem = (ContentItem) contentDao.findItemByUid(newItem.getUid());

        helper.verifyItem(newItem, queryItem);
    }
    
    public void testContentDaoUpdateCollection2() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        NoteItem note1 = generateTestNote("test1", "testuser");
        NoteItem note2 = generateTestNote("test2", "testuser");

        note1.setUid("1");
        note2.setUid("2");
        
        Set<ContentItem> items = new HashSet<ContentItem>();
        items.add(note1);
        items.add(note2);

        contentDao.updateCollection(root, items);

        items.clear();
        
        note1 = (NoteItem) contentDao.findItemByUid("1");
        note2 = (NoteItem) contentDao.findItemByUid("2");
        
        items.add(note1);
        items.add(note2);
        
        Assert.assertNotNull(note1);
        Assert.assertNotNull(note2);
        
        note1.setDisplayName("changed");
        note2.setIsActive(false);
       
        contentDao.updateCollection(root, items);
        
        note1 = (NoteItem) contentDao.findItemByUid("1");
        note2 = (NoteItem) contentDao.findItemByUid("2");
        
        Assert.assertNotNull(note1);
        Assert.assertEquals("changed", note1.getDisplayName());
        Assert.assertNull(note2);
    }
    
    public void testContentDaoUpdateCollectionWithMods() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        NoteItem note1 = generateTestNote("test1", "testuser");
        NoteItem note2 = generateTestNote("test2", "testuser");

        note1.setUid("1");
        note2.setUid("1:20070101");
        
        note2.setModifies(note1);
        
        Set<ContentItem> items = new LinkedHashSet<ContentItem>();
        items.add(note2);
        items.add(note1);

        
        // should fail because modification is processed before master
        try {
            contentDao.updateCollection(root, items);
            Assert.fail("able to create invalid mod");
        } catch (ModelValidationException e) {
        }
        
        items.clear();
        
        // now make sure master is processed before mod
        items.add(note1);
        items.add(note2);
       
        contentDao.updateCollection(root, items);
        
        note1 = (NoteItem) contentDao.findItemByUid("1");
        Assert.assertNotNull(note1);
        Assert.assertTrue(1==note1.getModifications().size());
        note2 = (NoteItem) contentDao.findItemByUid("1:20070101");
        Assert.assertNotNull(note2);
        Assert.assertNotNull(note2.getModifies());  
        
        // now create new collection
        CollectionItem a = new HibCollectionItem();
        a.setUid("a");
        a.setName("a");
        a.setOwner(user);
        
        a = contentDao.createCollection(root, a);
        
        // try to add mod to another collection before adding master
        items.clear();
        items.add(note2);
        
        // should fail because modification is added before master
        try {
            contentDao.updateCollection(a, items);
            Assert.fail("able to add mod before master");
        } catch (ModelValidationException e) {
        }
        
        items.clear();
        items.add(note1);
        items.add(note2);
        
        contentDao.updateCollection(a, items);
        
        // now create new collection
        CollectionItem b = new HibCollectionItem();
        b.setUid("b");
        b.setName("b");
        b.setOwner(user);
        
        b = contentDao.createCollection(root, b);
        
        // only add master
        items.clear();
        items.add(note1);
        
        contentDao.updateCollection(b, items);
        
        // adding master should add mods too
        clearSession();
        b = (CollectionItem) contentDao.findItemByUid("b");
        Assert.assertNotNull(b);
        Assert.assertEquals(2, b.getChildren().size());
    }
    
    public void testContentDaoUpdateCollectionWithDuplicateIcalUids() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        NoteItem note1 = generateTestNote("test1", "testuser");
        NoteItem note2 = generateTestNote("test2", "testuser");

        note1.setUid("1");
        note1.setIcalUid("1");
        note2.setUid("2");
        note2.setIcalUid("1");
        
        Set<ContentItem> items = new HashSet<ContentItem>();
        items.add(note1);
        items.add(note2);

        try {
            contentDao.updateCollection(root, items);
            Assert.fail("able to create duplicate icaluids!");
        } catch (IcalUidInUseException e) {
        }
    }
    
    private void verifyTicket(Ticket ticket1, Ticket ticket2) {
        Assert.assertEquals(ticket1.getKey(), ticket2.getKey());
        Assert.assertEquals(ticket1.getTimeout(), ticket2.getTimeout());
        Assert.assertEquals(ticket1.getOwner().getUsername(), ticket2
                .getOwner().getUsername());
        Iterator it1 = ticket1.getPrivileges().iterator();
        Iterator it2 = ticket2.getPrivileges().iterator();

        Assert.assertEquals(ticket1.getPrivileges().size(), ticket1
                .getPrivileges().size());

        while (it1.hasNext())
            Assert.assertEquals(it1.next(), it2.next());
    }

    private void verifyTicketInCollection(Collection tickets, String name) {
        for (Iterator it = tickets.iterator(); it.hasNext();) {
            Ticket ticket = (Ticket) it.next();
            if (ticket.getKey().equals(name))
                return;
        }

        Assert.fail("could not find ticket: " + name);
    }

    private void verifyContains(Collection items, CollectionItem collection) {
        for (Iterator it = items.iterator(); it.hasNext();) {
            Item item = (Item) it.next();
            if (item instanceof CollectionItem
                    && item.getName().equals(collection.getName()))
                return;
        }
        Assert.fail("collection not found");
    }

    private void verifyContains(Collection items, ContentItem content) {
        for (Iterator it = items.iterator(); it.hasNext();) {
            Item item = (Item) it.next();
            if (item instanceof ContentItem
                    && item.getName().equals(content.getName()))
                return;
        }
        Assert.fail("content not found");
    }

    private User getUser(UserDao userDao, String username) {
        return helper.getUser(userDao, contentDao, username);
    }

    private FileItem generateTestContent() throws Exception {
        return generateTestContent("test", "testuser");
    }

    private FileItem generateTestContent(String name, String owner)
            throws Exception {
        FileItem content = new HibFileItem();
        content.setName(name);
        content.setDisplayName(name);
        content.setContent(helper.getBytes("testdata/testdata1.txt"));
        content.setContentLanguage("en");
        content.setContentEncoding("UTF8");
        content.setContentType("text/text");
        content.setOwner(getUser(userDao, owner));
        content.addAttribute(new HibStringAttribute(new HibQName("customattribute"),
                "customattributevalue"));
        return content;
    }
    
    private NoteItem generateTestNote(String name, String owner)
            throws Exception {
        NoteItem content = new HibNoteItem();
        content.setName(name);
        content.setDisplayName(name);
        content.setOwner(getUser(userDao, owner));
        return content;
    }
    
    private org.w3c.dom.Element createTestElement() throws Exception {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( "root" );

        root.addElement( "author" )
            .addAttribute( "name", "James" )
            .addAttribute( "location", "UK" )
            .addText( "James Strachan" );
        
        root.addElement( "author" )
            .addAttribute( "name", "Bob" )
            .addAttribute( "location", "US" )
            .addText( "Bob McWhirter" );

        return new DOMWriter().write(document).getDocumentElement();
    }
    
    private HibItem getHibItem(Item item) {
        return (HibItem) item;
    }

}
