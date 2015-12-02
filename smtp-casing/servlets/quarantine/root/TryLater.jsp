<%@page language="java" import="com.untangle.uvm.*"%>
<!DOCTYPE html>

<%
UvmContext uvm = UvmContextFactory.context();
String companyName = uvm.brandingManager().getCompanyName();
String extjsTheme = uvm.skinManager().getSkinInfo().getExtjsTheme();
%>
<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title><%=companyName%> | Try Later</title>
    <style type="text/css">
        @import "/ext6/classic/theme-<%=extjsTheme%>/resources/theme-<%=extjsTheme%>-all.css";
    </style>
    <script type="text/javascript" src="/ext6/ext-all.js"></script>
    <script type="text/javascript" src="/ext6/classic/theme-<%=extjsTheme%>/theme-<%=extjsTheme%>.js"></script>

    <script type="text/javascript" src="script/tryLater.js"></script>

    <script type="text/javascript">
        Ext.onReady(function() {
            Ung.TryLater.init({
                companyName: '<%=companyName%>',
            })
        });
    </script>
 </head>
<body>
</body>
</html>
