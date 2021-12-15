package com.joansala.test.engine;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.util.suites.Suite;


/**
 *
 */
@DisplayName("Board interface contract")
public interface BoardContract {

    /**
     * Instantiate a new board object.
     */
    Board newInstance();


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("diagram to board peserves class type")
    default void ToBoardIsOfSameClassType(Suite suite) {
        Board instance = newInstance();
        String diagram = suite.diagram();
        Board board = instance.toBoard(diagram);
        assertEquals(instance.getClass(), board.getClass());
    }


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("diagram to board is commutative")
    default void ToBoardConversionIsCommutative(Suite suite) {
        Board instance = newInstance();
        String diagram = suite.diagram();
        Board board = instance.toBoard(diagram);
        String converted = board.toDiagram();
        assertEquals(diagram, converted);
    }


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("notation to moves is not a blank array")
    default void ToMovesIsNotBlank(Suite suite) {
        Board instance = newInstance();
        String notation = suite.notation();
        int[] moves = instance.toMoves(notation);
        assertNotNull(moves, "moves array is null");
        assertTrue(moves.length > 0, "moves array is empty");
    }


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("notation to moves is commutative")
    default void ToMovesIsCommutative(Suite suite) {
        Board instance = newInstance();
        String notation = suite.notation();
        int[] moves = instance.toMoves(notation);
        String converted = instance.toNotation(moves);
        int[] result = instance.toMoves(converted);
        assertArrayEquals(moves, result);
    }


    @ParameterizedTest()
    @NullSource @EmptySource @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("notation to moves returns empty array")
    default void ToMovesReturnsEmptyArray(String notation) {
        Board instance = newInstance();
        int[] moves = instance.toMoves(notation);
        assertTrue(moves.length == 0, "moves not empty");
    }


    @ParameterizedTest()
    @NullSource @EmptySource @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("diagram to board throws runtime exception")
    default void ToBoardThrowsRuntimeException(String notation) {
        Board instance = newInstance();
        assertThrows(RuntimeException.class, () -> {
            instance.toBoard(notation);
        });
    }


    @ParameterizedTest()
    @NullSource @EmptySource @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("move to notation throws runtime exception")
    default void ToMoveThrowsRuntimeException(String notation) {
        Board instance = newInstance();
        assertThrows(RuntimeException.class, () -> {
            instance.toMove(notation);
        });
    }


    @ParameterizedTest()
    @ValueSource(ints = { Game.NULL_MOVE, Integer.MIN_VALUE })
    @DisplayName("move to coordinates throws runtime exception")
    default void ToCoordinateThrowsRuntimeException(int move) {
        Board instance = newInstance();
        assertThrows(RuntimeException.class, () -> {
            instance.toCoordinates(move);
        });
    }


    @ParameterizedTest()
    @ValueSource(ints = { Game.NULL_MOVE, Integer.MIN_VALUE })
    @DisplayName("move to notation throws runtime exception")
    default void ToNotationThrowsRuntimeException(int move) {
        Board instance = newInstance();
        assertThrows(RuntimeException.class, () -> {
            int[] moves = { move };
            instance.toNotation(moves);
        });
    }
}
