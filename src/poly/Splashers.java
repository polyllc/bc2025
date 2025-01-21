package poly;

import battlecode.common.*;

public class Splashers extends MovableUnit {

  public Splashers(RobotController rc) throws GameActionException {
    super(rc);
    directionGoing = rc.getLocation().directionTo(lib.center);

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

    if (rc.getPaint() < 61) {
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

    goTowardsEmptySpots();
    if (currentTask == SplasherTask.EXPLORE) {
      paint();
    }

  }

  private void getNearestPaintTower() throws GameActionException {
    if (!paintTowerLocations.isEmpty()) {
      int distance = rc.getLocation().distanceSquaredTo(paintTowerLocations.get(0));
      for (MapLocation tower : paintTowerLocations) {
        if (distance < rc.getLocation().distanceSquaredTo(tower)) {
          nearestPaintTower = tower;
          distance = rc.getLocation().distanceSquaredTo(tower);
        }
      }
    }

    else {
      nearestPaintTower = Lib.noLoc;
    }

    /*
    try {
      for (MapLocation tower : towerLocations) {
        if (rc.senseRobotAtLocation(tower).getType().equals(UnitType.LEVEL_ONE_PAINT_TOWER) ||
                rc.senseRobotAtLocation(tower).getType().equals(UnitType.LEVEL_TWO_PAINT_TOWER) ||
                rc.senseRobotAtLocation(tower).getType().equals(UnitType.LEVEL_THREE_PAINT_TOWER)) {
          nearestPaintTower = tower;
        }
      }
    }
    catch (GameActionException e) {
      nearestPaintTower = Lib.noLoc;
    }
     */

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

    //if the splasher is standing on non-allied paint, splash that area IMMEDIATELY (self preservation)
    if (!rc.senseMapInfo(rc.getLocation()).getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
      attack(rc.getLocation());
    }

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
    if (maxScore > 4 && rc.canAttack(maxLocation)) {
      attack(maxLocation);
    }

    //old code, delete later if above works
    /*
    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir).add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (currentTile.isPassable() && !currentTile.getPaint().isAlly() && rc.canAttack(currentTile.getMapLocation())) {
          attack(currentTile.getMapLocation());
        }
      }
    }

    for (Direction dir : Lib.directionsCenter) {
      if (rc.canSenseLocation(rc.getLocation().add(dir))) {
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
        if (!currentTile.isPassable() && !currentTile.getPaint().isAlly() && rc.canAttack(currentTile.getMapLocation())) {
          attack(currentTile.getMapLocation());
        }
      }
    }
     */

  }

  private void attack(MapLocation loc) throws GameActionException {
    /*
    for (MapInfo info : rc.senseNearbyMapInfos(loc, 8)) {
      if (info.getMark().isAlly()) {
        return;
      }
    }
     */
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
      if (info.getMapLocation().isWithinDistanceSquared(center, 2) &&
              info.getPaint().isEnemy()) {
        score += 50;
      }
      else if (!info.isPassable()) {
        score -= 1;
      }
      else if (info.getPaint().isAlly()) {
        score -= 2;
      }
      else if (!info.getPaint().isEnemy() && !info.getPaint().isAlly()) {
        score += 1;
      }
      else if (info.getMark().isAlly()) {
        score -= 10;
      }
    }

    return score;
  }


  private void goTowardsEmptySpots() throws GameActionException {
    MapInfo[] nearbyTiles = lib.nearbyTiles();
    int bestTileDistance = 999999;

      for (MapInfo tile : nearbyTiles) {
        if (tile.isPassable()) {
          if (tile.getPaint() == PaintType.EMPTY) { // todo, make this more random
            int distance = tile.getMapLocation().distanceSquaredTo(lib.center);
            if (bestTileDistance < distance) {
              bestTileDistance = distance;
              directionGoing = rc.getLocation().directionTo(tile.getMapLocation());
            }
          }
        }
      }
  }

}