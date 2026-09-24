package com.slotsim.play;

import com.slotsim.engine.ReelWindow;
import com.slotsim.engine.SpinEngine;
import com.slotsim.engine.Win;
import com.slotsim.engine.WinEvaluator;
import com.slotsim.engine.WinRecorder;
import com.slotsim.engine.WinType;
import com.slotsim.model.MathModel;
import com.slotsim.model.Payline;
import com.slotsim.rng.RandomNumberGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Interactive console session on top of the same engine the simulation uses: each Enter plays one
 * round, prints the reel picture with the winning cells marked, the wins and the balance.
 * <p>
 * Meant as a demo that the engine plays real rounds; there is no wallet, only a credit balance.
 */
public final class ConsoleGame {

    private final MathModel model;
    private final SpinEngine engine;
    private final BufferedReader in;
    private final PrintStream out;
    private final WinRecorder recorder = new WinRecorder();
    private final int cellWidth;

    private long balance;
    private long rounds;
    private long totalBet;
    private long totalWin;

    public ConsoleGame(MathModel model, WinEvaluator evaluator, RandomNumberGenerator rng,
                       long startingBalance, BufferedReader in, PrintStream out) {
        this.model = model;
        this.engine = new SpinEngine(model, evaluator, rng);
        this.balance = startingBalance;
        this.in = in;
        this.out = out;
        this.cellWidth = model.symbols().stream().mapToInt(s -> s.code().length()).max().orElse(1) + 2;
    }

    /**
     * Plays rounds until the player quits, input ends or the balance cannot cover the bet.
     */
    public void run() {
        printf("%s: bet %d per round, balance %,d. Enter = spin, q = quit.%n",
                model.name(), model.bet(), balance);
        while (balance >= model.bet() && readCommand()) {
            playRound();
        }
        if (balance < model.bet()) {
            printf("%nBalance %,d is below the bet of %d.%n", balance, model.bet());
        }
        printSummary();
    }

    private boolean readCommand() {
        printf("%n> ");
        try {
            String line = in.readLine();
            return line != null && !line.trim().equalsIgnoreCase("q");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void playRound() {
        recorder.clear();
        balance -= model.bet();
        long win = engine.spin(recorder);
        balance += win;
        rounds++;
        totalBet += model.bet();
        totalWin += win;

        List<Win> wins = recorder.wins();
        printf("Round %d, stops %s%n%n", rounds, Arrays.toString(engine.lastStops()));
        printPicture(engine.window(), winningCells(wins));
        out.println();
        for (Win w : wins) {
            printf("  %s%n", describe(w));
        }
        printf("  Win: %,d   Balance: %,d%n", win, balance);
    }

    private boolean[][] winningCells(List<Win> wins) {
        ReelWindow window = engine.window();
        boolean[][] marked = new boolean[window.reels()][window.rows()];
        for (Win win : wins) {
            if (win.type() == WinType.LINE) {
                Payline line = payline(win.paylineId());
                for (int reel = 0; reel < win.count(); reel++) {
                    marked[reel][line.rowOnReel(reel)] = true;
                }
            } else {
                for (int reel = 0; reel < window.reels(); reel++) {
                    for (int row = 0; row < window.rows(); row++) {
                        marked[reel][row] |= window.symbolAt(reel, row) == win.symbolId();
                    }
                }
            }
        }
        return marked;
    }

    private void printPicture(ReelWindow window, boolean[][] marked) {
        for (int row = 0; row < window.rows(); row++) {
            StringBuilder line = new StringBuilder("  ");
            for (int reel = 0; reel < window.reels(); reel++) {
                String code = model.symbol(window.symbolAt(reel, row)).code();
                String cell = marked[reel][row] ? "[" + code + "]" : " " + code + " ";
                line.append(String.format(Locale.ROOT, "%-" + cellWidth + "s ", cell));
            }
            out.println(line.toString().stripTrailing());
        }
    }

    private String describe(Win win) {
        String symbol = model.symbol(win.symbolId()).code();
        if (win.type() == WinType.LINE) {
            return String.format(Locale.ROOT, "Line %d: %dx %s = %,d", win.paylineId(), win.count(), symbol,
                    win.amount());
        }
        return String.format(Locale.ROOT, "Scatter: %dx %s = %,d", win.count(), symbol, win.amount());
    }

    private Payline payline(int id) {
        return model.paylines().stream()
                .filter(line -> line.id() == id)
                .findFirst()
                .orElseThrow();
    }

    private void printSummary() {
        printf("%nSession: %,d rounds, bet %,d, won %,d", rounds, totalBet, totalWin);
        if (totalBet > 0) {
            printf(", RTP %.2f%%", 100.0 * totalWin / totalBet);
        }
        printf(", balance %,d%n", balance);
    }

    private void printf(String format, Object... args) {
        out.print(String.format(Locale.ROOT, format, args));
        out.flush();
    }
}
