package poly;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

import java.util.ArrayList;

public abstract class Tower extends Unit {

  protected int spawnTurn = 0;

  private int spawnedUnits = 0;

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
    int count = 0;
    ArrayList<RobotInfo> robots = new ArrayList<RobotInfo>();
    for (RobotInfo robot : lib.sort(lib.getRobots(false))) {
      if (robot.getTeam() != rc.getTeam()) {
        if (rc.canAttack(robot.getLocation())) {
          count++;
          robots.add(robot);
          //rc.attack(robot.getLocation());
        }
      }
    }
    if (count > 2) {
      rc.attack(null);
    }
    else if (count == 1) {
      rc.attack(robots.get(0).getLocation());
    }
    else if (robots.size() > 1) {
      int zero = robots.get(0).getLocation().distanceSquaredTo(rc.getLocation());
      int one = robots.get(1).getLocation().distanceSquaredTo(rc.getLocation());
      if (zero < one) {
        rc.attack(robots.get(0).getLocation());
      }
      else {
        rc.attack(robots.get(1).getLocation());
      }
    }
  }

  private void build() throws GameActionException {

    if (rc.getRoundNum() < 50 || rc.getMoney() > 1200 + (Math.sqrt(rc.getLocation().distanceSquaredTo(lib.center)) * 5)) {
      for (Direction dir : lib.directionsToMiddle(rc.getLocation())) {
        if (rc.canBuildRobot(getBestRobot(), rc.getLocation().add(dir))) {
          rc.buildRobot(getBestRobot(), rc.getLocation().add(dir));
          spawnedUnits++;
        }
      }
    }
  }

  // todo, update
  private UnitType getBestRobot() {
    if (rc.getRoundNum() < 50) {
      return UnitType.SOLDIER;
    }
    return spawnedUnits % 3 == 0 ? UnitType.SOLDIER : (spawnedUnits % 2 == 0 ? UnitType.MOPPER : UnitType.SPLASHER);
  }
}

//TODO: AoE
/**
 * each type of tower follows closely a 2:1 attack ratio for amount of damage for
 * each tower should determine which type of attack to use (single-block or area)
 *
 * x enemy robots, if x<=2 then use single block, else use area
 *
 */

//TODO: uprgrading towers
/**
 * each tower can be upgraded to lvl2 and lvl3, each costing 250 and 500 respectively
 * money towers are worth upgrading and should be done first as the money is recouped after 50 rds
 * and 100 rds. paint towers should be upgraded next, and defense towers shouldn't really be upgraded
 * at all. there isn't enough incentive, the hp increase and damage increase isn't nearly enough
 *
 */
