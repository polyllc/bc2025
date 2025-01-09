package poly;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Mopper extends MovableUnit {

  public Mopper(RobotController rc) {
    super(rc);
  }

  public enum MopperTask {
    EXPLORING,
    MOPPING_ENEMY_TILES,
    MOPPING_ENEMIES,
    MOPPING_RUIN
  }

  MopperTask currentTask = MopperTask.EXPLORING;

  @Override
  public void takeTurn() throws GameActionException {


    paint();
    move();


  }

  private void paint() {

  }

  private void mopEnemies() {

  }

  private void mopEnemyTiles() {

  }

  private void cleanUpRuin() {

  }
}
