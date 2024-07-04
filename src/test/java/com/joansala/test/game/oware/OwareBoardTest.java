package com.joansala.test.game.oware;

import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import com.joansala.engine.Board;
import com.joansala.test.engine.BoardContract;
import com.joansala.game.oware.OwareBoard;
import com.joansala.util.suites.Suite;
import com.joansala.util.suites.SuiteReader;


@DisplayName("Oware board")
public class OwareBoardTest implements BoardContract {

    /** Test suite file path */
    private static String SUITE_PATH = "oware-bench.suite";


    /**
     * {@inheritDoc}
     */
    @Override
    public Board newInstance() {
        return new OwareBoard();
    }


    /**
     * Stream of game suites to test.
     */
    public static Stream<Suite> suites() throws Exception {
        SuiteReader reader = new SuiteReader(SUITE_PATH);
        return reader.stream().onClose(() -> close(reader));
    }


    /**
     * Close an open autoclosable instance.
     */
    private static void close(AutoCloseable closeable) {
        try { closeable.close(); } catch (Exception e) {}
    }
}
