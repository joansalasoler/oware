#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Copyright (C) Joan Sala Soler, 2014 <contact@joansala.com>

This program is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
"""

import sys
import threading


class Engine(object):
    """A really simple game engine"""
    
    __NAME = 'Prototype'
    __AUTHOR = 'Joan Sala Soler'
    
    __INFINITY = 1000
    __MIN_DEPTH = 4
    
    __EXACT = 0
    __LOWER = 1
    __UPPER = 2
    
    
    def __init__(self, game):
        """Initializes this engine"""
        
        self._game = game
        self._depth = 126
        self._move_time = 2.0
        self._aborted = False
        self._line = dict()
        
        
    def get_name(self):
        """Returns this engine name"""
        
        return Engine.__NAME
        
        
    def get_author(self):
        """Returns this engine author"""
        
        return Engine.__AUTHOR
        
        
    def get_depth(self):
        """Returns the maximum search depth"""
        
        return self._depth
        
        
    def get_move_time(self):
        """Returns the maximum allowed time per search"""
        
        return self._move_time
        
        
    def set_depth(self, depth):
        """Sets the maximum search depth"""
        
        self._depth = depth < Engine.__MIN_DEPTH \
            and Engine.__MIN_DEPTH \
            or int(depth) + int(depth) % 2
        
        
    def set_move_time(self, move_time):
        """Sets the maximum allowed time per search"""
        
        self._move_time = move_time
        
        
    def stop_computation(self):
        """Aborts the current computation"""
        
        self._aborted = True
        
        
    def compute_best_move(self, board, turn, positions = tuple()):
        """Computes a best move for the specified position"""
        
        # Start a move time countdown
        
        self._aborted = False
        
        t = threading.Timer(
            self._move_time,
            self.stop_computation
        )
        
        t.start()
        
        # Fill the opened line of play
        
        self._line = dict.fromkeys(positions)
        moves = tuple(self._game.xlegal_moves(board, turn))
        
        # Iterative deepening search for a best move
        
        depth = Engine.__MIN_DEPTH
        alpha = -Engine.__INFINITY
        
        while not self._aborted:
            best_move = moves[0]
            best_score = -Engine.__INFINITY
            
            for cmove in moves:
                cboard = self._game.compute_board(board, cmove)
                
                # Perform a negamax search
                
                score = -self._search(
                    cboard,
                    -turn,
                    alpha,
                    -best_score,
                    depth
                )
                
                # Make sure the minimal depth is reached
                
                if self._aborted and depth > Engine.__MIN_DEPTH:
                    if last_score >= best_score:
                        best_move = last_move
                    break
                
                # Remember best moves and score
                
                if score > best_score:
                    best_move = cmove
                    best_score = score
            
            if depth >= self._depth:
                break
            
            # She's heading for the disco…
            
            last_move = best_move
            last_score = best_score
            depth += 2
        
        t.cancel()
        
        return best_move
        
        
    def _search(self, board, turn, alpha, beta, depth):
        """Recursive search of the best moves"""
        
        if self._aborted: return -Engine.__INFINITY
        
        # If the node is terminal, return its value
        
        position = (board, turn)
        
        if self._game.is_end(board, turn) or position in self._line:
            return turn * self._game.get_final_score(board)
        
        if depth == 0:
            return turn * self._game.get_score(board)
        
        # Recursive search for a best move
        
        self._line[position] = True
        
        for cmove in self._game.xlegal_moves(board, turn):
            cboard = self._game.compute_board(board, cmove)
            
            score = -self._search(
                cboard,
                -turn,
                -beta,
                -alpha,
                depth - 1
            )
            
            if score >= beta:
                alpha = beta
                break
            
            if score > alpha:
                alpha = score
        
        del self._line[position]
        
        return alpha

