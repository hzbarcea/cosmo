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
package org.osaf.cosmo.eim;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a single EIM record.
 * <p>
 * An EIM record is associated with a particular namespace that allows
 * EIM processors to understand the semantics of the entity modeled by
 * the record.
 * <p>
 * A record is composed of a uuid and 1..n fields. The uuid is a
 * string that uniquely identifies the entity, while the fields
 * represent its data.
 * <p>
 * A record may be marked as "deleted", representing the fact that an
 * aspect of an entity (for example a stamp) has been removed from
 * storage.
 *
 * @see EimRecordField
 */
public class EimRecord {
    private static final Log log = LogFactory.getLog(EimRecord.class);

    private EimRecordSet recordset;
    private String namespace;
    private String uuid;
    private ArrayList<EimRecordField> fields;
    private boolean deleted = false;

    /** */
    public EimRecord() {
        fields = new ArrayList<EimRecordField>();
    }

    /** */
    public String getNamespace() {
        return namespace;
    }

    /** */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /** */
    public String getUuid() {
        return uuid;
    }

    /** */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /** */
    public List<EimRecordField> getFields() {
        return fields;
    }

    /** */
    public void addField(EimRecordField field) {
        fields.add(field);
        field.setRecord(this);
    }

    /** */
    public boolean isDeleted() {
        return deleted;
    }

    /** */
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /** */
    public EimRecordSet getRecordSet() {
        return recordset;
    }

    /** */
    public void setRecordSet(EimRecordSet recordset) {
        this.recordset = recordset;
    }

    /** */
    public String toString() {
        return ToStringBuilder.
            reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
