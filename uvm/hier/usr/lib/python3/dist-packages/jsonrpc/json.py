
"""
  Copyright (c) 2007 Jan-Klaas Kollhof

  This file is part of jsonrpc.

  jsonrpc is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 2.1 of the License, or
  (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this software; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
"""

from types import *
import re

CharReplacements = {
    '\t': '\\t',
    '\b': '\\b',
    '\f': '\\f',
    '\n': '\\n',
    '\r': '\\r',
    '\\': '\\\\',
    '/': '\\/',
    '"': '\\"'}

EscapeCharToChar = {
    't': '\t',
    'b': '\b',
    'f': '\f',
    'n': '\n',
    'r': '\r',
    '\\': '\\',
    '/': '/',
    '"': '"'}

StringEscapeRE = re.compile(r'[\x00-\x19\\"/\b\f\n\r\t]')
Digits = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']


class JSONEncodeException(Exception):
    def __init__(self, obj):
        Exception.__init__(self)
        self.obj = obj

    def __str__(self):
        return "Object not encodeable: %s" % self.obj


class JSONDecodeException(Exception):
    def __init__(self, message):
        Exception.__init__(self)
        self.message = message

    def __str__(self):
        return self.message


def escapeChar(match):
    c = match.group(0)
    try:
        replacement = CharReplacements[c]
        return replacement
    except KeyError:
        d = ord(c)
        if d < 32:
            return '\\u%04x' % d
        else:
            return c


def dumps(obj):
    return "".join([part for part in dumpParts(obj)])


def dumpParts(obj):
    objType = type(obj)
    if obj == None:
        yield 'null'
    elif objType is bool:
        if obj:
            yield 'true'
        else:
            yield 'false'
    elif objType is dict:
        yield '{'
        isFirst = True
        for (key, value) in list(obj.items()):
            if isFirst:
                isFirst = False
            else:
                yield ","
                decoded = None
            try:
                decoded = str(key, 'utf-8')
            except (UnicodeDecodeError, UnicodeEncodeError, TypeError):
                decoded = str(key)
            yield '"' + StringEscapeRE.sub(escapeChar, decoded) + '":'
            for part in dumpParts(value):
                yield part
        yield '}'
    elif objType == str:
        decoded = None
        try:
            decoded = str(obj, 'utf-8')
        except (UnicodeDecodeError, UnicodeEncodeError, TypeError):
            decoded = str(obj)
        yield '"' + StringEscapeRE.sub(escapeChar, decoded) + '"'
    elif objType in [tuple, list, GeneratorType]:
        yield '['
        isFirst = True
        for item in obj:
            if isFirst:
                isFirst = False
            else:
                yield ","
            for part in dumpParts(item):
                yield part
        yield ']'
    elif objType in [int, float]:
        yield str(obj)
    else:
        raise JSONEncodeException(obj)


def loads(s):
    decoded = None
    try:
        decoded = str(s, 'utf-8')
    except (TypeError, UnicodeDecodeError, UnicodeEncodeError):
        decoded = s
    stack = []
    chars = iter(decoded)
    value = None
    currCharIsNext = False

    try:
        while(1):
            skip = False
            if not currCharIsNext:
                c = next(chars)
            while(c in [' ', '\t', '\r', '\n']):
                c = next(chars)
            currCharIsNext = False
            if c == '"':
                value = ''
                try:
                    c = next(chars)
                    while c != '"':
                        if c == '\\':
                            c = next(chars)
                            try:
                                value += EscapeCharToChar[c]
                            except KeyError:
                                if c == 'u':
                                    hexCode = next(
                                        chars) + next(chars) + next(chars) + next(chars)
                                    value += chr(int(hexCode, 16))
                                else:
                                    raise JSONDecodeException(
                                        "Bad Escape Sequence Found")
                        else:
                            value += chr(ord(c))
                        c = next(chars)
                except StopIteration:
                    raise JSONDecodeException("Expected end of String")
            elif c == '{':
                stack.append({})
                skip = True
            elif c == '}':
                value = stack.pop()
            elif c == '[':
                stack.append([])
                skip = True
            elif c == ']':
                value = stack.pop()
            elif c in [',', ':']:
                skip = True
            elif c in Digits or c == '-':
                digits = [c]
                c = next(chars)
                numConv = int
                try:
                    while c in Digits:
                        digits.append(c)
                        c = next(chars)
                    if c == ".":
                        numConv = float
                        digits.append(c)
                        c = next(chars)
                        while c in Digits:
                            digits.append(c)
                            c = next(chars)
                        if c.upper() == 'E':
                            digits.append(c)
                            c = next(chars)
                            if c in ['+', '-']:
                                digits.append(c)
                                c = next(chars)
                                while c in Digits:
                                    digits.append(c)
                                    c = next(chars)
                            else:
                                raise JSONDecodeException("Expected + or -")
                except StopIteration:
                    pass
                value = numConv("".join(digits))
                currCharIsNext = True

            elif c in ['t', 'f', 'n']:
                kw = c + next(chars) + next(chars) + next(chars)
                if kw == 'null':
                    value = None
                elif kw == 'true':
                    value = True
                elif kw == 'fals' and next(chars) == 'e':
                    value = False
                else:
                    raise JSONDecodeException('Expected Null, False or True')
            else:
                raise JSONDecodeException(
                    'Expected []{}," or Number, Null, False or True')

            if not skip:
                if len(stack):
                    top = stack[-1]
                    if type(top) is list:
                        top.append(value)
                    elif type(top) is dict:
                        stack.append(value)
                    elif type(top) is str:
                        key = stack.pop()
                        stack[-1][key] = value
                    else:
                        raise JSONDecodeException(
                            "Expected dictionary key, or start of a value")
                else:
                    return value
    except StopIteration:
        raise JSONDecodeException("Unexpected end of JSON source")
