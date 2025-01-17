package poly;

import battlecode.common.*;
import battlecode.schema.RobotType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mopper extends MovableUnit {

  public Mopper(RobotController rc) throws GameActionException {
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
    nav.avoidEnemyPaint = true;
  }

  public enum MopperTask {
    EXPLORING,
    MOPPING,
    MOPPING_RUIN,
    MOPPING_ENEMIES,
    FOLLOW,
    TRANSFER
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
    else if (currentTask == MopperTask.FOLLOW) {
      follow();
    }
    else if (currentTask == MopperTask.TRANSFER) {
      transfer();
    }

    move();

  }

  @Override
  protected void move() throws GameActionException {
    if (currentTask == MopperTask.EXPLORING) {
      if (rc.getNumberTowers() < 3) {
        currentTask = MopperTask.FOLLOW;

      }
      else {
        nav.avoidEnemyPaint = true;
        explore();
        // TODO: something with edge finding to use the averageEnemyTower direction
      }
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

    if (rc.getPaint() > 51) {
      currentTask = MopperTask.TRANSFER;
      return;
    }

    currentTask = MopperTask.EXPLORING;
  }

  // transfer paint to ally or tower
  // tries to keep paint above half
  private void transfer() throws GameActionException {
    int amountPaintAboveHalf = rc.getPaint() - 50;
    // can't transfer paint if it goes below 50
    if (amountPaintAboveHalf < 0) {
      return;
    }
    for (RobotInfo bot: rc.senseNearbyRobots()) {
      if (rc.canTransferPaint(bot.getLocation(), amountPaintAboveHalf)) {
        if (bot.getType() == UnitType.SOLDIER) {
          //locationGoing = bot.location;
          transferToAlly(amountPaintAboveHalf);
          locationGoing = Lib.noLoc;
          //currentTask = MopperTask.EXPLORING;
          return;
        }
        if (bot.getType() == UnitType.LEVEL_ONE_MONEY_TOWER || bot.type == UnitType.LEVEL_TWO_MONEY_TOWER
            || bot.getType() == UnitType.LEVEL_THREE_MONEY_TOWER) {
          rc.transferPaint(bot.getLocation(), amountPaintAboveHalf);
          locationGoing = Lib.noLoc;
          return;
        }


        currentTask = MopperTask.EXPLORING;
        return;
      }
      locationGoing = bot.getLocation();
      return;
    }
    currentTask = MopperTask.EXPLORING;

  }

  // transfers a certain percentage of paint to a soldier
  private void transferToAlly(int amount) throws GameActionException {
    int lowestPaint = Integer.MAX_VALUE;
    RobotInfo lowestBot = null;
    for (RobotInfo botInfo: rc.senseNearbyRobots(-1, rc.getTeam())) {
      if (botInfo.getPaintAmount() < lowestPaint) {
        lowestBot = botInfo;
        lowestPaint = botInfo.getPaintAmount();
      }
    }
    if (lowestBot != null) {
      if (rc.canTransferPaint(lowestBot.getLocation(), amount)) {
        rc.transferPaint(lowestBot.getLocation(), amount);
        currentTask = MopperTask.EXPLORING;
      }
    }

  }

  // the moppers will follow soliders
  // for the beginning rounds so they don't kill themselves
  // also tranfers some paint to soliders when they can
  private void follow() throws GameActionException {
    for (RobotInfo botInfo: rc.senseNearbyRobots(-1, rc.getTeam())) {
      if (botInfo.getType() == UnitType.SOLDIER) {
        directionGoing = rc.getLocation().directionTo(botInfo.getLocation());
        return;
      }
      else {
        nav.minPaintLoss(rc.getLocation());
        return;
      }
    }
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
    if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length == 0) {
      currentTask = MopperTask.EXPLORING;
    }

  }


  // cleans up enemy tiles around ruins
  private void cleanUpRuin() throws GameActionException {

    int paintCounter = 0;
    for (MapLocation ruinInfo : rc.senseNearbyRuins(-1)) {
      for (MapInfo loc : rc.senseNearbyMapInfos(ruinInfo)) {
        if (loc.getPaint() == PaintType.ENEMY_PRIMARY || loc.getPaint() == PaintType.ENEMY_SECONDARY) {
          paintCounter++;

          locationGoing = loc.getMapLocation();

          if (rc.canAttack(locationGoing)) {
            rc.attack(locationGoing);
            paintCounter--;
          }

        }

      }
    }
    if (paintCounter == 0) {
      currentTask = MopperTask.EXPLORING;
    }


  }

  /*
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

   */

  // counts how many enemy bots there are and returns direction with most
  private Direction numEnemiesInDir() throws GameActionException {

    int[] enemyInDirection = {0, 0, 0, 0};
    // n e s w

    for (Direction d : Lib.directions) {
      if (rc.canSenseLocation(rc.getLocation().add(d))) {
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