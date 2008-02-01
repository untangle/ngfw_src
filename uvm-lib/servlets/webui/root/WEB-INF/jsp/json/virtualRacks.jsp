<%@ include file="/WEB-INF/jsp/include.jsp" %>

{
 'success':${success},
 'msg':'<fmt:message key="${msg}"/>',
 'data':[ 
	<c:forEach var="rack" items="${data}" varStatus="status">
	  {'id':'${rack.id}','name':'${rack.name}','notes':'${rack.notes}','isDefault':${rack.default}}<c:if test="${!status.last}">,</c:if>
	</c:forEach>  			
 ]
}