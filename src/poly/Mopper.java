package poly;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mopper extends MovableUnit {

  public Mopper(RobotController rc) {
    super(rc);
  }

  public enum MopperTask {
    EXPLORING,
    MOPPING,
    MOPPING_RUIN
  }

  MopperTask currentTask = MopperTask.EXPLORING;

  @Override
  public void takeTurn() throws GameActionException {
    paint();
    move();
  }

  private void paint() {

  }

  // mops nearby enemies in the best direction with most enemies
  // if there are ties, chooses the first direction
  private void mop() throws GameActionException {
    int mostEnemiesInDir = 0;
    Direction bestDirToSweep = null;

    for (Direction dir : Direction.allDirections()) {
      if (rc.canMopSwing(dir)) {
        int enemyInDir = 0;
        // need a list of directions (tile-wise) to check for enemies
        switch (dir) {
          case NORTH:
            // if the robot in one of the north sweep directions is an enemy
            enemyInDir = numEnemiesInDir(Direction.NORTH);
            break;
          case EAST:
            enemyInDir = numEnemiesInDir(Direction.EAST);
            break;
          case SOUTH:
            enemyInDir = numEnemiesInDir(Direction.SOUTH);
            break;
          case WEST:
            enemyInDir = numEnemiesInDir(Direction.WEST);
            break;
          default:
            break;
        }

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
      for (Direction dir : Direction.allDirections()) {
        int numEnemyTiles = 0;
        switch (dir) {
          case NORTH:
            numEnemyTiles = numEnemyTilesInDir(Direction.NORTH);
            break;
          case EAST:
            numEnemyTiles = numEnemyTilesInDir(Direction.EAST);
            break;
          case SOUTH:
            numEnemyTiles = numEnemyTilesInDir(Direction.SOUTH);
            break;
          case WEST:
            numEnemyTiles = numEnemyTilesInDir(Direction.WEST);
            break;
          default:
            break;
        }

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
      for (MapLocation ruin : rc.senseNearbyRuins(rc.getType().actionRadiusSquared)) {
        int thisNumEnemyTiles = 0;
        Direction dirToRuin = rc.getLocation().directionTo(ruin);

        // look at each ruin and count how many enemy paint tiles are there
        // sweep in direction of the ruin that has the most enemy tiles around
        thisNumEnemyTiles = numEnemyTilesInDir(dirToRuin);
        if (thisNumEnemyTiles > numEnemyTiles) {
          dirToClean = dirToRuin;
          numEnemyTiles = thisNumEnemyTiles;
        }

      }

      if (dirToClean != null) {
        rc.mopSwing(dirToClean);
      }
    }
  }

  // counts how many enemy tiles are around in the given direction
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
    dirMap.put(Direction.NORTHEAST, Arrays.asList(Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST));
    dirMap.put(Direction.NORTHWEST, Arrays.asList(Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST));
    dirMap.put(Direction.SOUTHEAST, Arrays.asList(Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST));
    dirMap.put(Direction.SOUTHWEST, Arrays.asList(Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST));

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