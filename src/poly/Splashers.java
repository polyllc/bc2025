package poly;

import battlecode.common.*;

public class Splashers extends MovableUnit {

  MapLocation nearestPaintTower = null;

  public Splashers(RobotController rc) throws GameActionException {
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
    PAINT_WORLD,
    FINDING_TOWER
  }

  SplasherTask currentTask = SplasherTask.EXPLORE;


  @Override
  public void takeTurn() throws GameActionException {

    updateNearbyTowers();

    rc.setIndicatorString("lG: " + locationGoing.toString()
        + " | cT: " + currentTask
        + " | dG: " + directionGoing);

    move();

    if (currentTask.equals(SplasherTask.EXPLORE)) {
      paint();
    }

    if (rc.getPaint() < 51) {
      if (this.getNearestPaintTower() != null) {
        locationGoing = this.getNearestPaintTower();
        nearestPaintTower = this.getNearestPaintTower();
        currentTask = SplasherTask.GOING_BACK_TO_TOWER;
      }
      else {
        locationGoing = spawnedTower;
        currentTask = SplasherTask.FINDING_TOWER;
      }
    }

    if (currentTask == SplasherTask.FINDING_TOWER) {
      if (this.getNearestPaintTower() != null) {
        nearestPaintTower = this.getNearestPaintTower();
        locationGoing = this.getNearestPaintTower();
        currentTask = SplasherTask.GOING_BACK_TO_TOWER;
      }
    }

    if (rc.getLocation().distanceSquaredTo(nearestPaintTower) < 4) {
      rc.transferPaint(spawnedTower, 50);
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
        if (!currentTile.getPaint().isAlly() && rc.canAttack(currentTile.getMapLocation())) {
          rc.attack(currentTile.getMapLocation());
          return true;
        }
      }
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir).add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.getPaint().isAlly() && rc.canAttack(currentTile.getMapLocation())) {
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
      if (rc.canAttack(rc.getLocation())) {
        rc.attack(rc.getLocation());
      }
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir).add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.getPaint().isAlly() && rc.canAttack(currentTile.getMapLocation())) {
          rc.attack(currentTile.getMapLocation());
        }
      }
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.getPaint().isAlly() && rc.canAttack(currentTile.getMapLocation())) {
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

  private MapLocation getNearestPaintTower() throws GameActionException {
    for (MapLocation tower : towerLocations) {
      if (rc.senseRobotAtLocation(tower).getType().equals(UnitType.LEVEL_ONE_PAINT_TOWER) ||
      rc.senseRobotAtLocation(tower).getType().equals(UnitType.LEVEL_TWO_PAINT_TOWER) ||
      rc.senseRobotAtLocation(tower).getType().equals(UnitType.LEVEL_THREE_PAINT_TOWER)) {
        return tower;
      }
    }
      return null;
  }

  private int getSplashScore() throws GameActionException {
    return 0;
  }


}