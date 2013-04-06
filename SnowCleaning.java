import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;

class Fixed {
  public static final String DIR_STR = "DLUR";
  public static final int[] DR = new int[]{1, 0, -1, 0};
  public static final int[] DC = new int[]{0, -1, 0, 1};
  public static final int BOUNDS = 4;
  public static final int DAYS_SLICE = 20;
  public static final int WORKER_SLICE = 1;
}

class SnowCleaning {

  private int boardSize_;
  private int salary_;
  private int snowFine_;

  private int numWorkers_;
  private int[][] snowState_;
  private int[][] workerState_;

  private int day_;
  private int slice;

  private ArrayList<String> commands;
  private int[] moved_workers;
  private ArrayList<MoveData> moving_commands;
  private ArrayList<HireData> hiring_commands;

  private LinkedList<Point> snowCells_;

  class HireData {
    public int row;
    public int col;
    public int id;
    HireData(int _row, int _col, int _id) {
      row = _row;
      col = _col;
      id = _id;
    }
  }

  class MoveData {
    public int id;
    public int start_row;
    public int start_col;
    public int end_row;
    public int end_col;
    public String dir;
    MoveData(int _id, int _start_row, int _start_col,
        int _end_row, int _end_col, String _dir) {
      id = _id;
      start_row = _start_row;
      start_col = _start_col;
      end_row = _end_row;
      end_col = _end_col;
      dir = _dir;
    }
  }

  class Point {
    public int row;
    public int col;
    Point(int _row, int _col) {
      row = _row;
      col = _col;
    }
  }

  SnowCleaning() {
  }

  private boolean isLocationSnowed(int row, int col) {
    return (snowState_[row][col] == 1);
  }

  private boolean isIdleWorker(int row, int col) {
    return ((workerState_[row][col] != -1) &&
        !isLocationSnowed(row, col));
  }

  private String getDirection(int dx, int dy) {
    if (dx > 0) {
      return "D";
    }
    if (dx < 0) {
      return "U";
    }
    if (dy > 0) {
      return "R";
    }
    if (dy < 0) {
      return "L";
    }
    return "";
  }

  private boolean isValidCell(int row, int col) {
    return (row >= 0 && row < boardSize_ && col >=0 && col < boardSize_);
  }

  private MoveData nearestIdleWorker(int row, int col) {
    int min_distance = 1000;
    int start_row = row;
    int start_col = col;
    for (int i = -Fixed.BOUNDS; i <= Fixed.BOUNDS; ++i) {
      for (int j = -Fixed.BOUNDS; j <= Fixed.BOUNDS; ++j) {
        if (!isValidCell(row + i, col + j)) continue;
        int worker = workerState_[row + i][col + j];
        if (isIdleWorker(row + i, col + j)) {
          if (moved_workers[worker] == 1) continue;
          int distance = Math.abs(i) + Math.abs(j);
          if (distance < min_distance) {
            min_distance = distance;
            start_row = row + i;
            start_col = col + j;
          }
        }
      }
    }
    if (min_distance < 1000) {
      int dx = row - start_row;
      int dy = col - start_col;
      String direction = getDirection(dx, dy);
      int dir = Fixed.DIR_STR.indexOf(direction);
      int end_row = start_row + Fixed.DR[dir];
      int end_col = start_col + Fixed.DC[dir];
      return new MoveData(workerState_[start_row][start_col], start_row,
          start_col, start_row + Fixed.DR[dir], start_col + Fixed.DC[dir],
          direction);
    }
    return null;
  }

  public int init(int boardSize, int salary, int snowFine) {
    boardSize_ = boardSize;
    salary_ = salary;
    snowFine_ = snowFine;
    numWorkers_ = 0;
    day_ = 0;
    snowCells_ = new LinkedList<Point>();
    snowState_ = new int[boardSize_][boardSize_];
    workerState_ = new int[boardSize_][boardSize_];
    for (int i = 0; i < boardSize_; ++i) {
      for (int j = 0; j < boardSize_; ++j) {
        workerState_[i][j] = -1;
        snowState_[i][j] = 0;
      }
    }
    return 0;
  }

  public void processSnowCell(int row, int col) {
    if (snowState_[row][col] == 0) return;
    boolean hasWorker = (workerState_[row][col] != -1);
    if (!hasWorker) {
      MoveData move_data = nearestIdleWorker(row, col);
      if (move_data != null) {
        commands.add("M " + move_data.id + " " + move_data.dir);
        moved_workers[move_data.id] = 1;
        moving_commands.add(move_data);
      } else {
        if (numWorkers_ < 100 && (numWorkers_ < Fixed.WORKER_SLICE * (1 + slice))) {
          commands.add("H " + row + " " + col);
          hiring_commands.add(new HireData(row, col, numWorkers_));
          numWorkers_++;
        }
      }
    }
  }

  public String[] nextDay(int[] snowFalls) {
    day_++;
    slice = day_ / Fixed.DAYS_SLICE;
    commands = new ArrayList<String>();
    if (snowFine_ < salary_) {
      return commands.toArray(new String[commands.size()]);
    }
    hiring_commands = new ArrayList<HireData>();
    moving_commands = new ArrayList<MoveData>();
    moved_workers = new int[100];
    int K = snowFalls.length / 2;
    for (int i = 0; i < K; ++i) {
      int row = snowFalls[2 * i];
      int col = snowFalls[2 * i + 1];
      snowState_[row][col] = 1;
      snowCells_.addLast(new Point(row, col));
    }
    for (Point p : snowCells_) {
      processSnowCell(p.row, p.col);
    }
    for (HireData hire_data : hiring_commands) {
      workerState_[hire_data.row][hire_data.col] = hire_data.id;
      snowState_[hire_data.row][hire_data.col] = 0;
      snowCells_.remove(new Point(hire_data.row, hire_data.col));
    }
    for (MoveData move_data : moving_commands) {
      workerState_[move_data.start_row][move_data.start_col] = -1;
      workerState_[move_data.end_row][move_data.end_col] = move_data.id;
      snowState_[move_data.end_row][move_data.end_col] = 0;
      snowCells_.remove(new Point(move_data.end_row, move_data.end_col));
    }
    return commands.toArray(new String[commands.size()]);
  }

  public static void main(String[] args) {
    try {
      BufferedReader in = new BufferedReader(
          new InputStreamReader(System.in));
      int boardSize = Integer.parseInt(in.readLine());
      int salary = Integer.parseInt(in.readLine());
      int snowFine = Integer.parseInt(in.readLine());

      SnowCleaning cleaner = new SnowCleaning();

      long duration = 0;
      long startTime = System.nanoTime();
      cleaner.init(boardSize, salary, snowFine);
      long endTime = System.nanoTime();
      duration += (endTime - startTime);

      for (int t = 0; t < 2000; ++t) {
        int snowCnt = Integer.parseInt(in.readLine());
        int[] snowFalls = new int[2 * snowCnt];
        for (int i = 0; i < 2 * snowCnt; ++i) {
          snowFalls[i] = Integer.parseInt(in.readLine());
        }

        startTime = System.nanoTime();
        String[] ret = cleaner.nextDay(snowFalls);
        endTime = System.nanoTime();
        duration += (endTime - startTime);

        System.out.println(ret.length);
        for (int i = 0; i < ret.length; ++i) {
          System.out.println(ret[i]);
        }
      }
      System.out.flush();
      System.err.println("Duration: " + duration / 1e9);
    } catch (Exception e) {}
  }
}
