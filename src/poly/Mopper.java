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
    if (currentTask == MopperTask.EXPLORING) {
      setCurrentTask();
    }

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


    for (MapLocation ruinInfo : rc.senseNearbyRuins(-1)) {
      for (MapInfo loc : rc.senseNearbyMapInfos(ruinInfo, 8)) {
        if (loc.getPaint() == PaintType.ENEMY_PRIMARY || loc.getPaint() == PaintType.ENEMY_SECONDARY) {
          locationGoing = ruinInfo;
          currentTask = MopperTask.MOPPING_RUIN;
          return;
        }
      }
    }

    RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    if (robotInfos.length > 0) {
      locationGoing = robotInfos[0].getLocation();
      currentTask = MopperTask.MOPPING_ENEMIES;
      return;
    }

    for (MapInfo loc : rc.senseNearbyMapInfos()) {
      if (loc.getPaint() == PaintType.ENEMY_PRIMARY || loc.getPaint() == PaintType.ENEMY_SECONDARY) {
        locationGoing = loc.getMapLocation();
        currentTask = MopperTask.MOPPING;
        return;
      }
    }




      currentTask = MopperTask.EXPLORING;
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
    Direction bestDir = numEnemiesInDir();
    rc.mopSwing(bestDir);
    /*
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

     */
  }


  // cleans up enemy tiles around ruins
  private void cleanUpRuin() throws GameActionException {

    for (MapLocation ruinInfo : rc.senseNearbyRuins(-1)) {
      for (MapInfo loc : rc.senseNearbyMapInfos(ruinInfo)) {
        if (loc.getPaint() == PaintType.ENEMY_PRIMARY || loc.getPaint() == PaintType.ENEMY_SECONDARY) {
          if (rc.canSenseRobotAtLocation(locationGoing) && !rc.senseRobotAtLocation(locationGoing).getTeam().equals(rc.getTeam())) {
            if (rc.canMopSwing(rc.getLocation().directionTo(locationGoing))) {
              rc.mopSwing(rc.getLocation().directionTo(locationGoing));
            }
          }
            locationGoing = loc.getMapLocation();
            if (rc.canAttack(locationGoing)) {
              rc.attack(locationGoing);
            }
            return;

        }

      }
    }
    currentTask = MopperTask.EXPLORING;

    /*
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
        rc.attack(rc.getLocation().add(bestDirToSweep));
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
        rc.attack(rc.getLocation().add(dirToClean));
      }
    }

     */
  }

  // counts how many enemy tiles are around and returns direction that has the most
  private Direction numEnemyTiles() throws GameActionException {

    int[] enemyTilesInDirection = {0, 0, 0, 0};
    // n e s w

    for (Direction d : Lib.directions) {
      if (rc.senseMapInfo(rc.getLocation().add(d)).getPaint() == PaintType.ENEMY_PRIMARY ||
          rc.senseMapInfo(rc.getLocation().add(d)).getPaint() == PaintType.ENEMY_SECONDARY) {
        switch (d) {
          case NORTHEAST:
          case NORTH:
          case NORTHWEST:
            enemyTilesInDirection[0]++;
            break;
          case SOUTH:
          case SOUTHWEST:
          case SOUTHEAST:
            enemyTilesInDirection[2]++;
            break;
          case EAST:
            enemyTilesInDirection[1]++;
            break;
          case WEST:
            enemyTilesInDirection[3]++;
            break;
          default:
            break;
        }
      }
    }

    int dir = 0;
    int maxEnemies = 0;
    for (int i = 0; i < enemyTilesInDirection.length; i++) {
      if (enemyTilesInDirection[i] > maxEnemies) {
        dir = i;
        maxEnemies = enemyTilesInDirection[i];
      }
    }

    return switch (dir) {
      case 0 -> Direction.NORTH;
      case 1 -> Direction.EAST;
      case 2 -> Direction.SOUTH;
      default -> Direction.WEST;
    };

  }

  // counts how many enemy bots there are and returns direction with most
  private Direction numEnemiesInDir() throws GameActionException {

    int[] enemyInDirection = {0, 0, 0, 0};
    // n e s w

    for (Direction d : Lib.directions) {
      if (rc.senseMapInfo(rc.getLocation().add(d)).getPaint() == PaintType.ENEMY_PRIMARY ||
          rc.senseMapInfo(rc.getLocation().add(d)).getPaint() == PaintType.ENEMY_SECONDARY) {
        switch (d) {
          case NORTHEAST:
          case NORTH:
          case NORTHWEST:
            enemyInDirection[0]++;
            break;
          case SOUTH:
          case SOUTHWEST:
          case SOUTHEAST:
            enemyInDirection[2]++;
            break;
          case EAST:
            enemyInDirection[1]++;
            break;
          case WEST:
            enemyInDirection[3]++;
            break;
          default:
            break;
        }
      }
    }

    int dir = 0;
    int maxEnemies = 0;
    for (int i = 0; i < enemyInDirection.length; i++) {
      if (enemyInDirection[i] > maxEnemies) {
        dir = i;
        maxEnemies = enemyInDirection[i];
      }
    }

    return switch (dir) {
      case 0 -> Direction.NORTH;
      case 1 -> Direction.EAST;
      case 2 -> Direction.SOUTH;
      default -> Direction.WEST;
    };
  }
}