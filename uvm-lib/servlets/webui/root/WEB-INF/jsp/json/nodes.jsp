<%@ include file="/WEB-INF/jsp/include.jsp" %>

{
 'success':${success},
 'msg':'<fmt:message key="${msg}"/>',
 'data':[ 
	<c:forEach var="node" items="${data}" varStatus="status">
	  <%--workaround because NodeContext.node() should be named NodeContext.getNode()--%>
  	  <jsp:useBean id="node"  type="com.untangle.uvm.node.NodeContext" />
	  {'id':'${node.tid.id}','tid':'${node.tid.id}','name':'${node.nodeDesc.name}','displayName':'${node.nodeDesc.displayName}','viewPosition':${node.mackageDesc.viewPosition},
	  'rackType':'${node.mackageDesc.rackType}','isService':${node.mackageDesc.service},'isUtil':${node.mackageDesc.util},'isSecurity':${node.mackageDesc.security},'isCore':${node.mackageDesc.core},
	  'runState':'<%= node.node().getRunState() %>','image':'rack.htm?action=getImage&name=${node.mackageDesc.name}',
	  'helpLink':'<uvm:help source="${node.nodeDesc.displayName}"/>',
	  'blingers':[{'type':'ActivityBlinger','bars':[{'code':'pass','name':'PASS'},{'code':'block','name':'BLOCK'},{'code':'mark','name':'MARK'},{'code':'quarant','name':'QUARANT'}]},{'type':'SessionsBlinger'},{'type':'DataRateBlinger'}]}
	  <c:if test="${!status.last}">,</c:if>
	</c:forEach>  			
 ]
}

