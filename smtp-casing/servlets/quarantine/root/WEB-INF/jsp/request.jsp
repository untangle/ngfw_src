<!DOCTYPE html>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>${companyName} | Request Quarantine Digest</title>
    <style type="text/css">
        @import "/ext6/classic/theme-${extjsTheme}/resources/theme-${extjsTheme}-all.css";
    </style>
    <script type="text/javascript" src="/ext6/ext-all.js"></script>
    <script type="text/javascript" src="/ext6/classic/theme-${extjsTheme}/theme-${extjsTheme}.js"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="script/request.js"></script>

    <script type="text/javascript">
        Ext.onReady(function() {
            Ung.Request.init({
                companyName: "${fn:replace(companyName,'"','')}",
            })
        });
    </script>
 </head>
<body>
</body>
</html>
