#! /bin/sed

/^ *<html>/d
/^ *<head>/d
/^ *<meta http-equiv="Content-Type" content="text\/html; charset=.\+">/d
/^ *<style type="text\/css">/d
/^ *a {text-decoration\: none}/d
/^ *<\/style>/d
/^ *<\/head>/d
/^ *<body text="#[0-9]\+" link="#[0-9]\+" alink="#[0-9]\+" vlink="#[0-9]\+">/d
/^ *<\/body>/d
/^ *<\/html>/d
/^ *<td colspan=[0-9]\+><img src="\.\.\/images\/px" style="width\: 612px; height\: 19px"><\/td>/d
s/ *bgcolor=white//
s/ *background-color\: #FFFFFF;//
/*/p
