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

import os
import pickle
import re


class Oware(object):
    """Oware Abapa game logic."""
    
    SOUTH       =  1
    NORTH       = -1
    DRAW        =  0
    NULL_MOVE   = -1
    
    __file = open(os.path.join(
        os.path.dirname(__file__), 'machinery.pkl'), 'rb')
    
    _EMPTY_ROW = (0, 0, 0, 0, 0, 0)
    _EMPTY_ROWL = [0, 0, 0, 0, 0, 0]
    _SEED_DRILL = pickle.load(__file)
    _HARVESTER = pickle.load(__file)
    _REAPER = pickle.load(__file)
    
    __file.close(); del __file
    
    
    @staticmethod
    def get_initial_board():
        """
        Builds the initial board position and returns it.
        The board is represented by a tuple of integers, the
        last two being the score of each contrincant.
        """
        
        return (4,) * 12 + (0, 0)
    
    
    @staticmethod
    def get_final_board(board):
        """
        Performs the necessari computations on the board after
        the endgame position and returns the resulting board
        """
        
        # If the game ended because a player captured more than 24
        # seeds or each player captured 24 seeds, the final board
        # is the current board. Otherwise, if the game ended unexpectedly
        # (because of a move repetition or a lack of legal moves), each
        # player gathers the remaining seeds on their side of the board.
        
        if board[12] > 24 or board[13] > 24:
            return board
        
        if board[12] == board[13] == 24:
            return board
        
        return (0,) * 12 + (
            sum(board[0:6], board[12]),
            sum(board[6:12], board[13])
        )
    
    
    @staticmethod
    def get_winner(board, turn):
        """
        Checks if a player has won the match and returns 1, -1 or 0
        depending on who has won. Returns 1 for south, -1 for north
        and zero for a draw or None if the match hasn't ended yet.
        """
        
        # A player captured more than 24 seeds
        
        if board[12] > 24:
            return Oware.SOUTH
        elif board[13] > 24:
            return Oware.NORTH
        
        # No legal move can be performed with the current position.
        
        if not Oware.has_legal_moves(board, turn):
            cboard = Oware.get_final_board(board)
            if cboard[12] > cboard[13]:
                return Oware.SOUTH
            elif cboard[12] < cboard[13]:
                return Oware.NORTH
            else:
                return Oware.DRAW
        
        return None
        
        
    @staticmethod
    def is_capture(board, move):
        """Returns true if the move performs at least one capture"""
        
        if board[move] in Oware._REAPER[move]:
            (last, positions) = Oware._REAPER[move][board[move]]
            
            if board[last] > 2:
                return False
            
            if (board[last] == 0) ^ (board[move] < 12) \
            or (11 < board[move] < 23 and board[last] == 1):
                if move < 6:
                    if board[6:12] in positions:
                        return False
                else:
                    if board[0:6] in positions:
                        return False
                return True
        
        return False


    @staticmethod
    def is_end(board, turn):
        """Checks if the position is an endgame position"""
        
        # A player captured more than 24 seeds
        
        if board[12] > 24 or board[13] > 24:
            return True
        
        # No legal move can be performed
        
        if Oware.has_legal_moves(board, turn):
            return False
        
        return True
        
        
    @staticmethod
    def has_legal_moves(board, turn):
        """
        Returns True if the specified player has at least one legal
        move for the board position
        """
        
        if turn == Oware.SOUTH:
            if board[0:6] != Oware._EMPTY_ROW:
                if board[6:12] != Oware._EMPTY_ROW:
                    return True
                
                for house in (5, 4, 3, 2, 1, 0):
                    if 0 < board[house] > 5 - house:
                        return True
        else:
            if board[6:12] != Oware._EMPTY_ROW:
                if board[0:6] != Oware._EMPTY_ROW:
                    return True
                
                for house in (11, 10, 9, 8, 7, 6):
                    if 0 < board[house] > 11 - house:
                        return True
        
        return False
        
        
    @staticmethod
    def get_score(board):
        """Evaluates de position and returns its score"""
        
        return (board[12] - board[13])
    
    
    @staticmethod
    def get_final_score(board):
        """
        Computes the exact final value for an endgame position.
        Return the maximum possible value if south wins, the minimum
        possible value if north wins or zero in case of a draw
        """
        
        # The game ended because of captured seeds
        
        if board[12] > 24:
            return 10000
        
        if board[13] > 24:
            return -10000
        
        # The game ended because of a move repetition or because no
        # legal moves could be performed. Each player captures all
        # seeds on his side of the board
        
        score = sum(board[:6], board[12])
        
        if score > 24:
            return 10000
        elif score < 24:
            return -10000
        
        return 0
        
        
    @staticmethod
    def compute_board(board, move):
        """
        Executes a move on the board and returns the resulting
        board
        """
        
        # Grand Slam moves are legal moves but the player to move does
        # not capture any of the oponent's seeds.
        
        new_board = list(board)
        new_board[move] = 0
        
        # Sow
        
        for house in Oware._SEED_DRILL[move][:board[move]]:
            new_board[house] += 1
        
        # Gather
        
        if 4 > new_board[house] > 1:
            if move < 6 and house > 5:
                board_copy = new_board[:]
                for house in Oware._HARVESTER[house]:
                    if not 4 > board_copy[house] > 1:
                        break
                    board_copy[12] += board_copy[house]
                    board_copy[house] = 0
                if board_copy[6:12] != Oware._EMPTY_ROWL:
                    return tuple(board_copy)
            elif move > 5 and house < 6:
                board_copy = new_board[:]
                for house in Oware._HARVESTER[house]:
                    if not 4 > board_copy[house] > 1:
                        break
                    board_copy[13] += board_copy[house]
                    board_copy[house] = 0
                if board_copy[0:6] != Oware._EMPTY_ROWL:
                    return tuple(board_copy)
        
        return tuple(new_board)
        
        
    @staticmethod
    def xlegal_moves(board, turn):
        """Returns all the legal moves sorted by importance"""
        
        # Illegal moves are those which don't reach the opponent's
        # side when the opponent has all pits empty.
        
        # In order to improve prunning captures are yield first
        # followed by pits that could be captured.
        
        if turn == Oware.SOUTH:
            # Captures
            
            for move in (5, 4, 3, 2, 1, 0):
                if 0 < board[move] > 5 - move \
                and Oware.is_capture(board, move):
                    yield move
            
            # Vulnerable pits and other moves
            
            if board[6:12] != Oware._EMPTY_ROW:
                for move in (0, 1, 2, 3, 4, 5):
                    if 0 < board[move] < 3 \
                    and not Oware.is_capture(board, move):
                        yield move
                
                for move in (0, 1, 2, 3, 4, 5):
                    if board[move] > 2 \
                    and not Oware.is_capture(board, move):
                        yield move
            else:
                for move in (0, 1, 2, 3, 4, 5):
                    if 0 < board[move] > 5 - move \
                    and not Oware.is_capture(board, move):
                        yield move
        else:
            # Captures
            
            for move in (11, 10, 9, 8, 7, 6):
                if 0 < board[move] > 11 - move \
                and Oware.is_capture(board, move):
                    yield move
            
            # Vulnerable pits and other moves
            
            if board[0:6] != Oware._EMPTY_ROW:
                for move in (6, 7, 8, 9, 10, 11):
                    if 0 < board[move] < 3 \
                    and not Oware.is_capture(board, move):
                        yield move
                
                for move in (6, 7, 8, 9, 10, 11):
                    if board[move] > 2 \
                    and not Oware.is_capture(board, move):
                        yield move
            else:
                for move in (6, 7, 8, 9, 10, 11):
                    if 0 < board[move] > 11 - move \
                    and not Oware.is_capture(board, move):
                        yield move


    @staticmethod
    def xdisruptive_moves(board, turn):
        """Returns all the legal moves which perform a capture"""
        
        if turn == Oware.SOUTH:
            for move in (5, 4, 3, 2, 1, 0):
                if 0 < board[move] > 5 - move \
                and Oware.is_capture(board, move):
                    yield move
        else:
            for move in (11, 10, 9, 8, 7, 6):
                if 0 < board[move] > 11 - move \
                and Oware.is_capture(board, move):
                    yield move
    
    
    @staticmethod
    def to_board_notation(board, turn):
        """Converts a board tuple to its notation"""
        
        notation = []
        
        for house in board:
            notation.append(str(house))
        
        notation.append(turn == Oware.SOUTH and 'S' or 'N')
        
        return '-'.join(notation)
    
    
    @staticmethod
    def to_move_notation(move):
        """Converts a move identifier to its notation"""
        
        if type(move) == int:
            if 0 <= move <= 5:
                return chr(65 + move)
            elif 6 <= move <= 11:
                return chr(91 + move)
        
        raise ValueError("Not a valid move identifier")
    
    
    @staticmethod
    def to_moves_notation(moves):
        """Converts a move tuple to its notation"""
        
        notation = []
            
        for move in moves:
            n = Oware.to_move_notation(move)
            notation.append(n)
        
        return ''.join(notation)
    
    
    @staticmethod
    def to_move(notation):
        """Converts a notation to a move identifier"""
        
        if type(notation) == str and len(notation) == 1:
            if 65 <= ord(notation) <= 70:
                return ord(notation) - 65
            elif 97 <= ord(notation) <= 102:
                return ord(notation) - 91
        
        raise ValueError("Not a valid move notation")
    
    
    @staticmethod
    def to_moves(notation):
        """Converts a moves notation to a move identifiers tuple"""
        
        pattern = '([A-F]([a-f][A-F])*[a-f]?)|([a-f]([A-F][a-f])*[A-F]?)$'
        
        if re.match(pattern, notation) is None:
            raise ValueError("Moves notation is not valid")
        
        return tuple(Oware.to_move(n) for n in notation)
    
    
    @staticmethod
    def to_position(notation):
        """Converts a position notation to a board tuple and turn"""
        
        pattern = '((?:[1-4]?[0-9]-){14})(S|N)$'
        match = re.match(pattern, notation)
        
        if match is None:
            raise ValueError("Position notation is not valid")
        
        turn = Oware.SOUTH
        board = []
        
        if 'N' == match.group(2):
            turn = Oware.NORTH
        
        houses = match.group(1).split('-')
        houses.pop()
        
        for seeds in houses:
            board.append(int(seeds))
        
        return (tuple(board), turn)
    
