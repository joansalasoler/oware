#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Utility methods used to generate binomial hashes.
Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
"""

import os
import sys
import math


def binomial(n, r):
    """Computes the binomial number (n r)"""
    
    nf = math.factorial(n)
    rf = math.factorial(r)
    df = math.factorial(n - r)
    
    return nf / (rf * df)


def count_combinations(items, boxes):
    """Counts how many ways there are to distribute exactly
       N items into R different boxes"""
    
    n = items + boxes - 1
    r = boxes - 1
    
    return binomial(n, r)


def compute_coefficients(items, boxes):
    """Precomputes a coefficients array"""
    
    coeffs = []
    
    for zeros in range(items):
        clist = []
        
        for index in range(1, boxes):
            r = items - zeros - 1
            n = r + index
            
            c = binomial(n, r)
            clist.append(c)
        
        coeffs.append(clist)
    
    return coeffs


def ranking(array, items, boxes, coeffs):
    """Computes a rank for a given tuple"""
    
    rank = 0
    n = array[-1]
    
    for i in range(boxes - 2, -1, -1):
        if n >= items: break
        rank += coeffs[n][i]
        n += array[i]
    
    return rank;


def unranking(rank, items, boxes, coeffs):
    """Converts a rank to a tuple"""
    
    array = [0] * boxes;
    i = boxes - 2
    elem = 0
    n = 0
    
    while i >= 0 and n < items:
        value = coeffs[n][i]
        
        if rank >= value:
            rank -= value
            array[i + 1] = elem
            elem = 0
            i -= 1
        else:
            elem += 1
            n += 1
    
    array[i + 1] = items - n + elem
    
    return tuple(array)


# Example: Binomial hash function for Oware. For the hash to work
# each position must contain exactly 48 seeds. Indices 0-11 of the
# array represent an oware house and indices 12-13 correspond to
# the captured seeds by south and north

def oware_hash_sample():
    items = 48
    boxes = 14
    
    count = count_combinations(items, boxes)
    coeffs = compute_coefficients(items, boxes)
    bits = int(math.ceil(math.log(count, 2)))
    
    print "Distribute %d items into %d boxes." % (items, boxes)
    print "There are %d possible combinations." % count
    print "Range = [0-%d]" % (count - 1)
    print "Necessary bits = %d" % bits
    print "Used ranks = %d %%" % ((100.0 * count) / (2 ** bits))
    print
    
    sample = (0, 3, 3, 0, 0, 1, 0, 10, 0, 2, 1, 0, 10, 18)
    rank = ranking(sample, items, boxes, coeffs)
    unrank = unranking(rank, items, boxes, coeffs)
    
    print "sample =", sample
    print "unrank =", unrank
    print "rank =", rank


# Count the number of possible combinations of oware

def oware_combinations():
    
    a = count_combinations(48, 14)
    b = 2 * count_combinations(47, 12)
    
    print "Oware positions: %d" % (a - b)


if __name__ == "__main__":
    oware_hash_sample()
    oware_combinations()
    
