import java.io.*;
import java.util.ArrayList;

class SnowCleaning {

  private int boardSize_;
  private int salary_;
  private int snowFine_;

  private int numWorkers_;
  private int[][] snowState_;
  private int[][] workerState_;

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

  SnowCleaning() {
  }

  private boolean isLocationSnowed(int row, int col) {
    return (snowState_[row][col] == 1);
  }

  private MoveData nearestNotRequiredHelper(int start_row, int start_col,
      int end_row, int end_col, String dir, ArrayList<Integer> moved_workers) {
    int worker = workerState_[start_row][start_col];
    if (moved_workers.contains(worker)) {
      return null;
    }
    if (worker != -1) {
      if (!isLocationSnowed(start_row, start_col)) {
        return new MoveData(worker, start_row, start_col, end_row, end_col, dir);
      }
    }
    return null;
  }

  private MoveData nearestNotRequiredWorker(int row, int col,
      ArrayList<Integer> moved_workers) {
    if (col != 0) {
      MoveData move_data = nearestNotRequiredHelper(row, col - 1, row, col,
          "R", moved_workers);
      if (move_data != null) {
        return move_data;
      }
    }
    if (col != (boardSize_ - 1)) {
      MoveData move_data = nearestNotRequiredHelper(row, col + 1, row, col,
          "L", moved_workers);
      if (move_data != null) {
        return move_data;
      }
    }
    if (row != 0) {
      MoveData move_data = nearestNotRequiredHelper(row - 1, col, row, col,
          "D", moved_workers);
      if (move_data != null) {
        return move_data;
      }
    }
    if (row != (boardSize_ - 1)) {
      MoveData move_data = nearestNotRequiredHelper(row + 1, col, row, col,
          "U", moved_workers);
      if (move_data != null) {
        return move_data;
      }
    }
    return null;
  }

  public int init(int boardSize, int salary, int snowFine) {
    boardSize_ = boardSize;
    salary_ = salary;
    snowFine_ = snowFine;
    numWorkers_ = 0;
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

  public String[] nextDay(int[] snowFalls) {
    ArrayList<String> commands = new ArrayList<String>();
    if (snowFine_ < salary_) {
      return commands.toArray(new String[commands.size()]);
    }
    ArrayList<HireData> hiring_commands = new ArrayList<HireData>();
    ArrayList<MoveData> moving_commands = new ArrayList<MoveData>();
    ArrayList<Integer> moved_workers = new ArrayList<Integer>();
    int K = snowFalls.length / 2;
    for (int i = 0; i < K; ++i) {
      int row = snowFalls[2 * i];
      int col = snowFalls[2 * i + 1];
      snowState_[row][col] = 1;
    }
    for (int row = 0; row < boardSize_; ++row) {
      for (int col = 0; col < boardSize_; ++col) {
        if (snowState_[row][col] == 0) continue;
        boolean hasWorker = (workerState_[row][col] != -1);
        if (!hasWorker) {
          MoveData move_data = nearestNotRequiredWorker(row, col,
              moved_workers);
          if (move_data != null) {
            commands.add("M " + move_data.id + " " + move_data.dir);
            moved_workers.add(move_data.id);
            moving_commands.add(move_data);
          } else {
            if (numWorkers_ < 100) {
              commands.add("H " + row + " " + col);
              hiring_commands.add(new HireData(row, col, numWorkers_));
              numWorkers_++;
            }
          }
        }
      }
    }
    for (HireData hire_data : hiring_commands) {
      workerState_[hire_data.row][hire_data.col] = hire_data.id;
      if (snowState_[hire_data.row][hire_data.col] == 1) {
        snowState_[hire_data.row][hire_data.col] = 0;
      }
      System.err.println("Added worker " + hire_data.id +
          " to " + hire_data.row + " " + hire_data.col);
    }
    for (MoveData move_data : moving_commands) {
      workerState_[move_data.start_row][move_data.start_col] = -1;
      workerState_[move_data.end_row][move_data.end_col] = move_data.id;
      if (snowState_[move_data.end_row][move_data.end_col] == 1) {
        snowState_[move_data.end_row][move_data.end_col] = 0;
      }
      System.err.println("Moved worker " + move_data.id + " from " +
          move_data.start_row + " " + move_data.start_col + " to " +
          move_data.end_row + " " + move_data.end_col);
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

      cleaner.init(boardSize, salary, snowFine);

      for (int t = 0; t < 2000; ++t) {
        int snowCnt = Integer.parseInt(in.readLine());
        int[] snowFalls = new int[2 * snowCnt];
        for (int i = 0; i < 2 * snowCnt; ++i) {
          snowFalls[i] = Integer.parseInt(in.readLine());
        }

        String[] ret = cleaner.nextDay(snowFalls);

        System.out.println(ret.length);
        for (int i = 0; i < ret.length; ++i) {
          System.out.println(ret[i]);
        }
      }
      System.out.flush();
    } catch (Exception e) {}
  }
}
