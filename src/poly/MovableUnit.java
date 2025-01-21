package poly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public abstract class MovableUnit extends Unit {

  protected boolean stopMoving = false;
  protected Direction directionGoing = Direction.CENTER;
  protected MapLocation locationGoing = Lib.noLoc;
  protected Nav nav;
  protected boolean lastMovement = false;
  protected int turnsMovingInDirection = 0;
  protected MapLocation lastLocationGoing = Lib.noLoc;

  protected final Random rng = new Random(6147);

  protected List<MapLocation> towerLocations = new ArrayList<>();

  protected List<MapLocation> paintTowerLocations = new ArrayList<>();

  protected MapLocation spawnedTower = Lib.noLoc;


  public MovableUnit(RobotController rc) throws GameActionException {
    super(rc);
    nav = new Nav(rc);
    for (RobotInfo robot : rc.senseNearbyRobots(4)) {
      if (robot.getTeam() == rc.getTeam()) {
        if (lib.isTower(robot.getType())) {
          spawnedTower = robot.getLocation();
        }
      }
    }
  }

  protected void move() throws GameActionException {
    explore();
    if (locationGoing == Lib.noLoc) {
      if (directionGoing != Direction.CENTER) {
        nav.goTo(rc.getLocation().add(directionGoing));
        turnsMovingInDirection++;
      }
    } else {

      boolean goToResult = nav.goTo(locationGoing);
      lastMovement = goToResult; //if we need to save bytecode, well this is where we're saving it
      lastLocationGoing = locationGoing;
      if (!lastMovement) {
        lastMovement = goToResult;
        if (!lastMovement) {
            lastMovement = nav.navTo(locationGoing);
          if (!lastMovement) {
            lastMovement = nav.goTo(rc.getLocation().directionTo(locationGoing));
          }
        }
      }
    }
  }

  protected void explore() throws GameActionException {
    if (lib.detectCorner(directionGoing)) {
      // directionGoing = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
      if (rng.nextBoolean()) {
        directionGoing = directionGoing.rotateLeft().rotateLeft();
      } else {
        directionGoing = directionGoing.rotateRight().rotateRight();
      }
    }
    else if (turnsMovingInDirection > (rc.getMapHeight() + rc.getMapWidth()) / 2) {
      turnsMovingInDirection = 0;
      if (rng.nextBoolean()) {
        directionGoing = directionGoing.rotateLeft().rotateLeft();
      } else {
        directionGoing = directionGoing.rotateRight().rotateRight();
      }
    }
    else if (turnsMovingInDirection > (rc.getMapHeight() + rc.getMapWidth()) / 5) {
      if (lib.detectWall(rc.getLocation()) != Direction.CENTER) {
        directionGoing = lib.detectWall(rc.getLocation());
      }
    }
  }

  protected void updateNearbyTowers() {
    RobotInfo[] robots = lib.getRobots(false);
    for (RobotInfo robot : robots) {
      if (robot.getTeam() == rc.getTeam()) {
        if (lib.isTower(robot.getType())) {
          if (!towerLocations.contains(robot.getLocation())) {
            towerLocations.add(robot.getLocation());
            if (lib.isPaintTower(robot.getType())) {
              paintTowerLocations.add(robot.getLocation());
            }
          }
        }
      }
    }
  }

  protected Direction getRushDirection() {

    int myQuadrant = lib.getQuadrant();
    int towerQuadrant = lib.getQuadrant(spawnedTower);

    if (myQuadrant != towerQuadrant) {
      if (myQuadrant == 1 && towerQuadrant == 4 || myQuadrant == 4 && towerQuadrant == 1) {
        return Direction.WEST;
      }
      else if (myQuadrant == 1 && towerQuadrant == 2 || myQuadrant == 2 && towerQuadrant == 1) {
        return Direction.SOUTH;
      }
      else if (myQuadrant == 2 && towerQuadrant == 3 || myQuadrant == 3 && towerQuadrant == 2) {
        return Direction.EAST;
      }
      else {
        return Direction.NORTH;
      }
    }

    return rc.getLocation().directionTo(lib.center);
  }


  // updates list in Lib for where ally towers are
  protected void updateAllySpawns() throws GameActionException {
    lib.spawnLocations = towerLocations;
    lib.autofillEnemySpawnPoints(spawnedTower);
  }

  // gets the direction of the average enemy tower location
  // aka where the most "noise" is
  protected MapLocation averageEnemyTower() throws GameActionException {
    List<MapLocation> enemyTowers = lib.enemyTowerLocations();
    int x = 0;
    int y = 0;
    for (MapLocation tower: enemyTowers) {
      x+= tower.x;
      y+= tower.y;
    }
    x = x / enemyTowers.size();
    y = y / enemyTowers.size();

    MapLocation averageLoc = new MapLocation(x, y);
    return averageLoc;
  }
}
