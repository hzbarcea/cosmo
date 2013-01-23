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

<%@ include file="/WEB-INF/jsp/taglibs.jsp"  %>
<%@ include file="/WEB-INF/jsp/tagfiles.jsp" %>

<%@ attribute name="prefix" 		%>
<%@ attribute name="recoverFunctionModule" 		%>
<%@ attribute name="recoverFunctionName" 		%>

<fmt:setBundle basename="MessageResources"/>

<cosmo:threeColumnLayout prefix="${prefix}" stylesheets="three_column_forms">

<script type="text/javascript">
dojo.require("cosmo.ui.widget.Recoverer");
</script>

<div id="center" class="column">
  <div dojoType="cosmo.ui.widget.Recoverer" id="recoverer" class="threeColumnForm"
       displayDefaultInfo="true" i18nPrefix="${prefix}"
       recoverFunctionModule="${recoverFunctionModule}" recoverFunctionName="${recoverFunctionName}">
  </div>
  <div class="padTopLots">
    <cosmo:aboutPopupLink/>
  </div>
</div>
</cosmo:threeColumnLayout>
