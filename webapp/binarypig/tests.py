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


"""
run "manage.py test".
"""
from django.test import TestCase
from django.test.client import Client
from django.conf import settings
import query
import jsonlib2 as json
from pyes.es import ES
import os

class SearchTest(TestCase):
    """search searching using elasticsearch"""
    def setUp(self):
        """reads the "real" elasticsearch settings 
        from SOURCE/elasticsearch/settings.json and uses it to configure
        an index for the unittests"""
        self.es_settings = {'ES_HOSTS':['localhost:9200',],
                            'INDEX':"unittest-binarypig",
                            'FACET_SIZE':999999}
        query.settings.ES_SETTINGS = self.es_settings

        index_template_fn = os.path.join(settings.SOURCE_ROOT, 
                                         'elasticsearch', 
                                         'settings.json')
        self.index_settings = json.loads(file(index_template_fn).read())
        conn = ES(self.es_settings['ES_HOSTS'])
        self.createIndex(conn)

    def tearDown(self):
        """delete the index"""
        conn = ES(self.es_settings['ES_HOSTS'])
        self.deleteIndex(conn)

    def createIndex(self, conn):
        """create the index and add docs for testing"""
        index = self.es_settings['INDEX']
        settings = self.index_settings

        try:
            conn.delete_index(index)
        except:
            pass
        conn.create_index(index,settings['settings'])
        for doc_type in settings['mappings']:
            conn.put_mapping(doc_type=doc_type,
                             mapping=settings['mappings'][doc_type],
                             indices=[index])
        clamav = {
            'filename': "0e61bec5c12bf098118195c47361d16c",
            'results': "Trojan.VB-55934",
            'timeout': "false"
        }

        pehash = {
            'filename': "0e61bec5c12bf098118195c47361d16c",
            'md5': "0e61bec5c12bf098118195c47361d16c",
            'pe_hash': "993bf16df440e5d8f3de866e4829764b",
            'sha1': "33b1478ca663d387fd5df5e9190a3a3cd0ceb0b0",
            'sha256': "79df4e0c86d8497746a0656758f975d34f576782ea83940268434f55b764650f",
            'sha512': "3f605e1a89df761fa6a2916ef69508af5b076ebe2e652c5adeab2711a3ae87d3dbf4d79549b126cee60a75e63d0429752ab392386802fd0492b206fc78288f7d",
            'timeout': "false"
            }

        yara = {
            'filename': "0e61bec5c12bf098118195c47361d16c",
            'matches': [
                {'meta': {
                    'description': "dUP v2.x Patcher --> www.diablo2oo2.cjb.net"},
                'namespace': "userdb_jclausing",
                'rule': "_dUP_v2x_Patcher__wwwdiablo2oo2cjbnet_"
            }]
        }

        # add docs and refresh the index
        for doc, _type in ((yara, 'yara'),
                           (clamav, 'clamav'),
                           (pehash, 'pehash')):
            conn.index(doc=doc, index=index, doc_type=_type, id=doc['filename'])
        conn.refresh(index)
        
    def deleteIndex(self, conn):
        index = self.es_settings['INDEX']
        conn.delete_index_if_exists(index)

    def testFacet(self):
        #"""Tests the facets results"""
        analysis = query.facet_search("clamav")
        self.assertEquals(len(analysis.facets), 1)
        for facet in analysis.facets:
            self.failUnless(len(facet.results),0)

    def testFreeTextSearch(self):
        #"""Tests the free text page with query parms"""
        results = query.free_text_search("troj", 50)
        cnt = results['count']
        res = results['results']
        self.assertEquals(cnt,1)
        self.assertEquals(len(res),1)
        self.assertEquals(res[0].name,'0e61bec5c12bf098118195c47361d16c')

    def testGetEvent(self):
        #"""Test event detail view"""
        result = query.get_details('0e61bec5c12bf098118195c47361d16c')
        for _type in ('yara','clamav','pehash'):
            self.failUnless(result.has_key(_type))
