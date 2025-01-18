package poly;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

import java.util.ArrayList;

public abstract class Tower extends Unit {

  protected int spawnTurn = 0;

  private int spawnedUnits = 0;

  private int mopperBuildCooldown = 0;

  protected Tower(RobotController rc) throws GameActionException {
    super(rc);
    spawnTurn = rc.getRoundNum();
  }

  public void takeTurn() throws GameActionException {
    attack();

    if (rc.getRoundNum() < 8){
      build();
    }
    else if (rc.getRoundNum() > ((rc.getMapWidth() + 10) - ((rc.getMapWidth() / 6) * rc.getNumberTowers()))) {
      build();
    }

    upgrade();
    mopperBuildCooldown--;

    clearMarksAroundMe();
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

    if (rc.getRoundNum() < 200 && rc.getRoundNum() > 50) {
      for (MapInfo info : lib.nearbyTiles()) {
        if (!info.getPaint().isAlly() && info.getPaint() != PaintType.EMPTY) {
          if (mopperBuildCooldown <= 0) {
            for (Direction dir : lib.directionsToMiddle(rc.getLocation(), info.getMapLocation())) {
              if (rc.canBuildRobot(UnitType.MOPPER, rc.getLocation().add(dir).add(dir))) {
                rc.buildRobot(UnitType.MOPPER, rc.getLocation().add(dir).add(dir));
                mopperBuildCooldown = 40;
              }
            }
          }
        }
      }
    }

    //System.out.println("cost to build: " + (1200 + (Math.sqrt(rc.getLocation().distanceSquaredTo(lib.center)) * 7)));
    if (rc.getRoundNum() < 65 || rc.getMoney() > 1200 + (Math.sqrt(rc.getLocation().distanceSquaredTo(lib.center)) * 7)) { // todo screw around with the directions at times
      if (rc.getRoundNum() % 6 == 0 && rc.getRoundNum() > 300) {
        for (Direction dir : lib.directionsToMiddle(rc.getLocation(), rc.getLocation().add(rc.getLocation().directionTo(lib.center).opposite()))) {
          MapLocation loc = rc.getLocation().add(dir);
          if (rc.getRoundNum() < 10) {
            loc = loc.add(dir);
          }
          if (rc.canBuildRobot(getBestRobot(), loc)) {
            rc.buildRobot(getBestRobot(), loc);
            spawnedUnits++;
          }
          else if (rc.canBuildRobot(getBestRobot(), loc.add(dir.opposite()))) {
            rc.buildRobot(getBestRobot(), loc.add(dir.opposite()));
            spawnedUnits++;
          }
        }
      }
      for (Direction dir : lib.directionsToMiddle(rc.getLocation(), lib.center)) {
        MapLocation loc = rc.getLocation().add(dir);
        if (rc.getRoundNum() < 10) {
          loc = loc.add(dir);
        }
        if (rc.canBuildRobot(getBestRobot(), loc)) {
          rc.buildRobot(getBestRobot(), loc);
          spawnedUnits++;
        }
        else if (rc.canBuildRobot(getBestRobot(), loc.add(dir.opposite()))) {
          rc.buildRobot(getBestRobot(), loc.add(dir.opposite()));
          spawnedUnits++;
        }
      }
    }
  }

  // todo, update
  private UnitType getBestRobot() {

    if (rc.getRoundNum() < 100) {
      return UnitType.SOLDIER;
    }
    else if (rc.getRoundNum() < 40) {
      return (spawnedUnits % 2 == 0 ? UnitType.SOLDIER : UnitType.MOPPER);
    }
    else if (rc.getNumberTowers() > 10) {
      return spawnedUnits % 3 == 0 ? UnitType.SOLDIER : (spawnedUnits % 2 == 0 ? UnitType.MOPPER : UnitType.SPLASHER);
    }
    return spawnedUnits % 2 == 0 ? UnitType.SOLDIER : (spawnedUnits % 3 == 0 ? UnitType.MOPPER : UnitType.SPLASHER);
  }

  private void upgrade() throws GameActionException {
    if (rc.getMoney() > 1700) {
      if (rc.canUpgradeTower(rc.getLocation())) {
        rc.upgradeTower(rc.getLocation());
      }
    }
  }

  private void clearMarksAroundMe() throws GameActionException {
    for (Direction dir : Lib.directions) {
      for (Direction d : Lib.directionsCenter) {
        MapLocation newLoc = rc.getLocation().add(dir).add(d);
        if (rc.canMark(newLoc)) {
          rc.mark(newLoc, getPaintMarker(newLoc));
        }
      }
    }
  }

  private boolean getPaintMarker(MapLocation loc) {
    if (loc.x % 4 == 0) { // 1st column
      if ((loc.y - 2) % 4 == 0) {
        return false;
      }
      return true;
    }
    else {
      boolean col24 = (loc.y - 4) % 4 == 0 || loc.y % 4 == 0;
      if ((loc.x - 1) % 4 == 0) { // 2nd column
        if (col24) { //1st and 5th row
          return true;
        }
        return false;
      }
      else if ((loc.x - 2) % 4 == 0) { // 3rd column
        if ((loc.y - 2) % 4 == 0) {
          return true;
        }
        return false;
      }
      else if ((loc.x - 3) % 4 == 0) { // 4th column
        if (col24) { //1st and 5th row
          return true;
        }
        return false;
      }
      else {
        if ((loc.y - 2) % 4 == 0) {
          return false;
        }
        return true;
      }
    }
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
