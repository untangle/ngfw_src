<!DOCTYPE html>
<%
String companyName = com.untangle.uvm.UvmContextFactory.context().brandingManager().getCompanyName();
%>
<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>${companyName} | Request Quarantine Digest</title>
    <style type="text/css">
        @import "/ext5/packages/ext-theme-gray/build/resources/ext-theme-gray-all.css";
    </style>
    <script type="text/javascript" src="/ext5/ext-all-debug.js"></script>
    <script type="text/javascript" src="/ext5/packages/ext-theme-gray/build/ext-theme-gray.js"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="script/request.js"></script>

    <script type="text/javascript">
        Ext.onReady(function() {
            Ung.Request.init({
                companyName: '${companyName}',
            })
        });
    </script>
 </head>
<body>
</body>
</html>
