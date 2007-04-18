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
package org.osaf.cosmo.atom.processor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.eim.EimRecordSet;
import org.osaf.cosmo.eim.json.JsonStreamException;
import org.osaf.cosmo.eim.json.JsonStreamReader;
import org.osaf.cosmo.eim.json.JsonValidationException;
import org.osaf.cosmo.eim.schema.EimValidationException;
import org.osaf.cosmo.eim.schema.ItemTranslator;
import org.osaf.cosmo.model.NoteItem;

/**
 * A class that processes content specified as JSON-serialized EIM
 * recordsets.
 *
 * @see NoteItem
 */
public class JsonProcessor implements ContentProcessor {
    private static final Log log = LogFactory.getLog(JsonProcessor.class);

    /**
     * Process an EIM-JSON content body describing changes to an
     * item.
     *
     * @param content the content
     * @param item the item which the content represents
     * @throws ProcessorException
     */
    public void processContent(Reader content,
                               NoteItem item)
        throws ProcessorException {
        JsonStreamReader reader = null;
        EimRecordSet recordset = null;
        try {
            reader = new JsonStreamReader(content);
            recordset = reader.nextRecordSet();
            if (recordset == null) {
                throw new ValidationException("No recordset read from stream");
            }
        } catch (IOException e) {
            throw new ProcessorException("Unable to read stream", e);
        } catch (JsonValidationException e) {
            throw new ValidationException("Invalid json packet", e);
        } catch (JsonStreamException e) {
            throw new ProcessorException("Unable to read parse recordset", e);
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                log.warn("Unable to close json reader", e);
            }
        }

        try {
            ItemTranslator translator = new ItemTranslator(item);
            translator.applyRecords(recordset);
        } catch (EimValidationException e) {
            throw new ValidationException("Invalid recordset", e);
        } catch (Exception e) {
            throw new ProcessorException("Unable to apply recordset", e);
        }
    }

    /**
     * Process an EIM-JSON content body describing changes to an
     * item.
     *
     * @param content the content
     * @param item the item which the content represents
     * @throws ProcessorException
     */
    public void processContent(String content,
                               NoteItem item)
        throws ProcessorException {
        processContent(new StringReader(content), item);
    }
}