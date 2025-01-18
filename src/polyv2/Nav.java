package polyv2;

import battlecode.common.*;

import java.util.ArrayList;

public class Nav {
  RobotController rc;
  // bug nav variables
  boolean useBugNav = true;
  boolean turnRight = false;
  int minDistanceToTarget = Integer.MAX_VALUE;
  Direction lastDirMoved = null;
  MapLocation bugNavTarget = null;

  boolean avoidEnemyPaint = false;

  boolean avoidEnemyTowers = false;

  boolean avoidNonAllyPaint = false;

  private Lib lib;



  public Nav(RobotController robot) throws GameActionException {
    rc = robot;
    lib = new Lib(robot);
  }
  //this is the navigation class that we create an object of for each robot to keep our nav code nice and neat

  public boolean tryMove(Direction dir) throws GameActionException {
    if(rc.canMove(dir) && !enemyPaint(dir) && !enemyTower(dir) && !nonAllyPaint(dir)){
      rc.move(dir);
      return true;
    }
    return false;
  }

  //pretty much everything down here is modified battlegaode's code from bc2020, thank you! (the license says i can use it hehe)
  boolean tryMove() throws GameActionException {
    for (Direction dir : Lib.directions)
      if (tryMove(dir))
        return true;
    return false;
  }

  //use this when wanting to go a certain direction, useful for randomly moving about
  boolean goTo(Direction dir) throws GameActionException {
    Direction [] toTry;
    if (Math.random() < 0.5) {
      toTry = new Direction[]{dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
    }
    else {
      toTry = new Direction[]{dir, dir.rotateRight(), dir.rotateLeft(), dir.rotateRight().rotateRight(),dir.rotateLeft().rotateLeft()};
    }
    for (Direction d : toTry){
      if(tryMove(d))
        return true;
    }
    return false;
  }

  // tries to move in the general direction of dir with preference to the right (if preferenceRight is true)
  boolean goTo(Direction dir, boolean preferenceLeft) throws GameActionException {
    Direction [] toTry;
    if (preferenceLeft) {
      Direction [] temp = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
      toTry = temp;
    }
    else {
      Direction[] temp = {dir, dir.rotateRight(), dir.rotateLeft(), dir.rotateRight().rotateRight(),dir.rotateLeft().rotateLeft()};
      toTry = temp;
    }
    for (Direction d : toTry){
      if(tryMove(d))
        return true;
    }
    return false;
  }

  boolean goTo(MapLocation destination) throws GameActionException {
    return goTo(destination, false);
  }

  // navigate towards a particular location
  // we use this function for moving to a specific location
  boolean goTo(MapLocation destination, boolean avoidBugNav) throws GameActionException {
    if (!(rc.isMovementReady()) || destination == Lib.noLoc) return false;
    if (useBugNav && !avoidBugNav) {
      return bugNavTo(destination);
    }
    else if (Clock.getBytecodesLeft() > 0) { // if we have time, do more intelligent navigation
      boolean success = navTo(destination);
      if (!success && !avoidBugNav) {
        // System.out.println("Intelligent navigation failed; switching to bug nav");
        useBugNav = true;
        MapLocation currentLoc = rc.getLocation();
        minDistanceToTarget = Math.min(minDistanceToTarget, currentLoc.distanceSquaredTo(destination));
        // only change direction if bugNavTarget changes
        if (bugNavTarget == null || !bugNavTarget.equals(destination)) {
          minDistanceToTarget = currentLoc.distanceSquaredTo(destination);
        }
        MapLocation mapCenter = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        lastDirMoved = currentLoc.directionTo(destination);
        double angleToCenter = Math.atan2(mapCenter.y - currentLoc.y, mapCenter.x - currentLoc.x);
        double angleToTarget = Math.atan2(destination.y - currentLoc.y, destination.x - currentLoc.x);
        double difference = (angleToTarget - angleToCenter + 2 * Math.PI) % (2 * Math.PI);
        turnRight = (difference > Math.PI);
        if (turnRight)
          lastDirMoved = lastDirMoved.rotateLeft().rotateLeft();
        else
          lastDirMoved = lastDirMoved.rotateRight().rotateRight();
        // if (Math.abs(difference) < Math.PI / 10) turnRight = !turnRight;
        bugNavTarget = destination;
        //  System.out.println(minDistanceToTarget);

        // we want bug nav toward the center
        // this means that we should turn right if, when facing the center, the destination is on the right
      }
      if (avoidBugNav) {
        // System.out.println("avoidBugNav: " + success);
        return success;
      }
      else {
        return success || bugNavTo(destination);
      }
    }
    else {
      //   System.out.println("Trying to go to " + destination);
      MapLocation myLoc = rc.getLocation();
      double x = destination.x - myLoc.x;
      double y = destination.y - myLoc.y;
      double actualAngle = Math.atan2(y, x);
      Direction dir = myLoc.directionTo(destination);
      double dirAngle = Math.atan2(dir.dy, dir.dx);
      double difference = (actualAngle - dirAngle + 2 * Math.PI) % (2 * Math.PI);
      //   System.out.println(difference);
      if (difference < Math.PI)
        return goTo(dir, true);
      else
        return goTo(dir, false);
    }
  }

  private boolean enemyPaint(Direction dir) throws GameActionException {
    return (avoidEnemyPaint && rc.canSenseLocation(rc.getLocation().add(dir)))
            && (rc.senseMapInfo(rc.getLocation().add(dir)).getPaint() == PaintType.ENEMY_PRIMARY
            || rc.senseMapInfo(rc.getLocation().add(dir)).getPaint() == PaintType.ENEMY_SECONDARY);
  }

  private boolean enemyPaint(MapLocation loc) throws GameActionException {
    return (avoidEnemyPaint && rc.canSenseLocation(loc))
            && (rc.senseMapInfo(loc).getPaint() == PaintType.ENEMY_PRIMARY
            || rc.senseMapInfo(loc).getPaint() == PaintType.ENEMY_SECONDARY);
  }

  private boolean nonAllyPaint(Direction dir) throws GameActionException {
    return (avoidNonAllyPaint && rc.canSenseLocation(rc.getLocation().add(dir)) &&
            !rc.senseMapInfo(rc.getLocation().add(dir)).getPaint().isAlly());
  }

  private boolean enemyTower(Direction dir) throws GameActionException {
    if (avoidEnemyTowers) {
      for (RobotInfo robot : rc.senseNearbyRobots()) {
        if (robot.getTeam() == rc.getTeam().opponent()) {
          if (lib.isTower(robot.getType())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  boolean bugNavTo(MapLocation destination) throws GameActionException {
    // if target has changed, reset bug nav
    if (bugNavTarget == null || !(destination.equals(bugNavTarget))) {
      useBugNav = false;
      return false;
    }
    // if there are no walls (ie robot can move in all directions), don't use bug nav
    // this is to avoid problems where we get stuck and then keep going in circles
    boolean noWalls = true;
    MapLocation currentLoc = rc.getLocation();
    for (Direction dir : Lib.directions)
      if (!(rc.canMove(dir)) || enemyPaint(dir) || enemyTower(dir)) //|| rc.senseFlooding(currentLoc.add(dir))) maybe we change this to whatever those cloud things do
        noWalls = false;
    if (noWalls) {
      useBugNav = false;
      return false;
    }
    Direction newDir;
    int counter = 0; // make sure we don't try to move more than 8 times
    //System.out.println(lastDirMoved);
    if (turnRight) {
      //System.out.println("Turning right");
      newDir = lastDirMoved.rotateRight().rotateRight();
      //System.out.println(newDir);
      // now rotate left until there is a valid legal move
      while (!tryMove(newDir)) {
        //System.out.println(newDir);
        newDir = newDir.rotateLeft();
        if (counter ++ >= 8)
          return false;
      }
      lastDirMoved = newDir;
    } else {
      newDir = lastDirMoved.rotateLeft().rotateLeft();
      //System.out.println(newDir);
      // now rotate left until there is a valid legal move
      while (!tryMove(newDir)) {
        //System.out.println(newDir);
        newDir = newDir.rotateRight();
        if (counter ++ >= 8)
          return false;
      }
      lastDirMoved = newDir;
    }
    // halt bug nav if closer than ever before
    if (rc.getLocation().distanceSquaredTo(destination) < minDistanceToTarget)
      useBugNav = false;
    return true;
  }

  // more intelligent navigation (for non-drones)
  // radius is half-edge of square to search in
  boolean navTo(MapLocation destination, int radius, int iterations) throws GameActionException {
    int initialBytecodeCount = Clock.getBytecodeNum();
    int n = 2 * radius + 1;
    //System.out.println("Intelligent navigation to " + destination);
    MapLocation myLoc = rc.getLocation();
    boolean [][] validLocs = new boolean [n][n];
    int [][] elevations = new int [n][n];
    // initialize this array
    for (int x = 0; x < n; x++) {
      for (int y = 0; y < n; y ++) {
        MapLocation loc = new MapLocation(myLoc.x + x - radius, myLoc.y + y - radius);
        // check if location is valid movement place (within sensor radius, on map, unoccupied, and not flooded)
        boolean valid = false;
        if(!rc.canSenseLocation(loc)){

        }
        else if(rc.isLocationOccupied(loc)){

        }
        else {
          MapInfo mapInfo = rc.senseMapInfo(loc);
          valid = mapInfo.isPassable() && !enemyPaint(loc);
        }
        // boolean valid = rc.canSenseLocation(loc) && !rc.isLocationOccupied(loc) && rc.senseMapInfo(loc).getCurrentDirection() == Direction.CENTER;
        validLocs[x][y] = valid;
        if (valid) elevations[x][y] = 1; //technically it all has the same elevation so if i put it to 1 it'll work?
      }
    }
    // own location is also valid
    validLocs[radius][radius] = true;
    elevations[radius][radius] = 1;

    //System.out.println("Bytecodes used in checking location validity: " + (Clock.getBytecodeNum() - initialBytecodeCount));


    // figure out which edges are valid
    // This is a 4 by (n-1) by (n-1) array where the first entry loops over the edges {\,-,/,|}
    // Note: This omits some edges around the top right of the grid but oh well (for now)
    boolean [][] validEdges0 = new boolean [n-1][n-1];
    boolean [][] validEdges1 = new boolean [n-1][n-1];
    boolean [][] validEdges2 = new boolean [n-1][n-1];
    boolean [][] validEdges3 = new boolean [n-1][n-1];
    for (int x = 0; x < n-1; x ++) {
      for (int y = 0; y < n-1; y ++) {
        validEdges0[x][y] = (validLocs[x][y+1] && validLocs[x+1][y] && Math.abs(elevations[x][y+1] - elevations[x+1][y]) <= 3);
        validEdges1[x][y] = (validLocs[x][y] && validLocs[x+1][y] && Math.abs(elevations[x][y] - elevations[x+1][y]) <= 3);
        validEdges2[x][y] = (validLocs[x][y] && validLocs[x+1][y+1] && Math.abs(elevations[x][y] - elevations[x+1][y+1]) <= 3);
        validEdges3[x][y] = (validLocs[x][y] && validLocs[x][y+1] && Math.abs(elevations[x][y] - elevations[x][y+1]) <= 3);
      }
    }
    //System.out.println("Bytecodes used in setup: " + (Clock.getBytecodeNum() - initialBytecodeCount));

    // initialize grid with (large number) * distance to enemy HQ
    int [][] distances = new int[n][n];
    for (int x = 0; x < n; x ++)
      for (int y = 0; y < n; y ++)
        distances[x][y] = (int)(Math.sqrt(Math.pow(myLoc.x+x-radius-destination.x,2) + Math.pow(myLoc.y+y-radius-destination.y,2)) * 100 + 0.5);

    //System.out.println("Bytecodes used in setup and distance initialization: " + (Clock.getBytecodeNum() - initialBytecodeCount));


    // now iterate
    for (int counter = 0; counter < iterations; counter++) {
      for (int x = 0; x < n-1; x ++) {
        for (int y = 0; y < n-1; y ++) {
          //System.out.println((Clock.getBytecodeNum() - initialBytecodeCount));
          if (validEdges0[x][y]){
            //System.out.println((Clock.getBytecodeNum() - initialBytecodeCount));
            distances[x][y+1] = Math.min(distances[x][y+1], distances[x+1][y] + 1);
            //System.out.println((Clock.getBytecodeNum() - initialBytecodeCount));
            distances[x+1][y] = Math.min(distances[x][y+1] + 1, distances[x+1][y]);
            //System.out.println((Clock.getBytecodeNum() - initialBytecodeCount));
          }
          //System.out.println((Clock.getBytecodeNum() - initialBytecodeCount));
          if (validEdges1[x][y]){
            distances[x][y] = Math.min(distances[x][y], distances[x+1][y] + 1);
            distances[x+1][y] = Math.min(distances[x][y] + 1, distances[x+1][y]);
          }
          if (validEdges2[x][y]){
            distances[x][y] = Math.min(distances[x][y], distances[x+1][y+1] + 1);
            distances[x+1][y+1] = Math.min(distances[x][y] + 1, distances[x+1][y+1]);
          }
          if (validEdges3[x][y]){
            distances[x][y] = Math.min(distances[x][y], distances[x][y+1] + 1);
            distances[x][y+1] = Math.min(distances[x][y] + 1, distances[x][y+1]);
          }
        }
      }
      // System.out.println((Clock.getBytecodeNum() - initialBytecodeCount));
      //System.out.println((Clock.getBytecodeNum() - initialBytecodeCount));
    }

    //System.out.println("Bytecodes used in setup and iteration: " + (Clock.getBytecodeNum() - initialBytecodeCount));


    // now find minimum value among neighbors adjacent to self (with a valid edge)
    int bestX = radius;
    int bestY = radius;
    int minDistance = distances[radius][radius];
    // Unfortunately, due to how the edges are stored, we have to try the adjacent tiles manually
    if (validEdges0[radius-1][radius] && distances[radius-1][radius+1] < minDistance) {
      bestX = radius-1;
      bestY = radius+1;
      minDistance = distances[radius-1][radius+1];
      //System.out.println(bestX + " " + bestY + " " + minDistance);
    }
    if (validEdges0[radius][radius-1] && distances[radius+1][radius-1] < minDistance) {
      bestX = radius+1;
      bestY = radius-1;
      minDistance = distances[radius+1][radius-1];
      //System.out.println(bestX + " " + bestY + " " + minDistance);
    }

    if (validEdges1[radius-1][radius] && distances[radius-1][radius] < minDistance) {
      bestX = radius-1;
      bestY = radius;
      minDistance = distances[radius-1][radius];
      //System.out.println(bestX + " " + bestY + " " + minDistance);
    }
    if (validEdges1[radius][radius] && distances[radius+1][radius] < minDistance) {
      bestX = radius+1;
      bestY = radius;
      minDistance = distances[radius+1][radius];
      //System.out.println(bestX + " " + bestY + " " + minDistance);
    }

    if (validEdges2[radius-1][radius-1] && distances[radius-1][radius-1] < minDistance) {
      bestX = radius-1;
      bestY = radius-1;
      minDistance = distances[radius-1][radius-1];
      //System.out.println(bestX + " " + bestY + " " + minDistance);
    }
    if (validEdges2[radius][radius] && distances[radius+1][radius+1] < minDistance) {
      bestX = radius+1;
      bestY = radius+1;
      minDistance = distances[radius+1][radius+1];
      //System.out.println(bestX + " " + bestY + " " + minDistance);
    }

    if (validEdges3[radius][radius-1] && distances[radius][radius-1] < minDistance) {
      bestX = radius;
      bestY = radius-1;
      minDistance = distances[radius][radius-1];
      //System.out.println(bestX + " " + bestY + " " + minDistance);
    }
    if (validEdges3[radius][radius] && distances[radius][radius+1] < minDistance) {
      bestX = radius;
      bestY = radius+1;
      minDistance = distances[radius][radius+1];
      //System.out.println(bestX + " " + bestY + " " + minDistance);
    }

    if (minDistance == distances[radius][radius])
      return false;

    //System.out.println("Best location found (relative to current position): (" + (bestX - radius) + "," + (bestY - radius) + ")");
    Direction bestDir = myLoc.directionTo(new MapLocation(myLoc.x + (bestX - radius), myLoc.y + (bestY - radius)));
    //System.out.println("Total bytecodes used: " + (Clock.getBytecodeNum() - initialBytecodeCount));

    boolean successful = tryMove(bestDir);
    //System.out.println(successful);
    return successful;
  }

  // default parameters
  boolean navTo(MapLocation destination) throws GameActionException {
    return navTo(destination, 1, 1);
  }

  // finds the best move that minimizes paint loss
  public MapLocation minPaintLoss(MapLocation currentPos) throws GameActionException {
    int paintLoss = -1;
    MapLocation bestLoc = new MapLocation(currentPos.x, currentPos.y);

    for (Direction dir: Direction.allDirections()) {
      int thisPaintLoss = 0;
      MapLocation newLoc = new MapLocation(currentPos.x, currentPos.y).add(dir);
      if (rc.canSenseLocation(newLoc)) {
        MapInfo newLocInfo = rc.senseMapInfo(newLoc);

        if (newLocInfo.getPaint().equals(PaintType.EMPTY)) {
          thisPaintLoss++;
        }
        else if (!newLocInfo.getPaint().isAlly()) {
          thisPaintLoss += 2;
        }

        // get number of adjacent allies on enemy territory * 2
        int extraPenalty = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam()).length * 2;
        thisPaintLoss += extraPenalty;


        // checks if this is the least amount of paint lost
        if (paintLoss == -1 || thisPaintLoss < paintLoss) {
          paintLoss = thisPaintLoss;
          bestLoc = newLoc;
        }
      }

    }

    return bestLoc;
  }




  //from kryptonite




}