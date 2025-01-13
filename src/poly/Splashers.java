package poly;

import battlecode.common.*;

import java.util.List;

public class Splashers extends MovableUnit {

  public Splashers(RobotController rc) {
    super(rc);
    directionGoing = rc.getLocation().directionTo(lib.center);
    if (rc.getID() % 2 == 0) {
      directionGoing = directionGoing.rotateLeft();
    }
    else {
      directionGoing = directionGoing.rotateRight();
    }
    if (rc.getID() % 3 == 0) {
      directionGoing = rc.getLocation().directionTo(lib.center);
    }
  }

  public enum SplasherTask {
    EXPLORE,
    GOING_BACK_TO_TOWER,
    GOING_TO_PAINT_TOWER,
    PAINT_WORLD
  }

  SplasherTask currentTask = SplasherTask.EXPLORE;

  MapLocation pTower;

  private MapLocation getPTower() {
    for (MapLocation tower : towerLocations) {
      //check if the tower is a paint tower, if so make that
      //the location going for the splasher
      //if tower is paint tower, set currentTask to going to paint tower
      //because if there are no paint towers in the game (a real early game situation)
      //it will automatically go back to own tower and never seek a paint tower which it
      //should every turn untl it finds one
    }
    return null;
  }


  @Override
  public void takeTurn() throws GameActionException {

    updateNearbyTowers();

    rc.setIndicatorString("lG: " + locationGoing.toString()
        + " | cT: " + currentTask
        + " | dG: " + directionGoing);

    move();

    if (currentTask != SplasherTask.GOING_BACK_TO_TOWER) {
      paint();
    }


    if (rc.getPaint() < 51) {
      locationGoing = spawnedTower;
      currentTask = SplasherTask.GOING_BACK_TO_TOWER;
    }

    if (rc.getLocation().distanceSquaredTo(spawnedTower) < 4) {
      rc.transferPaint(spawnedTower, 50);
    }

    if (currentTask == SplasherTask.GOING_BACK_TO_TOWER && rc.getPaint() > 199) {
      currentTask = SplasherTask.EXPLORE;
    }
  }

  private void paint() throws GameActionException {
    currentTask = SplasherTask.PAINT_WORLD;
    splashWorld();
    currentTask = SplasherTask.EXPLORE;

  }


  private void splashWorld() throws GameActionException {
    if (!splashEnemyPaint()) {
    }
    //when new lib function is made, put all code in that

    //if not standing on allied paint, splash current location (self preservation)
    if (!rc.senseMapInfo(rc.getLocation()).getPaint().isAlly()) {
      rc.attack(rc.getLocation());
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir).add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.hasRuin() && currentTile.isPassable() && !currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation().add(dir))) {
          rc.attack(currentTile.getMapLocation());
        }
      }
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.hasRuin() && currentTile.isPassable() && !currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation().add(dir))) {
          rc.attack(currentTile.getMapLocation());
        }
      }
    }

  }

  private boolean splashEnemyPaint() throws GameActionException {
    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation().add(dir))) {
          rc.attack(currentTile.getMapLocation());
          return true;
        }
      }
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir).add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation().add(dir))) {
          //above if should be lib.isEnemyPaint() when made
          rc.attack(currentTile.getMapLocation());
          return true;
        }
      }
    }

    return false;
  }
  protected void move() throws GameActionException {
    if (currentTask == SplasherTask.EXPLORE) {
      explore();
      locationGoing = Lib.noLoc;
    }
    super.move();
  }


}