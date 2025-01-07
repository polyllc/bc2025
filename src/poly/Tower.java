package poly;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class Tower extends Unit {

  protected Tower(RobotController rc) {
    super(rc);
  }

  public abstract void takeTurn() throws GameActionException;
}
