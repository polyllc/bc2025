package poly;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

public abstract class Tower extends Unit {

  protected int spawnTurn = 0;

  protected Tower(RobotController rc) {
    super(rc);
    spawnTurn = rc.getRoundNum();
  }

  public void takeTurn() throws GameActionException {

    attack();

    if (rc.getRoundNum() < 4) {

      for (Direction dir : lib.directionsToMiddle(rc.getLocation())) {
        if (rc.canBuildRobot(UnitType.SOLDIER, rc.getLocation().add(dir))) {
          rc.buildRobot(UnitType.SOLDIER, rc.getLocation().add(dir));
        }
      }
    }

    if (rc.getRoundNum() > 4 && rc.getRoundNum() < 8) {

    }

  }

  private void attack() {

  }
}
