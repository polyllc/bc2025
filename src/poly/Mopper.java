package poly;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mopper extends MovableUnit {

  public Mopper(RobotController rc) {
    super(rc);
    currentTask = MopperTask.EXPLORING;
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

  public enum MopperTask {
    EXPLORING,
    MOPPING,
    MOPPING_RUIN,
    MOPPING_ENEMIES
  }

  MopperTask currentTask = MopperTask.EXPLORING;

  @Override
  public void takeTurn() throws GameActionException {

    rc.setIndicatorString("lG: " + locationGoing.toString()
        + " | cT: " + currentTask
        + " | dG: " + directionGoing);
    setCurrentTask();

    if (currentTask == MopperTask.MOPPING) {
      mopTile();
    }
    else if (currentTask == MopperTask.MOPPING_RUIN) {
      cleanUpRuin();
    }
    else if (currentTask == MopperTask.MOPPING_ENEMIES) {
      mopEnemies();
    }

    move();

  }

  @Override
  protected void move() throws GameActionException {
    if (currentTask == MopperTask.EXPLORING) {
      explore();
    }
    super.move();
  }

  private void setCurrentTask() throws GameActionException {
    if (rc.senseNearbyRuins(-1).length > 0) {
      //currentTask = MopperTask.MOPPING_RUIN;
      //cleanUpRuin();
    }
    // if there are more than x amount of enemy robots, mop them
    // note: number of robots nearby is subject to change
    else if (rc.senseNearbyRobots(20, rc.getTeam().opponent()).length > 4) {
      //currentTask = MopperTask.MOPPING_ENEMIES;
      //mopEnemies();
    }
    //else {
      for (MapInfo loc : rc.senseNearbyMapInfos()) {
        if (loc.getPaint() == PaintType.ENEMY_PRIMARY || loc.getPaint() == PaintType.ENEMY_SECONDARY) {
          locationGoing = loc.getMapLocation();
          currentTask = MopperTask.MOPPING;
        }
      }
    //}


  }

  // mops an enemy tile
  private void mopTile() throws GameActionException {
    // mops first available tile
    if (rc.getLocation().distanceSquaredTo(locationGoing) < 3) {
      if (rc.canAttack(locationGoing)) {
        rc.attack(locationGoing);
        locationGoing = Lib.noLoc;
        currentTask = MopperTask.EXPLORING;
      }
    }
  }

  // mops nearby enemies in the best direction with most enemies
  // if there are ties, chooses the first direction
  private void mopEnemies() throws GameActionException {
    int mostEnemiesInDir = 0;
    Direction bestDirToSweep = null;

    for (Direction dir : Direction.cardinalDirections()) {
      if (rc.canMopSwing(dir)) {
        int enemyInDir = 0;
        enemyInDir = numEnemiesInDir(dir);

        if (enemyInDir > mostEnemiesInDir) {
          mostEnemiesInDir = enemyInDir;
          bestDirToSweep = dir;
        }

      }
    }
    // if there is a best direction, sweep
    // if not, do nothing
    if (bestDirToSweep != null) {
      rc.mopSwing(bestDirToSweep);
    }
  }


  // cleans up enemy tiles around ruins
  // assumes that rc is near a ruin or is moving towards one
  private void cleanUpRuin() throws GameActionException {

    // if rc is on a ruin, sweep in direction that has the most enemy tiles
    if (rc.senseMapInfo(rc.getLocation()).hasRuin()) {
      int mostEnemyTiles = 0;
      Direction bestDirToSweep = null;

      for (Direction dir : Direction.cardinalDirections()) {
        int numEnemyTiles = 0;
        numEnemyTiles = numEnemyTilesInDir(dir);

        if (numEnemyTiles > mostEnemyTiles) {
          mostEnemyTiles = numEnemyTiles;
          bestDirToSweep = dir;
        }
      }
      if (bestDirToSweep != null) {
        rc.mopSwing(bestDirToSweep);
      }
    } else {
      // if rc is in range of a ruin, sweep direction closest to one
      Direction dirToClean = null;
      int numEnemyTiles = 0;
      for (MapLocation ruin : rc.senseNearbyRuins(-1)) {
        int thisNumEnemyTiles = 0;
        Direction dirToRuin = rc.getLocation().directionTo(ruin);

        // look at each ruin and count how many enemy paint tiles are there
        // sweep in direction of the ruin that has the most enemy tiles around
        thisNumEnemyTiles = numEnemyTilesInDir(dirToRuin);

        boolean cardinal = false;
        // checks to see if it's a cardinal direction
        for (Direction dir: Direction.cardinalDirections()) {
          if (dirToRuin.equals(dir)) {
            cardinal = true;
            break;
          }
        }
        if (!cardinal) {
          if (dirToRuin.equals(Direction.NORTHEAST) || dirToRuin.equals(Direction.SOUTHEAST)) {
            dirToRuin = Direction.EAST;
          }
          else {
            dirToRuin = Direction.WEST;
          }
        }

        if (thisNumEnemyTiles > numEnemyTiles) {
          dirToClean = dirToRuin;
          numEnemyTiles = thisNumEnemyTiles;
        }

      }

      if (dirToClean != null) {
        //System.out.println("dirToClean: " + dirToClean);
        rc.mopSwing(dirToClean);
      }
    }
  }

  // counts how many enemy tiles are around in the given cardinal direction
  private int numEnemyTilesInDir(Direction dir) throws GameActionException {
    Map<Direction, List<Direction>> dirMap = new HashMap<Direction, List<Direction>>();
    dirMap.put(Direction.NORTH, Arrays.asList(Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST));
    dirMap.put(Direction.EAST, Arrays.asList(Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST));
    dirMap.put(Direction.SOUTH, Arrays.asList(Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST));
    dirMap.put(Direction.WEST, Arrays.asList(Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST));
    dirMap.put(Direction.NORTHEAST, Arrays.asList(Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST));
    dirMap.put(Direction.NORTHWEST, Arrays.asList(Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST));
    dirMap.put(Direction.SOUTHEAST, Arrays.asList(Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST));
    dirMap.put(Direction.SOUTHWEST, Arrays.asList(Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST));



    int numEnemyTiles = 0;
    for (Direction dirToCheck: dirMap.get(dir)) {
      if (!rc.senseMapInfo(rc.getLocation().add(dir)).getPaint().isAlly()) {
        numEnemyTiles++;
      }
    }
    return numEnemyTiles;
  }

  // counts how many enemy bots there are in a given direction
  private int numEnemiesInDir(Direction dir) throws GameActionException {
    Map<Direction, List<Direction>> dirMap = new HashMap<Direction, List<Direction>>();
    dirMap.put(Direction.NORTH, Arrays.asList(Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST));
    dirMap.put(Direction.EAST, Arrays.asList(Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST));
    dirMap.put(Direction.SOUTH, Arrays.asList(Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST));
    dirMap.put(Direction.WEST, Arrays.asList(Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST));



    int numEnemies = 0;
    for (Direction dirToCheck: dirMap.get(dir)) {
      if (!rc.senseRobotAtLocation(rc.getLocation().add(dir)).getTeam().
          equals(rc.getTeam())) {
        numEnemies++;
      }
    }
    return numEnemies;
  }
}