package poly;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class Tower extends Unit {

  protected int spawnTurn = 0;

  protected Tower(RobotController rc) {
    super(rc);
    spawnTurn = rc.getRoundNum();
  }

  public void takeTurn() throws GameActionException {

  };
}
