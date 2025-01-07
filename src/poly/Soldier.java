package poly;

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
    PAINTING
  }

  static final Random rng = new Random(6147);

  MapInfo currentRuin = null;

  MapLocation taskLocation = Lib.noLoc;

  SoldierTask currentTask = SoldierTask.EXPLORING;

  MapLocation spawnedTower = Lib.noLoc;


  public Soldier(RobotController rc) {
    super(rc);
    for (RobotInfo robot : rc.senseNearbyRobots()) {
      if (robot.getTeam() == rc.getTeam()) {
        if (lib.isTower(robot.getType())) {
          spawnedTower = robot.getLocation();
        }
      }
    }
    directionGoing = rc.getLocation().directionTo(lib.center);
    // todo, the tower that spawn this robot might have an objective
    //  which may be in a message sent on over
  }


  @Override
  public void takeTurn() throws GameActionException {

    if (currentRuin == null) {
      searchForRuin();
    }

    move();
    paint();

  }

  private void paint() throws GameActionException {
    if (currentTask == SoldierTask.PAINTING_RUIN) {
      MapLocation targetLoc = currentRuin.getMapLocation();
      Direction dir = rc.getLocation().directionTo(targetLoc);
      if (rc.canMove(dir))
        rc.move(dir);
      // Mark the pattern we need to draw to build a tower here if we haven't already.
      MapLocation shouldBeMarked = currentRuin.getMapLocation().subtract(dir);
      if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
        rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
        System.out.println("Trying to build a tower at " + targetLoc);
      }
      // Fill in any spots in the pattern with the appropriate paint.
      for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)){
        if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
          boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
          if (rc.canAttack(patternTile.getMapLocation()))
            rc.attack(patternTile.getMapLocation(), useSecondaryColor);
        }
      }
      // Complete the ruin if we can.
      if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
        rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
        rc.setTimelineMarker("Tower built", 0, 255, 0);
        System.out.println("Built a tower at " + targetLoc + "!");
        currentTask = SoldierTask.EXPLORING;
        currentRuin = null;
      }
    }
    else {
      // Try to paint beneath us as we walk to avoid paint penalties.
      // Avoiding wasting paint by re-painting our own tiles.
      MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
      if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())){
        rc.attack(rc.getLocation());
      }
    }
  }

  private void searchForRuin() {
    MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
    for (MapInfo tile : nearbyTiles){
      if (tile.hasRuin()){
        currentRuin = tile;
        currentTask = SoldierTask.PAINTING_RUIN;
        return;
      }
    }
  }
}
