package poly;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Mopper extends MovableUnit {

  public Mopper(RobotController rc) {
    super(rc);
  }

  public enum MopperTask {
    EXPLORING,
    MOPPING_ENEMY_TILES,
    MOPPING_ENEMIES,
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
  private void mopEnemies() throws GameActionException {
    int mostEnemiesInDir = 0;
    Direction bestDirToSweep = null;

    for (Direction dir: Direction.allDirections()) {
      if (rc.canMopSwing(dir)) {
        int enemyInDir = 0;
        // need a list of directions (tile-wise) to check for enemies
        switch (dir) {
          case NORTH:
            // if the robot in one of the north sweep directions is an enemy
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTHWEST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTH)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTHEAST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            break;
          case EAST:
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTHEAST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.EAST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTHEAST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            break;
          case SOUTH:
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTHWEST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTH)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTHEAST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            break;
          case WEST:
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTHWEST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.WEST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
            if (!rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTHWEST)).getTeam().
                equals(rc.getTeam())) {
              enemyInDir++;
            }
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

  private void mopEnemyTiles() throws GameActionException {


  }

  private void cleanUpRuin() {

  }
}
