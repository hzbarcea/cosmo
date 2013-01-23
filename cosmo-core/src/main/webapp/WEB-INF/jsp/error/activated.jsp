<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<%--
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
--%>

<%@ page    isErrorPage="true"               %>
<%@ include file="/WEB-INF/jsp/taglibs.jsp"  %>
<%@ include file="/WEB-INF/jsp/tagfiles.jsp" %>
<cosmo:standardLayout prefix="Error.NotFound." showNav="false">
<cosmo:staticbaseurl var="staticBaseUrl"/>
<div id="contentWrapper">  
<p>
  <fmt:message key="Error.Activated.AlreadyActivated"/>
</p>
<p>
<a id="login" href="${staticBaseUrl}/login"><fmt:message key="Error.Activated.Login"/></a>
</p>
</div>
</cosmo:standardLayout>