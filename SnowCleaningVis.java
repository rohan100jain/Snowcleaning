import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;

class Constants {
    public static final int SIMULATION_TIME = 2000;
    public static final int MAX_WORKERS = 100;
    public static final String DIR_STR = "DLUR";
    public static final int[] DR = new int[]{1, 0, -1, 0};
    public static final int[] DC = new int[]{0, -1, 0, 1};
}

class CloudType {
    public static final int MIN_CLOUD_SIZE = 1;
    public static final int MAX_CLOUD_SIZE = 3;

    public static final int MIN_CLOUD_TIME = 10;
    public static final int MAX_CLOUD_TIME = 25;

    int size;
    int liveTime;
    double snowProbGlobal;
    double[][] snowProbLocal;

    int[] moveProb;
    int sumMoveProb;

    public int getDirection(Random rnd) {
        int val = rnd.nextInt(sumMoveProb);
        int res = 0;
        while (val >= moveProb[res]) {
            val -= moveProb[res];
            res++;
        }
        return res;
    }

    public CloudType(Random rnd) {
        size = rnd.nextInt(MAX_CLOUD_SIZE - MIN_CLOUD_SIZE + 1) + MIN_CLOUD_SIZE;
        liveTime = rnd.nextInt(MAX_CLOUD_TIME - MIN_CLOUD_TIME + 1) + MIN_CLOUD_TIME;
        snowProbGlobal = rnd.nextDouble();
        snowProbLocal = new double[2 * size + 1][2 * size + 1];
        for (int i = 0; i <= 2 * size; i++) {
            for (int j = 0; j <= 2 * size; j++) {
                snowProbLocal[i][j] = rnd.nextDouble();
            }
        }

        moveProb = new int[Constants.DR.length];
        for (int i = 0; i < moveProb.length; i++) {
            double x = rnd.nextDouble();
            moveProb[i] = (int) Math.ceil(100 * x * x);
            sumMoveProb += moveProb[i];
        }
    }
}

class Cell implements Comparable<Cell> {
    public int r, c;

    public Cell(int r, int c) {
        this.r = r;
        this.c = c;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Cell)) {
            return false;
        }

        Cell otherCell = (Cell) other;
        return this.r == otherCell.r && this.c == otherCell.c;
    }

    public int hashCode() {
        return 100 * r + c;
    }

    public int compareTo(Cell other) {
        return (r == other.r ? c - other.c : r - other.r);
    }
}

class TestCase {
    public static final int MIN_CLOUD_TYPES = 1;
    public static final int MAX_CLOUD_TYPES = 10;

    public static final int MIN_CLOUD_COUNT = 50;
    public static final int MAX_CLOUD_COUNT = 200;

    public static final int MIN_BOARD_SIZE = 20;
    public static final int MAX_BOARD_SIZE = 50;

    public static final int MIN_SALARY = 10;
    public static final int MAX_SALARY = 100;

    public static final int MIN_SNOW_FINE = 10;
    public static final int MAX_SNOW_FINE = 100;

    public int boardSize;
    public int salary;
    public int snowFine;

    public int cloudTypeCnt;
    public CloudType[] cloudTypes;

    public int cloudCnt;

    Set<Cell>[] snowFalls = new Set[Constants.SIMULATION_TIME];

    public TestCase(long seed) {
        SecureRandom rnd = null;

        try {
            rnd = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
            System.err.println("ERROR: unable to generate test case.");
            System.exit(1);
        }

        rnd.setSeed(seed);

        boardSize = rnd.nextInt(MAX_BOARD_SIZE - MIN_BOARD_SIZE + 1) + MIN_BOARD_SIZE;
        salary = rnd.nextInt(MAX_SALARY - MIN_SALARY + 1) + MIN_SALARY;
        snowFine = rnd.nextInt(MAX_SNOW_FINE - MIN_SNOW_FINE + 1) + MIN_SNOW_FINE;

        cloudTypeCnt = rnd.nextInt(MAX_CLOUD_TYPES - MIN_CLOUD_TYPES + 1) + MIN_CLOUD_TYPES;
        cloudTypes = new CloudType[cloudTypeCnt];
        for (int i = 0; i < cloudTypeCnt; i++) {
            cloudTypes[i] = new CloudType(rnd);
        }

        cloudCnt = rnd.nextInt(MAX_CLOUD_COUNT - MIN_CLOUD_COUNT + 1) + MIN_CLOUD_COUNT;

        for (int t = 0; t < Constants.SIMULATION_TIME; t++) {
            snowFalls[t] = new HashSet<Cell>();
        }

        for (int i = 0; i < cloudCnt; i++) {
            int type = rnd.nextInt(cloudTypeCnt);

            int curRow = rnd.nextInt(boardSize);
            int curCol = rnd.nextInt(boardSize);
            int startTime = rnd.nextInt(Constants.SIMULATION_TIME);

            for (int t = startTime; t < startTime + cloudTypes[type].liveTime && t < Constants.SIMULATION_TIME; t++) {
                if (rnd.nextDouble() < cloudTypes[type].snowProbGlobal) {
                    for (int r = 0; r <= 2 * cloudTypes[type].size; r++) {
                        for (int c = 0; c <= 2 * cloudTypes[type].size; c++) {
                            if (rnd.nextDouble() < cloudTypes[type].snowProbLocal[r][c]) {
                                int snowR = curRow + r - cloudTypes[type].size;
                                int snowC = curCol + c - cloudTypes[type].size;
                                if (snowR >= 0 && snowR < boardSize && snowC >= 0 && snowC < boardSize) {
                                    snowFalls[t].add(new Cell(snowR, snowC));
                                }
                            }
                        }
                    }
                }
                int dir = cloudTypes[type].getDirection(rnd);
                curRow += Constants.DR[dir];
                curCol += Constants.DC[dir];
            }
        }
    }
}

class Drawer extends JFrame {
    public static final int EXTRA_WIDTH = 300;
    public static final int EXTRA_HEIGHT = 100;

    public World world;
    public DrawerPanel panel;

    public int cellSize, boardSize;
    public int width, height;

    public boolean pauseMode = false;

    class DrawerKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            synchronized (keyMutex) {
                if (e.getKeyChar() == ' ') {
                    pauseMode = !pauseMode;
                }
                keyPressed = true;
                keyMutex.notifyAll();
            }
        }
    }

    class DrawerPanel extends JPanel {
        public void paint(Graphics g) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(15, 15, cellSize * boardSize + 1, cellSize * boardSize + 1);
            g.setColor(Color.RED);
            for (int i = 0; i <= boardSize; i++) {
                g.drawLine(15 + i * cellSize, 15, 15 + i * cellSize, 15 + cellSize * boardSize);
                g.drawLine(15, 15 + i * cellSize, 15 + cellSize * boardSize, 15 + i * cellSize);
            }

            g.setColor(Color.WHITE);
            for (int i=0; i < boardSize; i++) {
                for (int j=0; j < boardSize; j++) {
                    if (world.haveSnow[i][j]) {
                        g.fillRect(15 + j * cellSize + 1, 15 + i * cellSize + 1, cellSize - 2, cellSize - 2);
                    }
                }
            }

            g.setColor(Color.BLUE);
            synchronized (world.workersLock) {
                for (Cell worker : world.workers) {
                    g.fillRect(15 + worker.c * cellSize + 1, 15 + worker.r * cellSize + 1, cellSize - 2, cellSize - 2);
                }
            }

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            Graphics2D g2 = (Graphics2D)g;

            int horPos = 40 + boardSize * cellSize;

            g2.drawString("Board size = " + world.haveSnow.length, horPos, 30);
            g2.drawString("Snow fine = " + world.fine, horPos, 50);
            g2.drawString("Salary = " + world.salary, horPos, 70);

            g2.drawString("Day = " + world.curDay, horPos, 105);
            g2.drawString("Uncleared snow cells = " + world.snowCnt, horPos, 125);
            synchronized (world.workersLock) {
                g2.drawString("Workers = " + world.workers.size(), horPos, 145);
            }
            g2.drawString("Total snow fine = ", horPos, 180);
            g2.drawString("" + world.totFine, horPos + 100, 180);
            g2.drawString("Total salary = ", horPos, 200);
            g2.drawString("" + world.totSalary, horPos + 100, 200);
            g2.drawString("Current score = ", horPos, 220);
            g2.drawString("" + (world.totFine + world.totSalary), horPos + 100, 220);
        }
    }

    class DrawerWindowListener extends WindowAdapter {
        public void windowClosing(WindowEvent event) {
            SnowCleaningVis.stopSolution();
            System.exit(0);
        }
    }

    final Object keyMutex = new Object();
    boolean keyPressed;

    public void processPause() {
        synchronized (keyMutex) {
            if (!pauseMode) {
                return;
            }
            keyPressed = false;
            while (!keyPressed) {
                try {
                    keyMutex.wait();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }

    public Drawer(World world, int cellSize) {
        super();

        panel = new DrawerPanel();
        getContentPane().add(panel);

        addWindowListener(new DrawerWindowListener());

        this.world = world;

        boardSize = world.haveSnow.length;
        this.cellSize = cellSize;
        width = cellSize * boardSize + EXTRA_WIDTH;
        height = cellSize * boardSize + EXTRA_HEIGHT;

        addKeyListener(new DrawerKeyListener());

        setSize(width, height);
        setTitle("Visualizer tool for problem SnowCleaning");

        setResizable(false);
        setVisible(true);
    }
}

class World {
    final Object workersLock = new Object();

    int snowCnt;
    boolean[][] haveSnow;

    List<Cell> workers = new ArrayList<Cell>();
    Set<Integer> usedWorkers = new HashSet<Integer>();

    int salary, fine;
    int totSalary, totFine;
    int curDay = -1;

    public World(int boardSize, int salary, int fine) {
        this.salary = salary;
        this.fine = fine;
        haveSnow = new boolean[boardSize][boardSize];
    }

    public void updateTotalSalary() {
        synchronized (workersLock) {
            totSalary += salary * workers.size();
        }
    }

    public void updateTotalFine() {
        totFine += snowCnt * fine;
    }

    public void addSnow(int r, int c) {
        if (!haveSnow[r][c]) {
            snowCnt++;
            haveSnow[r][c] = true;
        }
    }

    public void removeSnow(int r, int c) {
        if (haveSnow[r][c]) {
            snowCnt--;
            haveSnow[r][c] = false;
        }
    }

    public void startNewDay() {
        curDay++;
        usedWorkers.clear();
    }

    public String addWorker(int r, int c) {
        synchronized (workersLock) {
            if (workers.size() == Constants.MAX_WORKERS) {
                return "You are allowed to have at most " + Constants.MAX_WORKERS + " workers.";
            } else if (r < 0 || r >= haveSnow.length || c < 0 || c >= haveSnow.length) {
                return "You are trying to hire a worker at a cell outside the board.";
            } else {
                workers.add(new Cell(r, c));
                usedWorkers.add(workers.size() - 1);
                removeSnow(r, c);
                return "";
            }
        }
    }

    public String moveWorker(int id, int dir) {
        synchronized (workersLock) {
            if (id < 0 || id >= workers.size()) {
                return "You are trying to move worker which does not exist.";
            } else if (usedWorkers.contains(id)) {
                return "You are trying to execute a command for some worker more than once during the same turn.";
            } else {
                Cell worker = workers.get(id);
                worker.r += Constants.DR[dir];
                worker.c += Constants.DC[dir];
                if (worker.r < 0 || worker.c < 0 || worker.r >= haveSnow.length || worker.c >= haveSnow.length) {
                    return "You are trying to move a worker outside the board.";
                }
                removeSnow(worker.r, worker.c);
                usedWorkers.add(id);
                return "";
            }
        }
    }

    public void cleanAllSnow() {
        synchronized (workersLock) {
            for (Cell worker : workers) {
                removeSnow(worker.r, worker.c);
            }
        }
    }
}

public class SnowCleaningVis {
    public static String MOVE_REG_EXP = "M [1-9]?[0-9] [ULDR]";
    public static String HIRE_REG_EXP = "H [1-9]?[0-9] [1-9]?[0-9]";

    public static String WRONG_COMMAND_ERROR = "ERROR: Each worker command must be formatted either \"M <ID> <DIR>\"" +
            " or \"H <ROW> <COL>\". Here <ID>, <ROW> and <COL> are integers from 0 to 99 without leading zeros" +
            " and <DIR> is one of 'U', 'L', 'D', 'R'.";

    public static String execCommand = null;
    public static long seed = 1;
    public static boolean vis = true;
    public static int cellSize = 12;
    public static int delay = 100;
    public static boolean startPaused = false;

    public static Process solution;

    public int runTest() {
        solution = null;

        try {
            solution = Runtime.getRuntime().exec(execCommand);
        } catch (Exception e) {
            System.err.println("ERROR: Unable to execute your solution using the provided command: "
                    + execCommand + ".");
            return -1;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(solution.getInputStream()));
        PrintWriter writer = new PrintWriter(solution.getOutputStream());
        new ErrorStreamRedirector(solution.getErrorStream()).start();

        TestCase tc = new TestCase(seed);

        writer.println(tc.boardSize);
        writer.println(tc.salary);
        writer.println(tc.snowFine);
        writer.flush();

        World world = new World(tc.boardSize, tc.salary, tc.snowFine);
        Drawer drawer = null;
        if (vis) {
            drawer = new Drawer(world, cellSize);
            if (startPaused) {
                drawer.pauseMode = true;
            }
        }

        for (int t = 0; t < Constants.SIMULATION_TIME; t++) {
            world.startNewDay();

            int snowFallCnt = tc.snowFalls[t].size();
            Cell[] snowFalls = new Cell[snowFallCnt];
            int pos = 0;
            for (Cell cell : tc.snowFalls[t]) {
                snowFalls[pos++] = cell;
            }
            Arrays.sort(snowFalls);

            StringBuilder sb = new StringBuilder();
            sb.append(snowFallCnt).append("\n");
            for (int i=0; i < snowFallCnt; i++) {
                sb.append(snowFalls[i].r).append("\n").append(snowFalls[i].c).append("\n");
                world.addSnow(snowFalls[i].r, snowFalls[i].c);
            }

            writer.print(sb.toString());
            writer.flush();

            int commandCnt;
            try {
                commandCnt = Integer.parseInt(reader.readLine());
            } catch (Exception e) {
                System.err.println("ERROR: time step = " + t + " (0-based). Unable to get the number of worker commands" +
                        " from your solution.");
                return -1;
            }

            for (int i = 0; i < commandCnt; i++) {
                String command;
                try {
                    command = reader.readLine();
                } catch (Exception e) {
                    System.err.println("ERROR: time step = " + t + " + (0-based). Unable to read " + i + "-th (0-based)" +
                            " worker command from your solution.");
                    return -1;
                }
                if (command.length() > 10) {
                    System.err.println("ERROR: time step = " + t + ", worker command = " + i + " (0-based indices). " + WRONG_COMMAND_ERROR);
                    return -1;
                }
                if (command.matches(HIRE_REG_EXP)) {
                    String[] items = command.split(" ");
                    int row = Integer.parseInt(items[1]);
                    int col = Integer.parseInt(items[2]);
                    String msg = world.addWorker(row, col);
                    if (msg.length() > 0) {
                        System.err.println("ERROR: time step = " + t + ", worker command = " + i + " (0-based indices). " + msg);
                        return -1;
                    }
                } else if (command.matches(MOVE_REG_EXP)) {
                    String[] items = command.split(" ");
                    int id = Integer.parseInt(items[1]);
                    int dir = Constants.DIR_STR.indexOf(items[2]);
                    String msg = world.moveWorker(id, dir);
                    if (msg.length() > 0 ){
                        System.err.println("ERROR: time step = " + t + ", worker command = " + i + " (0-based indices). " + msg);
                        return -1;
                    }
                } else {
                    System.err.println("ERROR: time step = " + t + ", worker command = " + i + " (0-based indices). " + WRONG_COMMAND_ERROR);
                    return -1;
                }
            }

            world.cleanAllSnow();

            world.updateTotalFine();
            world.updateTotalSalary();

            if (vis) {
                drawer.processPause();
                drawer.repaint();
                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        stopSolution();

        System.out.println("Fine   = " + world.totFine);
        System.out.println("Salary = " + world.totSalary);

        return world.totFine + world.totSalary;
    }

    public static void stopSolution() {
        if (solution != null) {
            try {
                solution.destroy();
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++)
            if (args[i].equals("-exec")) {
                execCommand = args[++i];
            } else if (args[i].equals("-seed")) {
                seed = Long.parseLong(args[++i]);
            } else if (args[i].equals("-novis")) {
                vis = false;
            } else if (args[i].equals("-sz")) {
                cellSize = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-delay")) {
                delay = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-pause")) {
                startPaused = true;
            } else {
                System.out.println("WARNING: unknown argument " + args[i] + ".");
            }

        if (execCommand == null) {
            System.err.println("ERROR: You did not provide the command to execute your solution." +
                    " Please use -exec <command> for this.");
            System.exit(1);
        }

        SnowCleaningVis vis = new SnowCleaningVis();
        try {
            int score = vis.runTest();
            System.out.println("Score  = " + score);
        } catch (RuntimeException e) {
            System.err.println("ERROR: Unexpected error while running your test case.");
            e.printStackTrace();
            SnowCleaningVis.stopSolution();
        }
    }
}

class ErrorStreamRedirector extends Thread {
    public BufferedReader reader;

    public ErrorStreamRedirector(InputStream is) {
        reader = new BufferedReader(new InputStreamReader(is));
    }

    public void run() {
        while (true) {
            String s;
            try {
                s = reader.readLine();
            } catch (Exception e) {
                // e.printStackTrace();
                return;
            }
            if (s == null) {
                break;
            }
            System.out.println(s);
        }
    }
}
