package com.joansala.uci;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Scanner;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import com.google.inject.Inject;

import com.joansala.except.IllegalMoveException;
import com.joansala.engine.*;


/**
 * Implements a communication protocol service for game engines.
 * The protocol implemented resembles that of the Universal Chess
 * Interface protocol with only minimal adaptations. Therefore, this
 * class provides a service that reads UCI commands from the standard
 * input and writes replies to the standard output.</p>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class UCIService {

    /** Thread where the computations are performed */
    private UCIBrain brain;

    /** Performs move computations for a game */
    protected Engine engine = null;

    /** Game where the computations are performed */
    protected Game game = null;

    /** Contains the current start position and turn */
    private Board board = null;

    /** Contains the initial board of the game */
    private Board rootBoard = null;

    /** Opening book */
    protected Roots<Game> roots = null;

    /** Engine's endgames database */
    protected Leaves<Game> leaves = null;

    /** Engine's transposition table */
    private Cache<Game> cache = null;

    /** Last info shown for the current computation */
    protected String lastInfo = null;

    /** Contains the performed moves for the next computation */
    private int[] moves = null;

    /** Minimum capacity in MB for the hash table */
    private int minHashSize = 1;

    /** Maximum capacity in MB for the hash table */
    private int maxHashSize = 1;

    /** Current capacity in MB for the hash table */
    private int currentHashSize = minHashSize;

    /** Requested hash table capacity for subsequent computations */
    private int requestedHashSize = minHashSize;

    /** Contempt factor for the engine */
    private int contempt = Game.DRAW_SCORE;

    /** Infinity score for the engine */
    private int infinity = Integer.MAX_VALUE;

    /** If debug mode is enabled the engine sends additional infos */
    private boolean debug = false;

    /** If set to true the engine will have a high preference for draws */
    private volatile boolean drawSearch = false;

    /** If set to true the engine will use its own book */
    protected volatile boolean ownBook = true;

    /** If set the engine will use its endgames database */
    protected volatile boolean useLeaves = true;

    /** If set to true the time for next computation will be infinite */
    protected volatile boolean infinite = false;


    /**
     * Instantiates a new UCI service for the given game. The provided
     * engine will be used to perform move computations. The provided
     * board is the initial position for the game.
     *
     * @param game      A game object
     * @param engine    An engine object
     */
    @Inject public UCIService(Game game, Engine engine) {
        this.game = game;
        this.engine = engine;
        this.rootBoard = game.getBoard();
        this.board = rootBoard;
        this.moves = new int[0];

        if (engine instanceof HasCache) {
            cache = ((HasCache) engine).getCache();
        }

        if (engine instanceof HasLeaves) {
            leaves = ((HasLeaves) engine).getLeaves();
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            System.exit(1);
        });
    }


    /**
     * Sets the contempt factor of the engine.
     *
     * @param score     Score for drawn positions
     */
    public synchronized void setContempt(int score) {
        this.contempt = score;
    }


    /**
     * Sets the infinity score of the engine.
     *
     * @param score     Maximum possible score
     */
    public synchronized void setInfinity(int score) {
        this.infinity = score;
    }


    /**
     * Sets the openings book database to use.
     *
     * @param roots     A roots object or {@code null} to disable
     *                  the use of an opening book
     */
    @Inject(optional=true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized void setRoots(Roots roots) {
        this.roots = roots;
    }


    /**
     * Asks this service to start receiving commands. Commands are parsed
     * from the standard input until a quit command is received. Replies
     * are written to the standard output.
     */
    public synchronized void start() {
        Scanner scanner = new Scanner(System.in);

        // Brain initialization

        this.brain = new UCIBrain(this);
        this.brain.start();

        // Remember current hash size and available memory

        if (cache != null) {
            long freeMemory = Runtime.getRuntime().freeMemory();
            long currentMemory = cache.size();

            this.maxHashSize = (int) ((currentMemory + freeMemory) >> 20);
            this.currentHashSize = (int) (currentMemory >> 20);
            this.requestedHashSize = currentHashSize;
        }

        // Start parsing received commands

        while (scanner.hasNext()) {
            String command = scanner.next();
            String params = scanner.nextLine();

            if ("debug".equals(command)) {
                switchDebugMode(params);
            } else if ("moves".equals(command)) {
                printMoves();
            } else if ("go".equals(command)) {
                startThinking(params);
            } else if ("isready".equals(command)) {
                answerPing();
            } else if ("ponderhit".equals(command)) {
                stopPondering();
            } else if ("position".equals(command)) {
                setPosition(params);
            } else if ("quit".equals(command)) {
                quitEngine();
                break;
            } else if ("setoption".equals(command)) {
                setOption(params);
            } else if ("stop".equals(command)) {
                stopThinking();
            } else if ("uci".equals(command)) {
                identifyEngine();
            } else if ("ucinewgame".equals(command)) {
                startNewGame();
            } else {
                showError("Unknown command");
            }
        }

        this.brain.interrupt();
        scanner.close();
    }


    /**
     * Performs a move on the internal board. This method asserts that
     * the move is legal and ensures the game has enough capacity to
     * store it.
     *
     * @param game  Game object where a move must be performed
     * @param move  Move to perform on the game
     * @throws IllegalMoveException  if the move cannot be
     *      performed on the provided game object
     */
    protected void performMove(Game game, int move) {
        if (game.isLegal(move)) {
            game.ensureCapacity(1 + game.length());
            game.makeMove(move);
        } else {
            throw new IllegalMoveException(
                "The provided move is not legal");
        }
    }


    /**
     * Waits for the brain to be ready for at most one second. The
     * brain is ready when it's thinking state is false.
     */
    protected void synchBrain() {
        for (int i = 0; brain.isThinking() && i < 50; i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                break;
            }
        }
    }


    /**
     * Prints the engine name and author on the standard output
     */
    private void identifyEngine() {
        Package pack = game.getClass().getPackage();
        String title = pack.getImplementationTitle();
        String version = pack.getImplementationVersion();
        String author = pack.getImplementationVendor();

        // Show version information and author

        output("id name " + (title == null ? "Unknown Engine" : title) +
            " " + (version == null ? "" : version));
        output("id author " + (author == null ? "Unknown Author" : author));

        // Hash table size is set based on the available memory

        if (cache != null) {
            output("option name Hash type spin default " + currentHashSize +
                " min " + minHashSize + " max " + maxHashSize);
        }

        if (leaves != null) {
            output("option name UseLeaves type check default true");
        }

        // Show own book option only when the book is present

        if (roots != null) {
            output("option name OwnBook type check default true");
        }

        // Ponder mode is enabled by default

        output("option name Ponder type check default true");

        // Sets the engine to prefer draw scores

        output("option name DrawSearch type check default false");

        // The engine is ready

        output("uciok");
    }


    /**
     * Prints 'readyok' on the standard output
     */
    private void answerPing() {
        output("readyok");
    }


    /**
     * Switch the debug mode on and off
     */
    private void switchDebugMode(String params) {
        Scanner scanner = new Scanner(params);

        if (!scanner.hasNext())
            debug = !debug;

        while (scanner.hasNext()) {
            String token = scanner.next();

            if (token.equals("on")) {
                debug = true;
            } else if (token.equals("off")) {
                debug = false;
            }
        }

        scanner.close();
    }


    /**
     * Tells the engine to start a new game
     */
    private void startNewGame() {
        synchBrain();

        if (brain.isThinking()) {
            showError("A calculation is in progress");
            return;
        }

        if (roots != null)
            roots.newMatch();

        engine.newMatch();
    }


    /**
     * Stops the search thread
     */
    private void quitEngine() {
        brain.stopThinking();
        brain.interrupt();
    }


    /**
     * Parses the 'go' command parameters setting the relevant engine
     * propieties and starts a new move computation.
     *
     * @param params  A string containing options for the command
     */
    private void startThinking(String params) {
        synchBrain();

        if (brain.isThinking()) {
            showError("Already calculating a move");
            return;
        }

        // Set default parameters

        int contempt = this.contempt;
        int depth = Integer.MAX_VALUE;
        long movetime = Engine.DEFAULT_MOVETIME;

        infinite = false;

        // Obtain search params

        Scanner scanner = new Scanner(params);

        while (scanner.hasNext()) {
            String token = scanner.next();

            if (token.equals("depth")) {
                if (scanner.hasNextByte())
                    depth = scanner.nextByte();
            } else if (token.equals("infinite")) {
                infinite = true;
            } else if (token.equals("movetime")) {
                if (scanner.hasNextLong())
                    movetime = scanner.nextLong();
            } else if (token.equals("ponder")) {
                infinite = true;
            }
        }

        scanner.close();

        // Ensure parameters for infinite search

        if (infinite) {
            depth = Integer.MAX_VALUE;
            movetime = 60 * 60 * 1000;
        }

        // Set the comptempt factor

        infinity = game.infinity();
        contempt = game.contempt();

        if (drawSearch == true) {
            contempt = infinity;
        }

        // Set engine parameters

        engine.setDepth(depth - 1);
        engine.setMoveTime(movetime);
        engine.setContempt(contempt);
        engine.setInfinity(infinity);

        // Resize the hash table if requested

        if (cache != null) {
            if (currentHashSize != requestedHashSize) {
                cache.resize(requestedHashSize << 20);
                currentHashSize = requestedHashSize;
            }
        }

        // Set the position

        game.setBoard(board);

        for (int move : moves) {
            performMove(game, move);
        }

        // Start calculating a move

        brain.startThinking();
    }


    /**
     * Outputs legals moves for the current game state.
     */
    private void printMoves() {
        StringJoiner notation = new StringJoiner(" ");
        int[] moves = game.legalMoves();

        for (int move : moves) {
            Board board = game.toBoard();
            notation.add(board.toCoordinates(move));
        }

        String message = String.format("Legal moves: %s",
            moves.length > 0 ? notation : "None");

        printString(message);
        answerPing();
    }


    /**
     * Parses the 'stop' command parameters setting the relevant engine
     * propieties.
     */
    private void stopThinking() {
        brain.stopThinking();
    }


    /**
     * Parses the 'ponderhit' command parameters setting the relevant
     * engine propieties.
     */
    private void stopPondering() {}


    /**
     * Parses the 'setoption' command parameters setting the relevant engine
     * propieties.
     *
     * @param params  A string containing options for the command
     */
    private void setOption(String params) {
        String name = null;
        String value = null;

        // Obtain option name and value

        Scanner scanner = new Scanner(params);
        Pattern stop = Pattern.compile("name|value");

        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.equals("name")) {
                name = consumeString(scanner, stop);
            } else if (token.equals("value")) {
                value = consumeString(scanner, stop);
            }
        }

        scanner.close();

        // No option name specified

        if (name == null) {
            showError("No option name specified");
            return;
        }

        // Set use own openings book parameter

        if (name.equals("OwnBook")) {
            if (value.equals("true")) {
                ownBook = true;
            } else if (value.equals("false")) {
                ownBook = false;
            } else {
                showError("Invalid value for option OwnBook");
            }

            return;
        }

        // Use endgame tablebases

        if (name.equals("UseLeaves")) {
            if (value.equals("true")) {
                useLeaves = true;
            } else if (value.equals("false")) {
                useLeaves = false;
            } else {
                showError("Invalid value for option UseLeaves");
            }

            return;
        }

        // Set draw search parameter

        if (name.equals("DrawSearch")) {
            if (value.equals("true")) {
                drawSearch = true;
            } else if (value.equals("false")) {
                drawSearch = false;
            } else {
                showError("Invalid value for option DrawSearch");
            }

            return;
        }

        // Set hash size parameter

        if (name.equals("Hash") && cache != null) {
            try {
                int size = Integer.parseInt(value);

                if (size >= minHashSize && size <= maxHashSize) {
                    requestedHashSize = size;
                } else {
                    showError("Hash size out of range");
                }
            } catch (NumberFormatException e) {
                showError("Hash size is not a number");
            }

            return;
        }

        // No valid option requested

        showError("Cannot set requested option");
    }


    /**
     * Parses the 'position' command parameters setting the relevant engine
     * propieties.
     *
     * @param params  A string containing options for the command
     */
    private void setPosition(String params) {
        String boardNotation = null;
        String movesNotation = null;
        Board board = this.rootBoard;
        int[] moves = new int[0];

        // Obtain the position command parameters

        Scanner scanner = new Scanner(params);
        Pattern stop = Pattern.compile("startpos|fen|moves");

        while (scanner.hasNext()) {
            String token = scanner.next();

            if (token.equals("fen")) {
                if (scanner.hasNext()) {
                    boardNotation = consumeString(scanner, stop);
                }
            } else if (token.equals("moves")) {
                movesNotation = consumeString(scanner, stop);
            }
        }

        scanner.close();

        // Set the new board if we received a notation for it

        if (boardNotation != null) {
            try {
                board = rootBoard.toBoard(boardNotation);
                game.setBoard(board);
            } catch (Exception e) {
                showError(e.getMessage());
                return;
            }
        }

        // Set the performed moves if we received a notation for them

        if (movesNotation != null) {
            try {
                moves = board.toMoves(movesNotation);
            } catch (Exception e) {
                showError(e.getMessage());
                return;
            }
        }


        // Set the board state on the game

        try {
            game.setBoard(board);

            for (int move : moves) {
                performMove(game, move);
            }

            this.board = board;
            this.moves = moves;
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }


    /**
     * Prints an error string if debug mode is enabled.
     *
     * @param s     message
     */
    protected void showError(String s) {
        if (debug == true)
            printString("Error: " + s);
    }


    /**
     * Prints an information string
     *
     * @param s     message
     */
    private void printString(String s) {
        output("info string " + s);
    }


    /**
     * Prints an engine message to the standart output.
     *
     * @param message   engine message
     */
    protected void output(String message) {
        synchronized (System.out) {
            System.out.println(message);
        }
    }


    /**
     * Builds a {@code String} from the {@code Scanner} concatenating
     * each token until it founds a token that matches the stop pattern
     * or the end of input is reached.
     *
     * @param scanner  The scanner from which to consume tokens
     * @param stop     The stop pattern
     */
    private static String consumeString(Scanner scanner, Pattern stop) {
        StringBuilder sb = new StringBuilder();

        while (scanner.hasNext() && !scanner.hasNext(stop)) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(scanner.next());
        }

        return sb.toString();
    }
}
