package poly;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public abstract class Tower extends Unit {

  protected int spawnTurn = 0;

  protected Tower(RobotController rc) {
    super(rc);
    spawnTurn = rc.getRoundNum();
  }

  public void takeTurn() throws GameActionException {

    attack();

    if (rc.getRoundNum() < 2) {

      for (Direction dir : lib.directionsToMiddle(rc.getLocation())) {
        if (rc.canBuildRobot(UnitType.SOLDIER, rc.getLocation().add(dir))) {
          rc.buildRobot(UnitType.SOLDIER, rc.getLocation().add(dir));
        }
      }
    }
    else {
      build();
    }

  }

  private void attack() throws GameActionException {
    // todo, of course make this much more complicated, quinny boy
    for (RobotInfo robot : lib.sort(lib.getRobots())) {
      if (robot.getTeam() != rc.getTeam()) {
        if (rc.canAttack(robot.getLocation())) {
          rc.attack(robot.getLocation());
        }
      }
    }
  }

  private void build() throws GameActionException {
    for (Direction dir : lib.directionsToMiddle(rc.getLocation())) {
      if (rc.canBuildRobot(UnitType.SOLDIER, rc.getLocation().add(dir))) {
        rc.buildRobot(UnitType.SOLDIER, rc.getLocation().add(dir));
      }
    }
  }
}
