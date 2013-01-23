/*
 * Copyright 2005-2006 Open Source Applications Foundation
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
package org.osaf.cosmo.ui.jsp.tag;

import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.CosmoSecurityException;
import org.osaf.cosmo.security.CosmoSecurityManager;
import org.osaf.cosmo.spring.CosmoPropertyPlaceholderConfigurer;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This tag provides access to the {@link User} object provided by
 * the current Cosmo security context as the value of a scripting
 * variable.
 *
 * @see SimpleVarSetterTag
 */
public class CosmoPropertiesTag extends SimpleVarSetterTag {
    private static final Log log = LogFactory.getLog(CosmoPropertiesTag.class);
    private WebApplicationContext wac;
    private String BEAN_PROPERTY_PLACEHOLDER_CONFIGURER = "propertyPlaceholderConfigurer";
    private String propertyName;
    /**
     * @return the <code>User</code> provided by the current security
     * context 
     * @throws JspException if there is an error obtaining the
     * security context
     */
    public Object computeValue()
        throws JspException {
        try {
            ServletContext sc =
                ((PageContext)getJspContext()).getServletContext();
            CosmoPropertyPlaceholderConfigurer propertyPlaceholderConfigurer = 
                (CosmoPropertyPlaceholderConfigurer)
                TagUtils.getBean(sc, BEAN_PROPERTY_PLACEHOLDER_CONFIGURER,
                                 CosmoPropertyPlaceholderConfigurer.class);
            return propertyPlaceholderConfigurer.getProperties().getProperty(
                    getPropertyName());
        } catch (CosmoSecurityException e) {
            throw new JspException(
                    "can't get configuration property placeholder", e);
        }
    }
    
    public String getPropertyName() {
        return propertyName;
    }
    
    public void setPropertyName(String property) {
        this.propertyName = property;
    }
    
    
}
