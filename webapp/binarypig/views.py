#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
#  Copyright 2013 Endgame Inc.


from django.conf import settings
from django.http import HttpResponse
from django.shortcuts import render
from query import _TYPES, facet_search, free_text_search, get_details
import jsonlib2 as json
import traceback

BREAK = ["__break__", "__break__", "__break__"]

def facet(request, template='facet.html'):
    context = {}
    context['analysis_types'] = []
    _type = request.GET.get('search_type', 'yara')
    try:
        context['analysis_types'].append(facet_search(_type))
    except Exception, ex:
        print "Exception",ex,traceback.format_exc()
        context['analysis_types'] = []
        context['error'] = ex

    return render(request, template, context)

def render_transform_keyval(name, value, ret, basename=None):
    if basename:
        query_fieldname = basename+"."+name
    else:
        query_fieldname = name

    if isinstance(value, list):
        if len(value) > 0:
            for item in value:
                render_transform_keyval(name, item, ret, basename)
    elif isinstance(value, dict):
        if len(value) > 0:
            for n,v in sorted(value.items()):
                render_transform_keyval(n, v, ret, query_fieldname)
            if ret[-1] != BREAK:
                ret.append(BREAK)
    else:
        ret.append([name, value, query_fieldname])

def render_transform(rec):
    ret = []
    for name, value in sorted(rec.items()):
        render_transform_keyval(name, value, ret)
    return ret

def remove_breaks(ret):
    return [x for x in ret if x != BREAK]

def search(request, template='search.html'):
    term = request.GET.get('term')
    doctype = request.GET.get('search_type')
    max_res = request.GET.get('max', 1000)
    context = {}
    if term:
        results = free_text_search(term, max_res, doctype)
        context['search_type'] = doctype
        context['count'] = results['count']
        context['results'] = [{'name':r.name, 'type':r.type, 'record':r.record, 'render': remove_breaks(render_transform(r.record))[0:10]} for r in results['results']]
        context['term'] = term
        if results.has_key('error'):
            context['error'] = results['error']

    return render(request, template, context)

def detail(request, event_id, template='detail.html'):
    """Show detail in a table"""
    result = get_details(event_id)
    context = {}
    context['event_id'] = event_id
    context['data'] = []

    for name in sorted(result.keys()):
        context['data'].append((name, render_transform(result[name])))
    return render(request, template, context)

def detail_json(request, event_id):
    """Show raw JSON doc"""

    if request.GET.get('type'):
        types = [request.GET.get('type')]
    else:
        types = _TYPES

    result = get_details(event_id, types)
    return HttpResponse(json.dumps(result, indent=2, sort_keys=True), 
                        mimetype='application/json; charset=UTF-8')


def _showFieldRows(data, skip=['filename','timeout']):
    rows = []
    for key in sorted(data.keys()):
        if key in skip:
            continue
        row = (key, str(data[key]))
        rows.append(row)
    
    return rows

