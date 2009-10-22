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
import reports
import reports.i18n_helper
import reports.sql_helper as sql_helper
import tempfile
import shutil

from email import Encoders
from email.MIMEBase import MIMEBase
from email.MIMEImage import MIMEImage
from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext

def mail_reports(start_date, end_date, file, mail_reports):
    if False: # XXX read setting
        zip_dir = __make_zip_file(start_date, end_date, mail_reports)
        zip_file = '%s/reports.zip' % zip_dir
    else:
        zip_file = None

    receivers, sender = __get_mail_info()
    company_name = __get_branding_info()

    url = __get_url(end_date)
    report_users = __get_report_users()

    for receiver in receivers:
        has_web_access = receiver in report_users
        mail(file, zip_file, sender, receiver, end_date, company_name,
             has_web_access, url)

    if zip_file:
        shutil.rmtree(zip_dir)

def mail(file, zip_file, sender, receiver, date, company_name, has_web_access,
         url):
    msgRoot = MIMEMultipart('related')
    msgRoot['Subject'] = _('New %s Reports Available') % company_name
    msgRoot['From'] = sender
    msgRoot['To'] = receiver

    if has_web_access and url:
        msg = _("""\
The %(company)s Summary Reports for %(date)s are attached.
The PDF file requires Adobe Acrobat Reader to view.

For more in-depth online reports
<a href='%(url)s'>click here to view Online %(company)s Reports</a>
""") % { 'company': company_name,
         'date': date.strftime(locale.nl_langinfo(locale.D_FMT)),
         'url': url }
    else:
        msg = _("""\
The %(company)s Summary Reports for %(date)s are attached.
The PDF file requires Adobe Acrobat Reader to view.
""") % { 'company': company_name,
        'date': date.strftime(locale.nl_langinfo(locale.D_FMT))}

    msgRoot.attach(MIMEText(msg))

    part = MIMEBase('application', "pdf")
    part.set_payload(open(file, 'rb').read())
    Encoders.encode_base64(part)
    part.add_header('Content-Disposition', 'attachment; filename="reports-%d%02d%02d.pdf"'
                    % (date.year, date.month, date.day))
    msgRoot.attach(part)

    if zip_file:
        part = MIMEBase('application', "zip")
        part.set_payload(open(zip_file, 'rb').read())
        Encoders.encode_base64(part)
        part.add_header('Content-Disposition', 'attachment; filename="reports.zip"')
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

def __get_url(date):
    rv = None

    try:
        f = open( "%s/usr/share/untangle/conf/networking.sh" % PREFIX )
        for line in f:
            if ( line.startswith( "UVM_PUBLIC_URL=" )):
                public_host = line.replace( "UVM_PUBLIC_URL=", "" )
                public_host = public_host.replace( "\"", "" )
                public_host = public_host.strip()
                rv = 'https://%s/reports?date=%s-%s-%s' \
                     % ( public_host, 2009, 11, 22 )
                break

    except Exception, e:
        logging.warn('could not get hostname', exc_info=True)
    finally:
        conn.commit()

    return rv

def __get_report_users():
    rv = set()

    conn = sql_helper.get_connection()
    try:
        curs = conn.cursor()
        curs.execute('SELECT u_user FROM settings.u_user WHERE reports_access')

        for r in curs.fetchall():
            rv.add(r[0])

    except Exception, e:
        logging.warn('could not get hostname', exc_info=True)
    finally:
        conn.commit()

    return rv

def __make_zip_file(start_date, end_date, mail_reports):
    tmp_dir = tempfile.mkdtemp()
    base_dir = '%s/reports' % tmp_dir
    os.mkdir(base_dir)

    for r in mail_reports:
        report_name = r.name
        report_dir = '%s/%s' % (base_dir, report_name)
        os.mkdir(report_dir)

        for s in r.sections:
            if isinstance(s, reports.DetailSection):
                filename = '%s/%s.csv' % (report_dir, s.name)
                s.write_csv(filename, start_date, end_date)

    os.system("""\
pushd %s;
zip -r reports.zip ./reports;
popd""" % tmp_dir)

    return tmp_dir
