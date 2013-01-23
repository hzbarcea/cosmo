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

<%@ include file="/WEB-INF/jsp/taglibs.jsp"  %>
<%@ include file="/WEB-INF/jsp/tagfiles.jsp" %>

<c:if test="${empty isCollection}">
  <c:set var="isCollection" value="false"/>
</c:if>

<c:if test="${isCollection}">
  <c:set var="Item" value="${Collection}"/>
  <c:set var="feedPath" value="/atom/collection/${Collection.uid}"/>
</c:if>
<c:set var="davPath" value="/dav${Path}"/>
<c:set var="pimPath" value="/pim/collection/${Collection.uid}"/>

<div class="hd" style="margin-top: 12px;">
  Tickets
</div>

<script type="text/javascript">
dojo.require('dojo.parser');
dojo.require("cosmo.ui.widget.TicketWidget");
</script>
<div id="newTicketContainer">
  <div dojoType="cosmo.ui.widget.TicketWidget" id="newTicketWidget" 
       itemId="${Path}"  dojoAttachEvent="createSuccess: reload">
    <script type="dojo/connect" event="createSuccess" args="data">
      // TODO: reloading the page here is a pretty ugly way to do this...
      location = location;
    </script>
  </div>
</div>
<script type="text/javascript">
    dojo.addOnLoad(function(){
        dojo.parser.parse(dojo.byId("newTicketContainer"));
    });
</script>

<div style="margin-top:12px;">
  <table cellpadding="4" cellspacing="1" border="0" width="100%">
    <tr>
      <td class="smTableColHead" style="width:1%;">
        &nbsp;
      </td>
      <td class="smTableColHead" style="text-align:left;">
        Id
      </td>
      <td class="smTableColHead">
        Sharing Links
      </td>
      <td class="smTableColHead">
        Owner
      </td>
      <td class="smTableColHead">
        Timeout
      </td>
      <td class="smTableColHead">
        Privileges
      </td>
      <td class="smTableColHead">
        Created
      </td>
    </tr>
    <c:forEach var="ticket" items="${Item.tickets}">
    <tr>
      <td class="smTableData" style="text-align:center; white-space:nowrap;">
        <a href='<c:url value="/browse/ticket${path}/revoke/${ticket.key}" />'>
          [revoke]
        </a>
      </td>
      <td class="smTableData">
        <c:out value="${ticket.key}"/>
      </td>
      <td class="smTableData" style="text-align:center;">
        <a href='<c:url value="${davPath}?ticket=${ticket.key}" />'>[dav]</a>
        <c:if test="${isCollection}">
          <a href='<c:url value="${feedPath}?ticket=${ticket.key}" />'>[feed]</a>
          <a href='<c:url value="${pimPath}?ticket=${ticket.key}" />'>[Chandler]</a>
        </c:if>
      </td>
      <td class="smTableData" style="text-align:center;">
        <c:out value="${ticket.owner.username}"/>
      </td>
      <td class="smTableData" style="text-align:center;">
        <c:out value="${ticket.timeout}"/>
      </td>
      <td class="smTableData" style="text-align:center;">
        <c:forEach var="privilege" items="${ticket.privileges}">
          <c:out value="${privilege}"/>
        </c:forEach>
      </td>
      <td class="smTableData" style="text-align:center;">
        <fmt:formatDate value="${ticket.created}" type="both"/>
      </td>
    </tr>
    </c:forEach>
  </table>
</div>
