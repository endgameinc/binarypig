#!/usr/bin/python

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

import copy
import sys
import hashlib
import binascii
import bz2
from bitstring import BitStream, BitArray
import math
import pefile
import anyjson

def roundUp(num):
    winPageBoundary = 4096.
    return int(math.ceil(num/winPageBoundary) * winPageBoundary)


def peHash(pe_data):
    pe = pefile.PE(data=pe_data)
    characteristics = BitArray(uint=pe.FILE_HEADER.Characteristics, length=16)
    subsystem = BitArray(uint=pe.OPTIONAL_HEADER.Subsystem, length=16)

# Rounded up to page boundary size
    sizeOfStackCommit = BitArray(uint=roundUp(pe.OPTIONAL_HEADER.SizeOfStackCommit), length=32)
    sizeOfHeapCommit = BitArray(uint=roundUp(pe.OPTIONAL_HEADER.SizeOfHeapCommit), length=32)

#sort these:
    sections = [];
    for section in pe.sections:
#calculate kolmogrov:
        data = pe.get_memory_mapped_image()[section.VirtualAddress: section.VirtualAddress + section.SizeOfRawData]
        compressedLength = len(bz2.compress(data))

        kolmogrov = 0
        if (section.SizeOfRawData > 0):
            kolmogrov = int(math.ceil((compressedLength/section.SizeOfRawData) * 7.))

        sections.append((section.Name, BitArray(uint=section.VirtualAddress, length=32),BitArray(uint=section.SizeOfRawData, length=32),BitArray(uint=section.Characteristics, length=32),BitArray(uint=kolmogrov, length=16)))
    hash = characteristics[0:8] ^ characteristics[8:16]
    characteristics_hash = characteristics[0:8] ^ characteristics[8:16]
    hash.append(subsystem[0:8] ^ subsystem[8:16])
    subsystem_hash = subsystem[0:8] ^ subsystem[8:16]
    hash.append(sizeOfStackCommit[8:16] ^ sizeOfStackCommit[16:24] ^ sizeOfStackCommit[24:32])
    stackcommit_hash = sizeOfStackCommit[8:16] ^ sizeOfStackCommit[16:24] ^ sizeOfStackCommit[24:32]
    hash.append(sizeOfHeapCommit[8:16] ^ sizeOfHeapCommit[16:24] ^ sizeOfHeapCommit[24:32])
    heapcommit_hash = sizeOfHeapCommit[8:16] ^ sizeOfHeapCommit[16:24] ^ sizeOfHeapCommit[24:32]

    sections_holder = []
    for section in sections:
        section_copy = copy.deepcopy(section)
        section_hash = section_copy[1]
        section_hash.append(section_copy[2])
        section_hash.append(section_copy[3][16:24] ^ section_copy[3][24:32])
        section_hash.append(section_copy[4])
        hash.append(section[1])
        hash.append(section[2])
        hash.append(section[3][16:24] ^ section[3][24:32])
        hash.append(section[4])

        sections_holder.append(str(section_hash))

    return hashlib.md5(str(hash)).hexdigest()
    
pe_data = open(sys.argv[1], 'rb').read()
print anyjson.serialize({
    'pe_hash': peHash(pe_data), 
    'md5':    hashlib.md5(pe_data).hexdigest(), 
    'sha1':   hashlib.sha1(pe_data).hexdigest(), 
    'sha256': hashlib.sha256(pe_data).hexdigest(), 
    'sha512': hashlib.sha512(pe_data).hexdigest()
    }),
