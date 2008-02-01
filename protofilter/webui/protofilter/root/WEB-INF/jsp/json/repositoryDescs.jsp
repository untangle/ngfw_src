<%@ include file="/WEB-INF/jsp/include.jsp" %>

{
 'success':${success},
 'msg':'<fmt:message key="${msg}"/>',
 'data':[ 
	<c:forEach var="repoDesc" items="${data}" varStatus="status">
	  {'name':'${repoDesc.name}','displayName':'<fmt:message key="${repoDesc.name}"/>'}<c:if test="${!status.last}">,</c:if>
	</c:forEach>  			
 ]
}