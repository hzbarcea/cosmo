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
package org.osaf.cosmo.model.hibernate;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.MapKey;
import org.osaf.cosmo.model.Attribute;
import org.osaf.cosmo.model.DictionaryAttribute;
import org.osaf.cosmo.model.ModelValidationException;
import org.osaf.cosmo.model.QName;


/**
 * Hibernate persistent DictionaryAtttribute.
 */
@Entity
@DiscriminatorValue("dictionary")
public class HibDictionaryAttribute extends HibAttribute
        implements java.io.Serializable, DictionaryAttribute {

    /**
     * 
     */
    private static final long serialVersionUID = 3713980765847199175L;
    
    @CollectionOfElements
    @JoinTable(
            name="dictionary_values",
            joinColumns = @JoinColumn(name="attributeid")
    )
    @MapKey(columns=@Column(name="keyname", length=255))
    @Column(name="stringvalue", length=2048)
    private Map<String, String> value = new HashMap<String,String>(0);

    /** default constructor */
    public HibDictionaryAttribute() {
    }

    public HibDictionaryAttribute(QName qname, Map<String, String> value)
    {
        setQName(qname);
        this.value = value;
    }

    // Property accessors
    /* (non-Javadoc)
     * @see org.osaf.cosmo.model.copy.InterfaceDictionaryAttribute#getValue()
     */
    public Map<String, String> getValue() {
        return this.value;
    }

    /* (non-Javadoc)
     * @see org.osaf.cosmo.model.copy.InterfaceDictionaryAttribute#setValue(java.util.Map)
     */
    public void setValue(Map<String, String> value) {
        this.value = value;
    }
    
    public void setValue(Object value) {
        if (value != null && !(value instanceof Map))
            throw new ModelValidationException(
                    "attempted to set non Map value on attribute");
        setValue((Map<String, String>) value);
    }
    
    public Attribute copy() {
        DictionaryAttribute attr = new HibDictionaryAttribute();
        attr.setQName(getQName().copy());
        attr.setValue(new HashMap<String, String>(value));
        return attr;
    }

}
