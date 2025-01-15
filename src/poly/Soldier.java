package poly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    GETTING_MORE_PAINT,
    WAITING_FOR_FUNDS
  }

  static final Random rng = new Random(6147);

  MapInfo currentRuin = null;

  MapLocation taskLocation = Lib.noLoc;

  MapLocation previousLocationGoing = Lib.noLoc;

  SoldierTask currentTask = SoldierTask.EXPLORING;
  SoldierTask previousTask = SoldierTask.EXPLORING;



  List<MapLocation> previousRuins = new ArrayList<MapLocation>();
  List<Integer> previousRuinsRounds = new ArrayList<Integer>();


  public Soldier(RobotController rc) throws GameActionException {
    super(rc);
    directionGoing = rc.getLocation().directionTo(lib.center);
    if (rc.getRoundNum() % 2 == 0) {
      directionGoing = directionGoing.rotateLeft();
    }
    else {
      directionGoing = directionGoing.rotateRight();
    }
    if (rc.getRoundNum() % 3 == 0) {
      directionGoing = rc.getLocation().directionTo(lib.center);
    }

    if (rc.getRoundNum() > 100) {
      if (rc.getRoundNum() % 4 == 2) {
        directionGoing = directionGoing.rotateLeft();
      }
      else if (rc.getRoundNum() % 4 == 1) {
        directionGoing = directionGoing.rotateRight();
      }
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

    if (currentRuin == null && rc.getNumberTowers() < 25) {
      searchForRuin();
    }

    if (currentTask == SoldierTask.EXPLORING) {
      locationGoing = Lib.noLoc;
      goTowardsEmptySpots();
    }

    if (currentTask == SoldierTask.WAITING_FOR_FUNDS) {
      if (rc.getMoney() >= 1000) {
        locationGoing = currentRuin.getMapLocation();
        currentTask = SoldierTask.PAINTING_RUIN;
      }
    }

    if (currentTask == SoldierTask.PAINTING_RUIN) {
      if (locationGoing.equals(Lib.noLoc)) {
        currentTask = SoldierTask.EXPLORING;
      }
    }
    move();
    if (currentTask != SoldierTask.GETTING_MORE_PAINT) {
      paint();
      nav.avoidNonAllyPaint = false;
    }
    checkToClearRuin();

    if (rc.getPaint() < 20 && currentTask != SoldierTask.GETTING_MORE_PAINT && currentTask != SoldierTask.WAITING_FOR_FUNDS) {
      previousTask = currentTask;
      previousLocationGoing = locationGoing;
      currentTask = SoldierTask.GETTING_MORE_PAINT;
      locationGoing = spawnedTower;
    }

    if (currentTask == SoldierTask.GETTING_MORE_PAINT) {
      nav.avoidNonAllyPaint = true;
      senseBetterTowersToGetPaint();
      if (rc.getLocation().distanceSquaredTo(locationGoing) < 3) {
        RobotInfo towerInfo = rc.senseRobotAtLocation(locationGoing);
        int max = towerInfo.getPaintAmount() < Math.clamp(90 - rc.getRoundNum() / 3, 40, 90) ? -Math.clamp(90 - rc.getRoundNum() / 3, 40, 90) :
                -Math.min(towerInfo.getPaintAmount(), 150 - Math.clamp(rc.getPaint(), 0, 150));
        //max = Math.min(100, Math.max(towerInfo.getPaintAmount(), max));
        if (rc.canTransferPaint(locationGoing, max)) {
          rc.transferPaint(locationGoing, max);
          currentTask = previousTask;
          locationGoing = previousLocationGoing;
        }
      }
      if (rc.getLocation().distanceSquaredTo(locationGoing) < 20) {
        if (rc.canSenseRobotAtLocation(locationGoing)) {
          if (rc.senseRobotAtLocation(locationGoing).getPaintAmount() < 40) {
            currentTask = previousTask;
            locationGoing = previousLocationGoing;
          }
        }
      }
    }



    decreaseRuinRounds();

  }

  private void senseBetterTowersToGetPaint() throws GameActionException {
    for (MapLocation towers : towerLocations) {
      if (rc.canSenseRobotAtLocation(towers)) {
        RobotInfo tower = rc.senseRobotAtLocation(towers);
        if (tower.getPaintAmount() > 50) {
          locationGoing = towers;
        }
      }
    }
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
        resetFromPaintingRuin();
      }
      resourcePatternPainting();
    }
    // Try to paint beneath us as we walk to avoid paint penalties.
    // Avoiding wasting paint by re-painting our own tiles.

    if (rc.getRoundNum() > 40 || rc.getPaint() < 140) {
      MapInfo[] possiblePaintLocations = rc.senseNearbyMapInfos(8);
      Arrays.sort(possiblePaintLocations, (a, b) -> a.getMapLocation().distanceSquaredTo(rc.getLocation()) - b.getMapLocation().distanceSquaredTo(rc.getLocation()));
      for (MapInfo loc : possiblePaintLocations) {
        if (!loc.getPaint().isAlly() && rc.canAttack(loc.getMapLocation())) {
          boolean useSecondaryColor = loc.getMark() == PaintType.ALLY_SECONDARY;
          rc.attack(loc.getMapLocation(), useSecondaryColor);
          return;
        }
        else if (loc.getMark() != loc.getPaint()) {
          if (rc.canAttack(loc.getMapLocation())) {
            boolean useSecondaryColor = loc.getMark() == PaintType.ALLY_SECONDARY;
            rc.attack(loc.getMapLocation(), useSecondaryColor);
            return;
          }
        }
      }
    }

  }

  private void resetFromPaintingRuin() {
    currentTask = SoldierTask.EXPLORING;
    locationGoing = Lib.noLoc;
  }

  private void paintRuin() throws GameActionException {
    MapLocation targetLoc = locationGoing;
    Direction dir = rc.getLocation().directionTo(targetLoc);
    // Mark the pattern we need to draw to build a tower here if we haven't already.
    if (rc.canSenseLocation(targetLoc)) {
      MapLocation shouldBeMarked = currentRuin.getMapLocation().subtract(dir);
      if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY) {
        if (rc.canMarkTowerPattern(getBestTowerToMark(), targetLoc)) {
          rc.markTowerPattern(getBestTowerToMark(), targetLoc);
          System.out.println("Trying to build a tower at " + targetLoc);
        }
      }

      int completedSpots = 0;
      // Fill in any spots in the pattern with the appropriate paint.
      for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)) {
        if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
          if (patternTile.getPaint() != PaintType.ENEMY_PRIMARY && patternTile.getPaint() != PaintType.ENEMY_SECONDARY) {
            boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
            if (rc.canAttack(patternTile.getMapLocation())) {
              System.out.println("PR Painted " + patternTile.getMapLocation());
              rc.attack(patternTile.getMapLocation(), useSecondaryColor);
            }
          }
        }
        if (patternTile.getMark() == patternTile.getPaint() && patternTile.getPaint() != PaintType.EMPTY) {
          completedSpots++;
        }
      }

      if (completedSpots >= 25 && rc.getMoney() < 1000) {
        currentTask = SoldierTask.WAITING_FOR_FUNDS;
        return;
      }

      // Complete the ruin if we can.
      boolean money = rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc);
      boolean defense = rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc);
      boolean paint = rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
      if (money || defense || paint) {
        if (money) {
          rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc);
        } else if (defense) {
          rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc);
        } else {
          rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
        }
        rc.setTimelineMarker("Tower built", 0, 255, 0);
        System.out.println("Built a tower at " + targetLoc + "!");
        currentTask = SoldierTask.EXPLORING;
        locationGoing = Lib.noLoc;
        currentRuin = null;
      }


    }
  }

  private void searchForRuin() {
    MapInfo[] nearbyTiles = lib.nearbyTiles();
    // todo, return to "old" ruins
    for (MapInfo tile : nearbyTiles) {
      if (tile.hasRuin()) {
        if (!rc.canSenseRobotAtLocation(tile.getMapLocation())) {
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
        if (totalFilled > 0 && rc.getRoundNum() > 40) {
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
    if (currentTask == SoldierTask.EXPLORING && rc.getRoundNum() > 100) {
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


//  private void goTowardsEmptySpots() {
//    if (currentTask == SoldierTask.EXPLORING && rc.getRoundNum() > 100) {
//      MapInfo[] nearbyTiles = lib.nearbyTiles();
//      Arrays.sort(nearbyTiles, (a, b) -> a.getMapLocation().distanceSquaredTo(lib.center) - b.getMapLocation().distanceSquaredTo(lib.center));
//
//
//
//      for (MapInfo tile : nearbyTiles) {
//        if (tile.isPassable()) {
//          if (tile.getPaint() == PaintType.EMPTY) { // todo, make this more random
//            directionGoing = rc.getLocation().directionTo(tile.getMapLocation());
//            return;
//          }
//        }
//      }
//    }
//  }

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
      return rc.getRoundNum() % 2 == 0 ?
          UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;
    }
    return rng.nextInt(0, 3) == 0 ? UnitType.LEVEL_ONE_DEFENSE_TOWER :
        (rng.nextInt(0, 2) == 0 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER);
  }

  private void resourcePatternPainting() throws GameActionException {
    if (rc.getPaint() > 50 && rc.getRoundNum() > 80) {
      for (MapInfo info : lib.nearbyTiles()) {
        if ((info.getMapLocation().x - 3) % 5 == 0 && (info.getMapLocation().y - 3) % 5 == 0) {
          for (MapInfo paintInfo : rc.senseNearbyMapInfos(5)) {
            if (paintInfo.hasRuin()) {
              return;
            } else if (paintInfo.getMark() == PaintType.ALLY_PRIMARY || paintInfo.getMark() == PaintType.ALLY_SECONDARY) {
              return;
            } else if (rc.canMarkResourcePattern(info.getMapLocation())) {
              rc.markResourcePattern(info.getMapLocation());
              return;
            } else if (rc.canCompleteResourcePattern(info.getMapLocation())) {
              rc.completeResourcePattern(info.getMapLocation());
              return;
            }
          }

        }
      }
    }
  }

}

