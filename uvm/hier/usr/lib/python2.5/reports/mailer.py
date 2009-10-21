#!/usr/bin/python

# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
# Copyright (c) 2003-2009 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Aaron Read <amread@untangle.com>

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

import sys

if PREFIX != '':
    sys.path.insert(0, REPORTS_PYTHON_DIR)

import gettext
import locale
import logging
import mx
import os
import smtplib
import reports.i18n_helper
import reports.sql_helper as sql_helper

from email import Encoders
from email.MIMEBase import MIMEBase
from email.MIMEImage import MIMEImage
from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext

def mail_reports(date, file):
    receivers, sender = __get_mail_info()
    company_name = __get_branding_info()

    url = self.__get_url(date)

    for receiver in receivers:
        mail(file, sender, receiver, date, company_name, has_web_access,
             url)

def mail(file, sender, receiver, date, company_name, has_web_access,
         url):
    msgRoot = MIMEMultipart('related')
    msgRoot['Subject'] = _('New %s Reports Available') % company_name
    msgRoot['From'] = sender
    msgRoot['To'] = receiver

    if has_web_access and url:
        msg = """\
The %(company)s Summary Reports for %(date)s are attached.
The PDF file requires Adobe Acrobat Reader to view.

For more in-depth online reports
<a href='%(url)s'>click here to view Online %(company)s Reports</a>
""" % { 'company': company_name,
        'date': date.strftime(locale.nl_langinfo(locale.D_FMT)),
        'url': url }
    else:
        msg = """\
The %(company)s Summary Reports for %(date)s are attached.
The PDF file requires Adobe Acrobat Reader to view.
""" % { 'company': company_name,
        'date': date.strftime(locale.nl_langinfo(locale.D_FMT))}

    msgRoot.attach(MIMEText(_("""\
%(company)s Reports are attached for %(date)s is attached. The pdf file requires Adobe Acrobat Reader to view.\
""") % {'company': company_name,
        'date': date.strftime(locale.nl_langinfo(locale.D_FMT))}))

    part = MIMEBase('application', "pdf")
    part.set_payload(open(file, 'rb').read())
    Encoders.encode_base64(part)
    part.add_header('Content-Disposition', 'attachment; filename="reports-%d%02d%02d.pdf"'
                    % (date.year, date.month, date.day))
    msgRoot.attach(part)

    smtp = smtplib.SMTP()
    smtp.connect('localhost')
    print "SENDER: '%s' RECEIVER: '%s'" % (sender, receiver)
    smtp.sendmail(sender, receiver, msgRoot.as_string())
    smtp.quit()

def __get_mail_info():
    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT report_email, from_address FROM settings.u_mail_settings""")

        row = curs.fetchone()

        if row:
            rv = (row[0].split(','), row[1])
        else:
            rv = None
    finally:
        conn.commit()

    return rv

def __get_branding_info():
    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT company_name FROM settings.uvm_branding_settings""")

        row = curs.fetchone()

        if row:
            rv = row[0]
        else:
            rv = 'Untangle'
    except Exception, e:
        rv = 'Untangle'
    finally:
        conn.commit()

    return rv

def __get_url():
    conn = sql_helper.get_connection()

    rv = None

    try:
        curs = conn.cursor()
        curs.execute("""\
SELECT hostname, https_port, is_hostname_public, has_public_address,
       public_ip_addr, public_port
FROM settings.u_address_settings
""")
        r = curs.fetchone()

        if r:
            hostname = r[0]
            https_port = r[1]
            is_hostname_public = r[2]
            has_public_address = r[3]
            public_ip_addr = r[4]
            public_port = r[5]

            date_str = '%s-%s-%s' %

            host = None
            port = None

            if is_hostname_public:
                host = hostname
                port = https_port
            elif has_pub
                host = public_address
                port = public_port

            if host and port:
                rv = 'https://%s:%s/reports?date=%s-%s-%s' \
                    % (host, port, date.year, date.month, date.day)

    except Exception, e:
        logging
    finally:
        conn.commit()

    return rv


