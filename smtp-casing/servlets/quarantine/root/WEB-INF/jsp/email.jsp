<!DOCTYPE html>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>${companyName} | Request Quarantine Digest</title>

    <!-- FontAwesome -->
    <link href="/ext6.2/fonts/font-awesome/css/font-awesome.min.css" rel="stylesheet" />

    <!-- JsonRPC -->
    <script src="/jsonrpc/jsonrpc.js"></script>

    <!-- ExtJS lib & theme -->
    <c:set var="debug" value="${param['debug']}"/>
    <c:choose>
        <c:when test="${debug == '1'}">
    <script src="/ext6.2/ext-all-debug.js"></script>
        </c:when>
        <c:otherwise>
    <script src="/ext6.2/ext-all.js"></script>
        </c:otherwise>
    </c:choose>
    <script src="/ext6.2/classic/theme-${extjsTheme}/theme-${extjsTheme}.js"></script>
    <link href="/ext6.2/classic/theme-${extjsTheme}/resources/theme-${extjsTheme}-all.css" rel="stylesheet" />


    <script src="/script/common/bootstrap.js"></script>
    <script>
        Ext.onReady(function () {
            // setups all initializations and load required scrips
            Bootstrap.load([
                '/script/common/util-all.js', // include custom grid module
//                '/script/common/reports-all.js', // include reports module
                '/script/common/ungrid-all.js', // include custom grid module
                // 'script/email.js'
                'script/email.js'
            ], 'QUARANTINE', function (ex) {
                // if everything is initialized just launch the application
                Ext.application({
                    extend: 'Ung.Application',
                    namespace: 'Ung'
                });
            });
        });
    </script>



    <!-- <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="script/request.js"></script>

    <script type="text/javascript">
        Ext.onReady(function() {
            Ung.Request.init({
                companyName: "${fn:replace(companyName,'"','')}",
            })
        });
    </script> -->
 </head>
<body>
</body>
</html>
