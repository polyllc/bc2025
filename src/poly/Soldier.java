package poly;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Soldier implements Unit {

  RobotController rc;

  public Soldier(RobotController rc) {
    this.rc = rc;
  }

  @Override
  public void takeTurn() throws GameActionException {

  }
}
