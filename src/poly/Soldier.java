package poly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public class Soldier extends MovableUnit {


  public enum SoldierTask {
    PAINTING_RUIN,
    EXPLORING,
    PAINTING,
    WAITING_TO_BUILD_RUIN,
    GETTING_MORE_PAINT
  }

  static final Random rng = new Random(6147);

  MapInfo currentRuin = null;

  MapLocation taskLocation = Lib.noLoc;

  MapLocation previousLocationGoing = Lib.noLoc;

  SoldierTask currentTask = SoldierTask.EXPLORING;
  SoldierTask previousTask = SoldierTask.EXPLORING;



  List<MapLocation> previousRuins = new ArrayList<MapLocation>();
  List<Integer> previousRuinsRounds = new ArrayList<Integer>();


  public Soldier(RobotController rc) {
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
    nav.avoidEnemyPaint = true;
    nav.avoidEnemyTowers = true;
    // todo, the tower that spawn this robot might have an objective
    //  which may be in a message sent on over
  }


  @Override
  public void takeTurn() throws GameActionException {

    updateNearbyTowers();

    rc.setIndicatorString("lG: " + locationGoing.toString()
            + " | cT: " + currentTask
            + " | dG: " + directionGoing
            + " | cR: " + currentRuin);

    if (currentRuin == null) {
      searchForRuin();
    }

    if (currentTask == SoldierTask.EXPLORING) {
      locationGoing = Lib.noLoc;
      goTowardsEmptySpots();
    }

    if (currentTask == SoldierTask.PAINTING_RUIN) {
      if (locationGoing.equals(Lib.noLoc)) {
        currentTask = SoldierTask.EXPLORING;
      }
    }
    move();
    if (currentTask != SoldierTask.GETTING_MORE_PAINT) {
      paint();
    }
    checkToClearRuin();

    if (rc.getPaint() < 0 && currentTask != SoldierTask.GETTING_MORE_PAINT) {
      previousTask = currentTask;
      previousLocationGoing = locationGoing;
      currentTask = SoldierTask.GETTING_MORE_PAINT;
      locationGoing = spawnedTower;
    }

    if (currentTask == SoldierTask.GETTING_MORE_PAINT) {
      if (rc.getLocation().distanceSquaredTo(locationGoing) < 3) {
        RobotInfo towerInfo = rc.senseRobotAtLocation(locationGoing);
        int max = Math.max(-50, -(200 - rc.getPaint()));
        //max = Math.min(100, Math.max(towerInfo.getPaintAmount(), max));
        if (rc.canTransferPaint(locationGoing, max)) {
          rc.transferPaint(locationGoing, max);
          currentTask = previousTask;
          locationGoing = previousLocationGoing;
        }
      }
    }



    decreaseRuinRounds();

  }

  protected void move() throws GameActionException {
    if (currentTask == SoldierTask.EXPLORING) {
      explore();
    }
    super.move();
  }

  private void paint() throws GameActionException {
    if (currentTask == SoldierTask.PAINTING_RUIN && currentRuin != null) {
      paintRuin();
    }
    else {
      if (currentTask == SoldierTask.PAINTING_RUIN) {
        currentTask = SoldierTask.EXPLORING;
        locationGoing = Lib.noLoc;
      }
    }
    // Try to paint beneath us as we walk to avoid paint penalties.
    // Avoiding wasting paint by re-painting our own tiles.

    MapInfo[] possiblePaintLocations = rc.senseNearbyMapInfos(8);
    Arrays.sort(possiblePaintLocations, (a, b) ->  a.getMapLocation().distanceSquaredTo(rc.getLocation()) - b.getMapLocation().distanceSquaredTo(rc.getLocation()));
    for (MapInfo loc : possiblePaintLocations) {
      if (!loc.getPaint().isAlly() && rc.canAttack(loc.getMapLocation())) {
       // System.out.println("Painted " + loc.getMapLocation());
        rc.attack(loc.getMapLocation());
      }
    }

//    for (Direction dir : Lib.directionsCenter) {
//      if (rc.canSenseLocation(rc.getLocation().add(dir))) {
//        MapInfo currentTile = rc.senseMapInfo(rc.getLocation().add(dir));
//        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation().add(dir))) {
//          System.out.println("Painted " + currentTile.getMapLocation());
//          rc.attack(currentTile.getMapLocation());
//        }
//      }
//    }
  }

  private void paintRuin() throws GameActionException {
    MapLocation targetLoc = locationGoing;
    Direction dir = rc.getLocation().directionTo(targetLoc);
    // Mark the pattern we need to draw to build a tower here if we haven't already.
    MapLocation shouldBeMarked = currentRuin.getMapLocation().subtract(dir);
    if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(getBestTowerToMark(), targetLoc)){
      rc.markTowerPattern(getBestTowerToMark(), targetLoc);
      System.out.println("Trying to build a tower at " + targetLoc);
    }
    // Fill in any spots in the pattern with the appropriate paint.
    for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)){
      if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
        if (patternTile.getPaint() != PaintType.ENEMY_PRIMARY && patternTile.getPaint() != PaintType.ENEMY_SECONDARY) {
          boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
          if (rc.canAttack(patternTile.getMapLocation())) {
            System.out.println("PR Painted " + patternTile.getMapLocation());
            rc.attack(patternTile.getMapLocation(), useSecondaryColor);
          }
        }
      }
    }
    // Complete the ruin if we can.
    if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc) ||
            rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc) ||
            rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)) {
      if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc)) {
        rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc);
      }
      else if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc)) {
        rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc);
      }
      else {
        rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
      }
      rc.setTimelineMarker("Tower built", 0, 255, 0);
      System.out.println("Built a tower at " + targetLoc + "!");
      currentTask = SoldierTask.EXPLORING;
      locationGoing = Lib.noLoc;
      currentRuin = null;
    }
  }

  private void searchForRuin() {
    MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
    // todo, return to "old" ruins
    for (MapInfo tile : nearbyTiles){
      if (tile.hasRuin() && !rc.canSenseRobotAtLocation(tile.getMapLocation())) {
        for (int i = 0; i < previousRuinsRounds.size(); i++) {
          if (previousRuins.get(i).equals(tile.getMapLocation())) {
            if (previousRuinsRounds.get(i) > 0) { // our ruins are still on cooldown
              return;
            }
          }
        }
        currentRuin = tile;
        currentTask = SoldierTask.PAINTING_RUIN;
        locationGoing = tile.getMapLocation();
        return;
      }
    }
  }

  //todo enemy paint
  private void checkToClearRuin() throws GameActionException {
    if (currentRuin != null) {
      if (rc.getNumberTowers() >= 25) {
        locationGoing = Lib.noLoc;
        previousRuins.add(currentRuin.getMapLocation());
        previousRuinsRounds.add(50);
        currentTask = SoldierTask.EXPLORING;
        currentRuin = null;
      }
      else if (rc.canSenseRobotAtLocation(currentRuin.getMapLocation())) {
        currentRuin = null;
        //directionGoing = rc.getLocation().directionTo(locationGoing).opposite();
        System.out.println("Cleared ruin");
        locationGoing = Lib.noLoc;
      } else if (rc.getLocation().distanceSquaredTo(currentRuin.getMapLocation()) < 5) {
        int totalFilled = 0;
        for (MapInfo patternTile : rc.senseNearbyMapInfos(currentRuin.getMapLocation(), 8)) {
          if (patternTile.getMark() != PaintType.EMPTY && patternTile.getPaint() == PaintType.ENEMY_PRIMARY
                  || patternTile.getMark() != PaintType.EMPTY && patternTile.getPaint() == PaintType.ENEMY_SECONDARY) {
            totalFilled++;
          }
        }
        if (totalFilled > 0) {
          previousRuins.add(currentRuin.getMapLocation());
          previousRuinsRounds.add(20);
          currentTask = SoldierTask.EXPLORING;
          currentRuin = null;
        }
      }
    }
  }

  private void decreaseRuinRounds() {
    for (int i = 0; i < previousRuinsRounds.size(); i++) {
      if (previousRuinsRounds.get(i) > 0) {
        previousRuinsRounds.set(i, previousRuinsRounds.get(i) - 1);
      }
    }
  }

  private void goTowardsEmptySpots() {
    if (currentTask == SoldierTask.EXPLORING) {
      MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
      for (MapInfo tile : nearbyTiles) {
        if (tile.isPassable()) {
          if (tile.getPaint() == PaintType.EMPTY) { // todo, make this more random
            directionGoing = rc.getLocation().directionTo(tile.getMapLocation());
            return;
          }
        }
      }
    }
  }

  private UnitType getBestTowerToMark() {
    /*
    if (rc.getRoundNum() < rc.getMapWidth()) {
      return UnitType.LEVEL_ONE_MONEY_TOWER;
    }
    else if (rc.getRoundNum() < 300) {
      return UnitType.LEVEL_ONE_PAINT_TOWER;
    }
    return rng.nextInt(0, 2) == 0 ?
            UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;

     */

    if (rc.getRoundNum() < rc.getMapWidth()) {
      return UnitType.LEVEL_ONE_MONEY_TOWER;
    }
    else if (rc.getRoundNum() < 500) {
      return rng.nextInt(0, 2) == 0 ?
          UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;
    }
    return rng.nextInt(0, 3) == 0 ? UnitType.LEVEL_ONE_DEFENSE_TOWER :
        (rng.nextInt(0, 2) == 0 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER);
  }

}

