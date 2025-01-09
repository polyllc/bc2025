package poly;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Splashers extends MovableUnit {

  public Splashers(RobotController rc) {
    super(rc);
  }

  public enum SplasherTask {
    EXPLORE,
    SPLASH_ENEMIES,
    PAINT_WORLD
  }

  SplasherTask currentTask = SplasherTask.EXPLORE;

  @Override
  public void takeTurn() throws GameActionException {
    move();
    paint();
  }

  private void paint() {

  }

  private void splashEnemies() throws GameActionException {

  }

}
