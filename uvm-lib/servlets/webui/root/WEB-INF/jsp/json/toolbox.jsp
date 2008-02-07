<%@ include file="/WEB-INF/jsp/include.jsp" %>

{
 'success':${success},
 'msg':'<fmt:message key="${msg}"/>',
 'data':[ 
	<c:forEach var="item" items="${data}" varStatus="status">
	  {'name':'${item.name}','displayName':'${item.displayName}','image':'image?name=${item.name}'}<c:if test="${!status.last}">,</c:if>	  
	</c:forEach>  			
 ]
}