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
from pyes.es import ES
from pyes.query import (FilteredQuery, MatchAllQuery, TextQuery, 
                        TermQuery, BoolQuery, WildcardQuery, StringQuery)
from pyes.filters import TermFilter
import jsonlib2 as json
import traceback

_TYPES = ['yara','clamav','pehash', 'peframe']

_FIELDS = {
    'yara': ['matches.namespace', 'matches.meta.description'],
    'clamav': ['results'],
    'pehash': ['pe_hash', 'filetype'],
    'peframe': ['info.compile_time', 'peid.packer', 'suspicious.api_alert', 'suspicious.api_antidebug', 'meta', 'sections.sections.suspicious', 'sections.sections.md5']
}

SPANS = {
    'yara': 1,
    'clamav': 2,
    'pehash': 3,
    'peframe':4
}

class FacetResult:
    def __init__(self, term, count):
        self.term = term
        self.count = count
        
class Facet:
    spanlength = 4

    def __init__(self, term, span):
        self.term = term
        self.results = []
        self.span = span*self.spanlength

    def add(self, row):
        self.results.append(FacetResult(row['term'],row['count']))

class Analysis:
    spanlength = 4

    def __init__(self, name, span):
        self.span = span*self.spanlength
        self.facets = []
        self.name = name

    def add(self, facet):
        self.facets.append(facet)
        
class TextSearchResult:
    def __init__(self, r):
        self.name = r._meta["id"]
        self.data = json.dumps(r)
        self.type = r._meta["type"]
        self.record = r

def connect_to_db():
    eshosts = settings.ES_SETTINGS['ES_HOSTS']
    index = settings.ES_SETTINGS['INDEX']
    timeout = settings.ES_SETTINGS.get('TIMEOUT', 60.0)

    # build query
    return ES(eshosts, timeout=timeout)

def add_filters(filters):
    # filters
    flist = []
    for field, term in filters:
        flist.append(TermFilter(field,term))

    if flist:
        f = ANDFilter(flist)
        q = FilteredQuery(q, f)
    else:
        q = MatchAllQuery()
        
    return q
    
def facet_search(_type='yara', filters=[]):
    index = settings.ES_SETTINGS['INDEX']
    facet_size = settings.ES_SETTINGS.get('FACET_SIZE',100)
    conn = connect_to_db()

    q = add_filters(filters)
    q = q.search(size=0)

    # facets
    fields = _FIELDS.get(_type, [])
    for field in fields:
        q.facet.add_term_facet(field, size=facet_size)

    resultset = conn.search(query=q, 
                            order="count", 
                            indices=index, 
                            doc_types=[_type])

    ret = Analysis(_type, SPANS[_type])
    for span, field in enumerate(fields):
        facet = Facet(field,span)
        for r in resultset.facets[field]['terms']:
            facet.add(r)
        ret.add(facet)
        
    return ret

'''
    get_query(s)

        s - a string that can be a mixture of:
            name:value
            name:"value with spaces"
            some free text

        returns a Query that combines the input queries in an intelligent manner:
          * name:value and name:"vals with spaces" are converted into TermQuery's
          * "some free text" is converted into a TextQuery
          * These queries are combined using a BoolQuery
'''
def get_query(s):
    import re
    queries = []

    i = 0
    freetext = ""
    for mat in re.finditer(r'(?P<name>\S+):(?P<value>"[^"]+"|\S+)\s*', s):
        freetext += s[i:mat.start()]
        i = mat.end()
        q = mat.groupdict()
        value = q['value'].strip('"')
        if value.endswith("*"):
            queries.append(WildcardQuery(field=q['name'], value=value))
        else:
            queries.append(TermQuery(field=q['name'], value=value))
    freetext += s[i:]
    freetext = freetext.strip()
    if freetext:
        if freetext == '*':
            queries.append(MatchAllQuery())
        else:
            queries.append(TextQuery("_all", freetext, operator='and'))

    if len(queries) == 1:
        return queries[0]
    else:
        q = BoolQuery()
        for query in queries:
            q.add_must(query)
        return q

def free_text_search(text, result_count, doc_types=None):
    index = settings.ES_SETTINGS['INDEX']
    facet_size = settings.ES_SETTINGS.get('FACET_SIZE',100)
    conn = connect_to_db()

    try:
        q = get_query(text).search()
        q.size = int(result_count)
        resultset = conn.search(q, indices=index, doc_types=doc_types)
        ret = []
        for r in resultset:
            ret.append(TextSearchResult(r))
        return {'count':resultset.count(), 'results':ret}
    except Exception, ex:
        print "Exception",ex,traceback.format_exc()
        return {'count':0, 'results':[], 'error': ex}

def get_details(event_id, search_types=_TYPES, field='filename'):
    index = settings.ES_SETTINGS['INDEX']
    conn = connect_to_db()
    mget_params = [(index, _type, event_id) for _type in search_types]
    resultset = conn.mget(ids=mget_params)
    result = {}
    for r in resultset:
        if len(r):
            result[r._meta["type"]] = r
    return result
