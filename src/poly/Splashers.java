package poly;

import battlecode.common.*;

public class Splashers extends MovableUnit {

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
    GOING_BACK_TO_PAINT_TOWER,
    PAINT_WORLD
  }

  SplasherTask currentTask = SplasherTask.EXPLORE;

  MapLocation nearestPaintTower = Lib.noLoc;


  @Override
  public void takeTurn() throws GameActionException {
    lib.resourcePatternPainting();
    updateNearbyTowers();

    if (rc.getPaint() < 51) {
      getNearestPaintTower();
      if (nearestPaintTower != Lib.noLoc) {
        locationGoing = nearestPaintTower;
        currentTask = SplasherTask.GOING_BACK_TO_PAINT_TOWER;
      }
      else {
        locationGoing = spawnedTower;
        currentTask = SplasherTask.GOING_BACK_TO_TOWER;
      }
    }

    if (currentTask == SplasherTask.GOING_BACK_TO_TOWER) {
      getNearestPaintTower();
      if (nearestPaintTower != Lib.noLoc) {
        locationGoing = nearestPaintTower;
        currentTask = SplasherTask.GOING_BACK_TO_PAINT_TOWER;
      }
    }



    if (currentTask == SplasherTask.GOING_BACK_TO_PAINT_TOWER &&
            rc.canTransferPaint(nearestPaintTower, -50)) {
      rc.transferPaint(nearestPaintTower, -50);
      if (rc.getPaint() > 199) {
        currentTask = SplasherTask.EXPLORE;
      }
    }

    rc.setIndicatorString("lG: " + locationGoing.toString()
        + " | cT: " + currentTask
        + " | dG: " + directionGoing);

    move();

    if (currentTask == SplasherTask.EXPLORE) {
      paint();
    }

  }

  private void getNearestPaintTower() throws GameActionException {
    if (!paintTowerLocations.isEmpty()) {
      int distance = rc.getLocation().distanceSquaredTo(paintTowerLocations.get(0));
      for (MapLocation tower : paintTowerLocations) {
        if (distance > rc.getLocation().distanceSquaredTo(tower)) {
          nearestPaintTower = tower;
          distance = rc.getLocation().distanceSquaredTo(tower);
        }
      }
    }
    else {
      nearestPaintTower = Lib.noLoc;
    }

  }

  private void paint() throws GameActionException {
    currentTask = SplasherTask.PAINT_WORLD;
    splashWorld();
    currentTask = SplasherTask.EXPLORE;

  }

  private void splashWorld() throws GameActionException {

    //check the nearby map infos and see which one produces the best score
    MapInfo[] infos = rc.senseNearbyMapInfos(4);
    int maxScore = getAttackScore(infos[0].getMapLocation());
    MapLocation maxLocation = infos[0].getMapLocation();
    for (MapInfo info : infos) {
      int currScore = getAttackScore(info.getMapLocation());
      if (currScore > maxScore) {
        maxScore = currScore;
        maxLocation = info.getMapLocation();
      }
    }

    //if the score isnt above 4, it isnt worth an attack, keep searching
    if (maxScore > 3 && rc.canAttack(maxLocation)) {
      attack(maxLocation);
    }

  }

  private void attack(MapLocation loc) throws GameActionException {
    rc.attack(loc);
  }

  protected void move() throws GameActionException {
    if (currentTask == SplasherTask.EXPLORE) {
      explore();
      locationGoing = Lib.noLoc;
    }
    super.move();
  }

  private int getAttackScore(MapLocation center) throws GameActionException {
    MapInfo[] infos = rc.senseNearbyMapInfos(center, 4);
    int score = 0;
    for (MapInfo info : infos) {

      //if it is enemy paint and will be effected by the inner splash radius that can repaint
      if (info.getMapLocation().isWithinDistanceSquared(center, 2) &&
              info.getPaint().isEnemy()) {
        score += 4;
      }
      //if it is one of our towers or a wall
      if (towerLocations.contains(info.getMapLocation()) || info.isWall()) {
        score -= 1;
      }
      //if it is one of their towers
      else if (!info.isPassable()) {
        score += 5;
      }
      //if it is allied paint
      if (info.getPaint().isAlly()) {
        score -= 1;
      }
      //if it is neutral paint
      if (!info.getPaint().isEnemy() && !info.getPaint().isAlly()) {
        score += 2;
      }
      //if it is a mark
      if (info.getMark().isAlly()) {
        score -= 100;
      }
      //if it is standing on non-allied paint
      if (rc.getLocation() == center && !info.getPaint().isAlly()) {
        score += 3;
      }
    }

    return score;
  }


}