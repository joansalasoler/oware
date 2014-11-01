#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Utility methods to generate precomputed tables.
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

import pickle
import pprint

# ------------------------------------------------------------------------
# For each house returns the sequence of possible pits to sower. Skipping
# the starting house.
# ------------------------------------------------------------------------

SEED_DRILL = (
    tuple(range(1, 12) * 5)[:48], 
    tuple((range(2, 12) + range(0, 1)) * 5)[:48],
    tuple((range(3, 12) + range(0, 2)) * 5)[:48],
    tuple((range(4, 12) + range(0, 3)) * 5)[:48],
    tuple((range(5, 12) + range(0, 4)) * 5)[:48],
    tuple((range(6, 12) + range(0, 5)) * 5)[:48],
    tuple((range(7, 12) + range(0, 6)) * 5)[:48],
    tuple((range(8, 12) + range(0, 7)) * 5)[:48],
    tuple((range(9, 12) + range(0, 8)) * 5)[:48],
    tuple((range(10, 12) + range(0, 9)) * 5)[:48],
    tuple((range(11, 12) + range(0, 10)) * 5)[:48],
    tuple(range(0, 11) * 5)[:48]
)

# ------------------------------------------------------------------------
# For each landing house returns the sequence of possible pits to gather
# ------------------------------------------------------------------------

HARVESTER = (
    tuple(range(0, -1, -1)),
    tuple(range(1, -1, -1)),
    tuple(range(2, -1, -1)),
    tuple(range(3, -1, -1)),
    tuple(range(4, -1, -1)),
    tuple(range(5, -1, -1)),
    tuple(range(6, 5, -1)),
    tuple(range(7, 5, -1)),
    tuple(range(8, 5, -1)),
    tuple(range(9, 5, -1)),
    tuple(range(10, 5, -1)),
    tuple(range(11, 5, -1))
)

# ------------------------------------------------------------------------
# For each house and number of seeds returns the landing house and all the
# possible sequences of pits that make a capture impossible (because all
# the remmaining seeds will be captured)
# ------------------------------------------------------------------------

REAPER = []

# Generate all possible non-capturing combinations

def xselections(items, length):
    if length == 0:
        yield []
    else:
        for n in xrange(len(items)):
            for selection in xselections(items, length - 1):
                yield [items[n]] + selection

def xboard_positions(items, length):
    for combination in xselections(items, length):
        yield tuple(combination + [0] * (6 - length))

positions = [dict(), dict(), dict()]

for n in range(3):
    for m in range(6):
        positions[n][m] = dict()

for n in range(6):
    for board in xboard_positions((1, 2), n + 1):
        positions[0][n][board] = None
    for board in xboard_positions((0, 1), n + 1):
        positions[1][n][board] = None
    positions[2][n][(0, 0, 0, 0, 0, 0)] = None

# For each house generate a dict with each possible number of seeds that
# would end sowing on an oponent's house. Add a tuple to the dict with
# the landing house and all non-capturing positions.

for move in range(12):
    sdict = {}
    for seeds in range(1, 34):
        last = SEED_DRILL[move][seeds - 1]
        if not ((6 > last) ^ (move > 5)):
            n = last
            if last > 5: n -= 6
            
            if seeds < 12:
                sdict[seeds] = (last, positions[0][n])
            elif last == 11 or last == 5:
                if seeds < 23:
                    sdict[seeds] = (last, positions[1][n])
                else:
                    sdict[seeds] = (last, positions[2][n])
            else:
                sdict[seeds] = (last, {})
    REAPER.append(sdict)

REAPER = tuple(REAPER)

# ------------------------------------------------------------------------
# Pickle the machinery
# ------------------------------------------------------------------------

output = open('machinery.pkl', 'wb')
pickle.dump(SEED_DRILL, output)
pickle.dump(HARVESTER, output)
pickle.dump(REAPER, output)
output.close()
