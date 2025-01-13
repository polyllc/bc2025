package poly;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.RobotController;

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
    PAINT_WORLD
  }

  SplasherTask currentTask = SplasherTask.EXPLORE;


  @Override
  public void takeTurn() throws GameActionException {

    updateNearbyTowers();

    rc.setIndicatorString("lG: " + locationGoing.toString()
        + " | cT: " + currentTask
        + " | dG: " + directionGoing);

    move();
    paint();

    if (rc.getPaint() < 51) {
      locationGoing = spawnedTower;
      currentTask = SplasherTask.GOING_BACK_TO_TOWER;
    }

    if (rc.getLocation().distanceSquaredTo(spawnedTower) < 4) {
      rc.transferPaint(spawnedTower, 150);
      currentTask = SplasherTask.EXPLORE;
    }
  }

  private void paint() throws GameActionException {
    currentTask = SplasherTask.PAINT_WORLD;
    splashWorld();
    currentTask = SplasherTask.EXPLORE;

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

  private void splashWorld() throws GameActionException {
    if (!splashEnemyPaint()) {
    }
    //when new lib function is made, put all code in that
    if (!rc.senseMapInfo(rc.getLocation()).getPaint().isAlly()) {
      rc.attack(rc.getLocation());
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir).add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation().add(dir))) {
          rc.attack(currentTile.getMapLocation());
        }
      }
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation().add(dir))) {
          rc.attack(currentTile.getMapLocation());
        }
      }
    }




  }
  protected void move() throws GameActionException {
    if (currentTask == SplasherTask.EXPLORE) {
      explore();
      locationGoing = Lib.noLoc;
    }
    super.move();
  }


}