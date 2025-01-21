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
    GETTING_MORE_PAINT,
    WAITING_FOR_FUNDS,
    ATTACKING_ENEMY_BASE,
    PAINTING_BOOST,
    WAITING_FOR_BOOST_FUNDS
  }

  static final Random rng = new Random(6147);

  MapInfo currentRuin = null;

  MapLocation taskLocation = Lib.noLoc;

  MapLocation previousLocationGoing = Lib.noLoc;
  Direction previousDirectionGoing = Direction.CENTER;

  SoldierTask currentTask = SoldierTask.EXPLORING;
  SoldierTask previousTask = SoldierTask.EXPLORING;

  int gettingPaintRounds = 0;

  MapLocation enemyTowerGoing = Lib.noLoc;


  List<MapLocation> previousRuins = new ArrayList<>();
  List<Integer> previousRuinsRounds = new ArrayList<>();

  List<MapLocation> emptySpots = new ArrayList<>(10);


  List<MapLocation> noGoBoosts = new ArrayList<>(10);

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

    nav.avoidEnemyPaint = false;
  //  nav.avoidEnemyTowers = true;
    // todo, the tower that spawn this robot might have an objective
    //  which may be in a message sent on over
  }


  @Override
  public void takeTurn() throws GameActionException {

    if (rc.getRoundNum() > 600) {
    //  rc.resign();
    }
    if (rc.getRoundNum() > 20) {
      nav.avoidEnemyPaint = true;
    }

    updateNearbyTowers();

    rc.setIndicatorString("lG: " + locationGoing.toString()
            + " | cT: " + currentTask
            + " | dG: " + directionGoing
            + " | cR: " + currentRuin);

    if (currentRuin == null && rc.getNumberTowers() < 25 && currentTask != SoldierTask.PAINTING_BOOST) {
      searchForRuin();
    }
    if (rc.getRoundNum() > 40 && currentTask == SoldierTask.EXPLORING) {
      searchForBoost();
    }
    checkToClearBoost();



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

    if (currentTask == SoldierTask.WAITING_FOR_BOOST_FUNDS) {
      if (rc.getMoney() >= 200) {
        currentTask = SoldierTask.PAINTING_BOOST;
      }
    }

    if (currentTask == SoldierTask.PAINTING_RUIN) {
      if (locationGoing.equals(Lib.noLoc)) {
        currentTask = SoldierTask.EXPLORING;
      }
    }

    if (currentTask == SoldierTask.PAINTING_BOOST) {
      if (locationGoing.equals(Lib.noLoc)) {
        currentTask = SoldierTask.EXPLORING;
      }
    }

    if (currentTask == SoldierTask.ATTACKING_ENEMY_BASE) {
      attackEnemyTowers();
    }

    if (currentTask != SoldierTask.GETTING_MORE_PAINT) {
      gettingPaintRounds = 0;
      paint();
      nav.avoidNonAllyPaint = false;

    }
    move();
    checkToClearRuin();

    if (rc.getPaint() < Math.min(30, 10 + rc.getLocation().distanceSquaredTo(spawnedTower)) && currentTask != SoldierTask.GETTING_MORE_PAINT && currentTask != SoldierTask.WAITING_FOR_FUNDS && currentTask != SoldierTask.WAITING_FOR_BOOST_FUNDS) {
      previousTask = currentTask;
      previousLocationGoing = locationGoing;
      previousDirectionGoing = directionGoing;
      currentTask = SoldierTask.GETTING_MORE_PAINT;
      locationGoing = spawnedTower;
    }

    if (currentTask == SoldierTask.GETTING_MORE_PAINT) {
      getMorePaint();
    }



    decreaseRuinRounds();

  }

  private void getMorePaint() throws GameActionException {
    if (gettingPaintRounds % 5 != 0) {
      nav.avoidNonAllyPaint = true;
      gettingPaintRounds++;
    }
    else {
      nav.avoidNonAllyPaint = false;
    }
    senseBetterTowersToGetPaint();

    if (rc.getRoundNum() % ((emptySpots.size() * 2) + 3) == 0) {
      addEmptySpots();
    }


    if (rc.getLocation().distanceSquaredTo(locationGoing) < 4) {
      RobotInfo towerInfo = rc.senseRobotAtLocation(locationGoing);
      int max = getMax(towerInfo);
      //max = Math.min(100, Math.max(towerInfo.getPaintAmount(), max));
      if (rc.canTransferPaint(locationGoing, max)) {
        rc.transferPaint(locationGoing, max);
        currentTask = previousTask;
        locationGoing = previousLocationGoing;
        directionGoing = previousDirectionGoing;
      }
    }
    if (rc.getLocation().distanceSquaredTo(locationGoing) < 20) {
      if (rc.canSenseRobotAtLocation(locationGoing)) {
        if (rc.senseRobotAtLocation(locationGoing).getPaintAmount() < 40) {
          addEmptySpots();
          emptySpots.sort((a, b) -> a.distanceSquaredTo(rc.getLocation()) - b.distanceSquaredTo(rc.getLocation()));
          currentTask = previousTask;
          locationGoing = previousLocationGoing;
          directionGoing = previousDirectionGoing;
        }
      }
    }
  }

  private int getMax(RobotInfo towerInfo) {
    int max = towerInfo.getPaintAmount() < Math.clamp(150 - rc.getRoundNum() / 3, 60, 150) ? -Math.clamp(150 - rc.getRoundNum() / 3, 60, 150) :
            -Math.min(towerInfo.getPaintAmount(), 150 - Math.clamp(rc.getPaint(), 0, 150));
    if (towerInfo.getType() == UnitType.LEVEL_ONE_MONEY_TOWER || towerInfo.getType() == UnitType.LEVEL_TWO_MONEY_TOWER || towerInfo.getType() == UnitType.LEVEL_THREE_MONEY_TOWER) {
      if (-towerInfo.getPaintAmount() < max && rc.getRoundNum() < 100) {
       // max = -towerInfo.getPaintAmount();
      }
    }
    return max;
  }

  private void senseBetterTowersToGetPaint() throws GameActionException {
    for (MapLocation towers : towerLocations) {
      if (rc.canSenseRobotAtLocation(towers)) {
        RobotInfo tower = rc.senseRobotAtLocation(towers);
        if (tower.getPaintAmount() > 30) {
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

    if (currentTask != SoldierTask.ATTACKING_ENEMY_BASE && currentTask != SoldierTask.PAINTING_BOOST) {
      findEnemyTowersToAttack();
    }

    if (currentTask == SoldierTask.PAINTING_BOOST) {
      paintBoost();
    }
    else if (currentTask == SoldierTask.PAINTING_RUIN && currentRuin != null) {
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

      if (currentTask == SoldierTask.WAITING_TO_BUILD_RUIN) {
        if (rc.getPaint() < 11) {
          return;
        }
      }

      MapInfo[] possiblePaintLocations = rc.senseNearbyMapInfos(8);
      if (currentTask == SoldierTask.PAINTING_RUIN && rc.getLocation().distanceSquaredTo(currentRuin.getMapLocation()) < 5) {
        Arrays.sort(possiblePaintLocations, (a, b) -> b.getMapLocation().distanceSquaredTo(rc.getLocation()) - a.getMapLocation().distanceSquaredTo(rc.getLocation()));
      }
      else {
        Arrays.sort(possiblePaintLocations, (a, b) -> a.getMapLocation().distanceSquaredTo(rc.getLocation()) - b.getMapLocation().distanceSquaredTo(rc.getLocation()));
      }

      boolean isNotRuin = true;
      MapLocation ruin = rc.getLocation();
      for (MapInfo loc : lib.nearbyTiles()) {
        if (loc.hasRuin()) {
          isNotRuin = false;
        }
      }

      for (MapInfo loc : possiblePaintLocations) {

        if (loc.getMark() == PaintType.ENEMY_PRIMARY || loc.getMark() == PaintType.ENEMY_SECONDARY) {
          if (rc.canAttack(loc.getMapLocation())) {
            rc.attack(loc.getMapLocation(), getPaintMarker(loc.getMapLocation()));
            return;
          }
        }

        if (loc.getPaint() == PaintType.EMPTY) {
          if (rc.canAttack(loc.getMapLocation())) {
            rc.attack(loc.getMapLocation(), getPaintMarker(loc.getMapLocation()));
            return;
          }
        }

        if (!isNotRuin) {
          if (rc.getLocation().directionTo(ruin) != rc.getLocation().directionTo(loc.getMapLocation())) {
            isNotRuin = true;
          }
        }

        if (loc.getPaint().isAlly() && currentTask != SoldierTask.PAINTING_RUIN) {
         // if (loc.getMark() != loc.getPaint() && isNotRuin) {
            if (paintType(loc.getPaint()) != getPaintMarker(loc.getMapLocation())) {
              if (rc.canAttack(loc.getMapLocation())) {
                rc.attack(loc.getMapLocation(), getPaintMarker(loc.getMapLocation()));
              }
            }
        //  }
        }
      }
    }
  }

  private boolean paintType(PaintType paintType) {
    return paintType == PaintType.ALLY_SECONDARY;
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
      // todo make sure that they dont clump together when painting so that they don't lose paint
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
       // return;
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
        if (rc.canTransferPaint(locationGoing, -(200 - rc.getPaint()))) {
          rc.transferPaint(locationGoing, -(200 - rc.getPaint()));
        }
        rc.setTimelineMarker("Tower built", 0, 255, 0);
        System.out.println("Built a tower at " + targetLoc + "!");
        currentTask = SoldierTask.EXPLORING;
        locationGoing = Lib.noLoc;
        currentRuin = null;
      }


    }
  }

  private void paintBoost() throws GameActionException {
    if (rc.canSenseLocation(locationGoing)) {
      int completedSpots = 0;
      for (MapInfo patternTile : rc.senseNearbyMapInfos(locationGoing, -1)) {
        if (patternTile.isWall() || patternTile.hasRuin()) {
          if (rc.canMark(locationGoing)) {
            rc.mark(locationGoing, false);
          }
          noGoBoosts.add(locationGoing);
          currentTask = SoldierTask.EXPLORING;
          locationGoing = Lib.noLoc;
          return;
        }
        if (patternTile.getPaint() != PaintType.EMPTY && patternTile.getPaint().isSecondary() == getPaintMarker(patternTile.getMapLocation())) {
          completedSpots++;
        }
      }

      if (completedSpots >= 25 && rc.getMoney() < 200) {
        currentTask = SoldierTask.WAITING_FOR_BOOST_FUNDS;
        // return;
      }

      if (rc.canCompleteResourcePattern(locationGoing)) {
        if (rc.canMark(locationGoing)) {
          rc.mark(locationGoing, false);
        }
        rc.setTimelineMarker("Boost built", 255, 255, 0);
        currentTask = SoldierTask.EXPLORING;
        locationGoing = Lib.noLoc;
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

  private void searchForBoost() throws GameActionException {
    MapInfo[] nearbyTiles = lib.nearbyTiles();

    for (MapInfo info : nearbyTiles) {
      if (info.getMapLocation().x > 2 && info.getMapLocation().y > 2 && info.getMapLocation().x < rc.getMapWidth() - 2 && info.getMapLocation().y < rc.getMapHeight() - 2) {
        if ((info.getMapLocation().x - 2) % 4 == 0 && (info.getMapLocation().y - 2) % 4 == 0 && !noGoBoosts.contains(info.getMapLocation())) {
          boolean validBoost = true;
          int numberFilled = 0;
          for (MapInfo tile : rc.senseNearbyMapInfos(info.getMapLocation(), 8)) {
            if (!tile.isPassable()) {
              validBoost = false;
              noGoBoosts.add(info.getMapLocation());
              break;
            }
            if (tile.getPaint() == PaintType.ENEMY_SECONDARY || tile.getPaint() == PaintType.ENEMY_PRIMARY) {
              validBoost = false;
              break;
            }
            if (tile.getPaint() != PaintType.EMPTY) {
              numberFilled++;
            }
          }
          if (info.getMark() == PaintType.ALLY_PRIMARY) {
            validBoost = false;
          }
          else if (numberFilled > 24) {
            validBoost = false;
            noGoBoosts.add(info.getMapLocation());
          }
          if (validBoost) {
            locationGoing = info.getMapLocation();
            currentTask = SoldierTask.PAINTING_BOOST;
            return;
          } else {
            // noGoBoosts.add(info.getMapLocation());
          }
        }
      }
    }
  }

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
          if (patternTile.getMark() != PaintType.EMPTY) {
            if ((patternTile.getPaint() == PaintType.ENEMY_PRIMARY || patternTile.getPaint() == PaintType.ENEMY_SECONDARY)) {
              totalFilled++;
            }
          }
        }
        if (totalFilled > 0 && rc.getRoundNum() > 10) {
          previousRuins.add(currentRuin.getMapLocation());
          previousRuinsRounds.add(20);
          currentTask = SoldierTask.EXPLORING;
          locationGoing = Lib.noLoc;
          currentRuin = null;
        }
      }
    }
  }

  private void checkToClearBoost() throws GameActionException {
    if (currentTask == SoldierTask.PAINTING_BOOST) {
      if (locationGoing == Lib.noLoc) {
        locationGoing = Lib.noLoc;
        currentTask = SoldierTask.EXPLORING;
        return;
      }
      if (noGoBoosts.contains(locationGoing)) {
        locationGoing = Lib.noLoc;
        currentTask = SoldierTask.EXPLORING;
        return;
      }
      if (rc.canSenseRobotAtLocation(locationGoing)) {
        if (rc.senseRobotAtLocation(locationGoing).getID() != rc.getID()) {

          locationGoing = Lib.noLoc;
          currentTask = SoldierTask.EXPLORING;
          return;
        }
      }
      if (rc.canSenseLocation(locationGoing)) {
        MapInfo info = rc.senseMapInfo(locationGoing);
        if (info.getMark() == PaintType.ALLY_PRIMARY) {
          locationGoing = Lib.noLoc;
          currentTask = SoldierTask.EXPLORING;
          return;
        }
      }
      if (rc.getLocation().distanceSquaredTo(locationGoing) < 5 && rc.getLocation() != locationGoing) {
        int totalFilled = 0;
        for (MapInfo patternTile : rc.senseNearbyMapInfos(locationGoing, 8)) {
          if (patternTile.getPaint() != PaintType.EMPTY) {
            totalFilled++;
          }
        }
        if (totalFilled > 24 && rc.getRoundNum() > 40) {
          if (rc.canMark(locationGoing)) {
            rc.mark(locationGoing, false);
          }
          currentTask = SoldierTask.EXPLORING;
          locationGoing = Lib.noLoc;
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

  private void goTowardsEmptySpots() throws GameActionException {
    if (currentTask == SoldierTask.EXPLORING && rc.getRoundNum() > 100) {
      MapInfo[] nearbyTiles = lib.nearbyTiles();
      int bestTileDistance = 999999;

      if (emptySpots.isEmpty()) {
        for (MapInfo tile : nearbyTiles) {
          if (tile.isPassable()) {
            if (tile.getPaint() == PaintType.EMPTY) { // todo, make this more random
              int distance = tile.getMapLocation().distanceSquaredTo(lib.center);
              if (bestTileDistance < distance) {
                bestTileDistance = distance;
                directionGoing = rc.getLocation().directionTo(tile.getMapLocation());
              }
              emptySpots.add(tile.getMapLocation());
            }
          }
        }
      }
      if (!emptySpots.isEmpty()) {
        if (rc.canSenseLocation(emptySpots.getFirst())) {
          MapInfo loc = rc.senseMapInfo(emptySpots.getFirst());
          directionGoing = rc.getLocation().directionTo(loc.getMapLocation());
          if (directionGoing == Direction.CENTER) {
            directionGoing = rc.getLocation().directionTo(lib.center);
          }
          if (loc.getPaint() != PaintType.EMPTY) {
            emptySpots.removeFirst();
          }
        }
        if (rc.getRoundNum() > 200) {
          addEmptySpots();
        }
      }
    }
  }

  private void addEmptySpots() {
    for (MapInfo tile : lib.nearbyTiles()) {
      if (tile.isPassable()) {
        if (tile.getPaint() == PaintType.EMPTY) { // todo, make this more random
          if (!emptySpots.contains(tile.getMapLocation())) {
            emptySpots.add(tile.getMapLocation());
          }
        }
      }
    }
  }


  private UnitType getBestTowerToMark() {
    if (rc.getRoundNum() < rc.getMapWidth()) {
      return UnitType.LEVEL_ONE_MONEY_TOWER;
    }
    else if (rc.getRoundNum() < 500) {
      return rc.getRoundNum() % 3.5 == 0 ?
          UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;
    }
    return (rng.nextInt(0, 2) == 0 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER);
  }

  private void resourcePatternPainting() throws GameActionException {
      for (MapInfo info : lib.nearbyTiles()) {
        if ((info.getMapLocation().x - 2) % 4 == 0 && (info.getMapLocation().y - 2) % 4 == 0) {
          if (rc.canCompleteResourcePattern(info.getMapLocation())) {
            Direction toLoc = rc.getLocation().directionTo(info.getMapLocation());
            directionGoing = toLoc != Direction.CENTER ? toLoc : directionGoing;
            rc.setTimelineMarker("Boost built", 255, 255, 0);
            rc.completeResourcePattern(info.getMapLocation());
          }
        }
      }
  }

  // 2 2 1 2 2
  // 2 1 1 1 2
  // 1 1 2 1 1
  // 2 1 1 1 2
  // 2 2 1 2 2
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

  // todo, if there are a lot of allies nearby, gang up and kill the tower!!!! (as in make locationGoing the tower)
  private void findEnemyTowersToAttack() throws GameActionException {
    for (RobotInfo robot : lib.getRobots(false)) {
      if (robot.getTeam() != rc.getTeam()) {
        if (lib.isTower(robot.getType())) {
            if (locationGoing == Lib.noLoc) {
              directionGoing = rc.getLocation().directionTo(robot.getLocation());
              enemyTowerGoing = robot.getLocation();
              currentTask = SoldierTask.ATTACKING_ENEMY_BASE;
              nav.avoidEnemyPaint = false;
            }
          }
          if (rc.canAttack(robot.getLocation())) {
            rc.attack(robot.getLocation());
          }
      }
    }
  }

  private void attackEnemyTowers() throws GameActionException {
    if (rc.canSenseLocation(enemyTowerGoing)) {
      RobotInfo enemyTower = rc.senseRobotAtLocation(enemyTowerGoing);
      if (enemyTower == null) {
        currentTask = SoldierTask.EXPLORING;
        enemyTowerGoing = Lib.noLoc;
      }
      else {
        directionGoing = rc.getLocation().directionTo(enemyTowerGoing);
        if (rc.getLocation().distanceSquaredTo(enemyTowerGoing) < 7) {
          directionGoing = directionGoing.opposite();
        }
        if (rc.canAttack(enemyTowerGoing)) {
          rc.attack(enemyTowerGoing);
        }
      }
    }
  }

}
