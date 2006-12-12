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
package org.osaf.cosmo.eim.eimml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.TestHelper;

/**
 * Test Case for {@link EimmlTypeConverter}.
 */
public class EimmlTypeConverterTest extends TestCase
    implements EimmlConstants {
    private static final Log log =
        LogFactory.getLog(EimmlTypeConverterTest.class);

    private static final TestHelper helper = new TestHelper();

    /** */
    public void testToBytes() throws Exception {
        String testString = "this is a test string";

        try {
            EimmlTypeConverter.toBytes(testString, null);
            fail("Converted to bytes with a null transfer encoding");
        } catch (IllegalArgumentException e) {}

        try {
            EimmlTypeConverter.toBytes(testString, "bogus-encoding");
            fail("Converted to bytes with a bogus transfer encoding");
        } catch (EimmlConversionException e) {}

        byte[] testBytes = testString.getBytes();
        String testEncoded =
            new String(Hex.encodeHex(Base64.encodeBase64(testBytes)));
        byte[] resultBytes =
            EimmlTypeConverter.toBytes(testEncoded, TRANSFER_ENCODING_BASE64);

        for (int i=0; i<resultBytes.length; i++)
            assertEquals("Byte " + i + " does not match", testBytes[i],
                         resultBytes[i]);
    }

    /** */
    public void testfromBytes() throws Exception {
        String testString = "this is a test string";
        byte[] testBytes = testString.getBytes();

        try {
            EimmlTypeConverter.fromBytes(testBytes, null);
            fail("Converted from bytes with a null transfer encoding");
        } catch (IllegalArgumentException e) {}

        try {
            EimmlTypeConverter.fromBytes(testBytes, "bogus-encoding");
            fail("Converted from bytes with a bogus transfer encoding");
        } catch (EimmlConversionException e) {}

        String resultEncoded =
            EimmlTypeConverter.fromBytes(testBytes, TRANSFER_ENCODING_BASE64);

        assertEquals("Encoded string does not match",
                     new String(Hex.encodeHex(Base64.encodeBase64(testBytes))),
                     resultEncoded);
    }

    /** */
    public void testToText() throws Exception {
        String testString = "this is a test string";

        try {
            EimmlTypeConverter.toText(testString, null);
            fail("Converted to text with a null original encoding");
        } catch (IllegalArgumentException e) {}

        try {
            EimmlTypeConverter.toText(testString, "bogus-encoding");
            fail("Converted to text with a bogus original encoding");
        } catch (EimmlConversionException e) {}

        String resultString = EimmlTypeConverter.toText(testString, "UTF-8");
        assertEquals("Result UTF-8 string does not match", testString,
                     resultString);

        // XXX: encode testString with a non UTF-8 encoding 
    }

    /** */
    public void testToClob() throws Exception {
        String testString = "this is a test string";
        byte[] testBytes = testString.getBytes();

        Reader resultReader = EimmlTypeConverter.toClob(testString);

        for (int i=0; i<testBytes.length; i++) {
            int rv = resultReader.read();
            if  (rv == -1)
                fail("Result reader is shorter than test string");
            assertEquals("Byte " + i + " does not match", testBytes[i], rv);
        }
    }

    /** */
    public void testFromClob() throws Exception {
        String testString = "this is a test string";
        StringReader testReader = new StringReader(testString);

        String resultString = EimmlTypeConverter.fromClob(testReader);

        assertEquals("Result string does not match", testString, resultString);
    }

    /** */
    public void testToBlob() throws Exception {
        String testString = "this is a test string";

        try {
            EimmlTypeConverter.toBlob(testString, null);
            fail("Converted to blob with a null transfer encoding");
        } catch (IllegalArgumentException e) {}

        try {
            EimmlTypeConverter.toBlob(testString, "bogus-encoding");
            fail("Converted to blob with a bogus transfer encoding");
        } catch (EimmlConversionException e) {}

        byte[] testBytes = testString.getBytes();
        String testEncoded =
            new String(Hex.encodeHex(Base64.encodeBase64(testBytes)));
        InputStream resultStream =
            EimmlTypeConverter.toBlob(testEncoded, TRANSFER_ENCODING_BASE64);

        for (int i=0; i<testBytes.length; i++) {
            int rv = resultStream.read();
            if  (rv == -1)
                fail("Result stream is shorter than test string");
            assertEquals("Byte " + i + " does not match", testBytes[i], rv);
        }
    }

    /** */
    public void testFromBlob() throws Exception {
        String testString = "this is a test string";
        byte[] testBytes = testString.getBytes();
        String testEncoded =
            new String(Hex.encodeHex(Base64.encodeBase64(testBytes)));
        ByteArrayInputStream testStream = new ByteArrayInputStream(testBytes);

        try {
            EimmlTypeConverter.fromBlob(testStream, null);
            fail("Converted from blob with a null transfer encoding");
        } catch (IllegalArgumentException e) {}

        try {
            EimmlTypeConverter.fromBlob(testStream, "bogus-encoding");
            fail("Converted from blob with a bogus transfer encoding");
        } catch (EimmlConversionException e) {}

        String resultEncoded =
            EimmlTypeConverter.fromBlob(testStream, TRANSFER_ENCODING_BASE64);

        assertEquals("Result string does not match", testEncoded,
                     resultEncoded);
    }

    /** */
    public void testToInteger() throws Exception {
        String testString = "42";

        Integer resultInteger = EimmlTypeConverter.toInteger(testString);

        assertEquals("Result integer does not match", new Integer(testString),
                     resultInteger);
    }

    /** */
    public void testFromInteger() throws Exception {
        String testString = "42";
        Integer testInteger = new Integer(testString);

        String resultString = EimmlTypeConverter.fromInteger(testInteger);

        assertEquals("Result string does not match", testString, resultString);
    }

    /** */
    public void testToDateTime() throws Exception {
        String testString = "1996-12-19T16:39:57-0800";

        Date resultDate = EimmlTypeConverter.toDateTime(testString);
        Calendar resultCalendar = Calendar.getInstance();
        resultCalendar.setTime(resultDate);

        assertEquals("Result year does not match", 1996,
                     resultCalendar.get(Calendar.YEAR));
        assertEquals("Result month does not match", 11,
                     resultCalendar.get(Calendar.MONTH));
        assertEquals("Result day of week does not match", 19,
                     resultCalendar.get(Calendar.DAY_OF_MONTH));
        assertEquals("Result hour of day does not match", 16,
                     resultCalendar.get(Calendar.HOUR_OF_DAY));
        assertEquals("Result minute does not match", 39,
                     resultCalendar.get(Calendar.MINUTE));
        assertEquals("Result second does not match", 57,
                     resultCalendar.get(Calendar.SECOND));
        assertEquals("Result timezone offset does not match",
                     1000 * 60 * 60 * -8,
                     resultCalendar.get(Calendar.ZONE_OFFSET));
    }

    /** */
    public void testFromDateTime() throws Exception {
        String testString = "1996-12-19T16:39:57-0800";

        Calendar testCalendar = Calendar.getInstance();
        testCalendar.set(Calendar.YEAR, 1996);
        testCalendar.set(Calendar.MONTH, 11);
        testCalendar.set(Calendar.DAY_OF_MONTH, 19);
        testCalendar.set(Calendar.HOUR_OF_DAY, 16);
        testCalendar.set(Calendar.MINUTE, 39);
        testCalendar.set(Calendar.SECOND, 57);
        testCalendar.set(Calendar.ZONE_OFFSET, 1000 * 60 * 60  * -8);

        String resultString =
            EimmlTypeConverter.fromDateTime(testCalendar.getTime());

        assertEquals("Result string does not match", testString, resultString);
    }

    /** */
    public void testToDecimal() throws Exception {
        try {
            EimmlTypeConverter.toDecimal("deadbeef");
            fail("converted to decimal with a bogus string");
        } catch (EimmlConversionException e) {}

        String testString = "3.14159";
        BigDecimal resultDecimal = EimmlTypeConverter.toDecimal(testString);

        assertEquals("Result decimal does not match",
                     resultDecimal.toString(), testString);
    }

    /** */
    public void testFromDecimal() throws Exception {
        String testString = "3.14159";
        BigDecimal testDecimal = new BigDecimal(testString);

        try {
            EimmlTypeConverter.fromDecimal(testDecimal, -1, 1);
            fail("converted to decimal with negative digits");
        } catch (IllegalArgumentException e) {}

        try {
            EimmlTypeConverter.fromDecimal(testDecimal, 1, -1);
            fail("converted to decimal with negative decimal places");
        } catch (IllegalArgumentException e) {}

        String resultString =
            EimmlTypeConverter.fromDecimal(testDecimal, 1, 5);

        assertEquals("Result string does not match", testString, resultString);
    }
}