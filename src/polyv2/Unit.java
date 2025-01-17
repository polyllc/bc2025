package polyv2;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class Unit {

  protected RobotController rc;
  protected Lib lib;

  public Unit(RobotController rc) throws GameActionException {
    this.rc = rc;
    lib = new Lib(rc);
  }

  abstract public void takeTurn() throws GameActionException;
}
