package com.joansala.game.go;

import java.io.FileInputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import com.joansala.engine.Board;
import com.joansala.engine.BoardContract;
import com.joansala.game.go.GoBoard;
import com.joansala.util.suites.Suite;
import com.joansala.util.suites.SuiteReader;


@DisplayName("Go board")
public class GoBoardTest implements BoardContract {

    /** Test suite file path */
    private static String SUITE_PATH = "/go-bench.suite";


    /**
     * {@inheritDoc}
     */
    @Override
    public Board newInstance() {
        return new GoBoard();
    }


    /**
     * Stream of game suites to test.
     */
    public static Stream<Suite> suites() throws Exception {
        String path = getResourcePath(SUITE_PATH);
        FileInputStream input = new FileInputStream(path);
        SuiteReader reader = new SuiteReader(input);

        return reader.stream().onClose(() -> close(reader));
    }


    /**
     * Obtain a path to the given resource file.
     */
    private static String getResourcePath(String path) {
        return BoardContract.class.getResource(path).getFile();
    }


    /**
     * Close an open autoclosable instance.
     */
    private static void close(AutoCloseable closeable) {
        try { closeable.close(); } catch (Exception e) {}
    }
}
