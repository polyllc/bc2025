package poly;

import battlecode.common.*;

import java.util.List;


public class Lib {

  RobotController rc;

  int roundNum;
  int lastRoundNum;
  MapLocation spawnLocations[];

  final MapLocation center;


  static MapLocation noLoc = new MapLocation(256,256);

  public Lib(RobotController robot){
    rc = robot;
    roundNum = rc.getRoundNum();
    lastRoundNum = roundNum--;
    center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
  }
  //pretty much any useful function or variables go here
  static final Direction[] directions = {
          Direction.NORTH,
          Direction.NORTHEAST,
          Direction.EAST,
          Direction.SOUTHEAST,
          Direction.SOUTH,
          Direction.SOUTHWEST,
          Direction.WEST,
          Direction.NORTHWEST,
  };

  static final Direction[] directionsCenter = {
          Direction.CENTER,
          Direction.NORTH,
          Direction.NORTHEAST,
          Direction.EAST,
          Direction.SOUTHEAST,
          Direction.SOUTH,
          Direction.SOUTHWEST,
          Direction.WEST,
          Direction.NORTHWEST,
  };

  public Direction[] directionsToMiddle(MapLocation loc) {
    Direction dirToCenter = loc.directionTo(center);
    return startDirList(dirToCenter.getDirectionOrderNum(), 10);
  }

  public int getQuadrant(){
    int width = rc.getMapWidth();
    int height = rc.getMapHeight();

    if(width/2 > rc.getLocation().x) { //left section
      if(height/2 <= rc.getLocation().y) { //top section, quadrant 2
        return 2;
      }
      if(height/2 >= rc.getLocation().y) { //bottom section quadrant 3
        return 3;
      }
    }
    if(width/2 <= rc.getLocation().x) { //right section
      if(height/2 <= rc.getLocation().y) { //top section, quadrant 1
        return 1;
      }
      if(height/2 > rc.getLocation().y) { //bottom section quadrant 4
        return 4;
      }
    }
    return 1;
  }

  public MapLocation mapCenter(){
    return  new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
  }

  public int getQuadrant(MapLocation m){
    int width = rc.getMapWidth();
    int height = rc.getMapHeight();

    if(width/2 > m.x) { //left section
      if(height/2 <= m.y) { //top section, quadrant 2
        return 2;
      }
      if(height/2 >= m.y) { //bottom section quadrant 3
        return 3;
      }
    }
    if(width/2 <= m.x) { //right section
      if(height/2 <= m.y) { //top section, quadrant 1
        return 1;
      }
      if(height/2 > m.y) { //bottom section quadrant 4
        return 4;
      }
    }
    return 1;
  }

  public MapLocation getOrigin(int q){
    if(q == 1){
      return new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
    }
    if(q == 2){
      return new MapLocation(rc.getMapWidth()/2-1, rc.getMapHeight()/2);
    }
    if(q == 3){
      return new MapLocation(rc.getMapWidth()/2-1, rc.getMapHeight()/2-1);
    }
    if(q == 4){
      return new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2-1);
    }
    return new MapLocation(0,0);
  }

  RobotInfo[] currentRoundRobots =  new RobotInfo[0];

  public RobotInfo[] getRobots(boolean sort){
    roundNum = rc.getRoundNum();
    if(currentRoundRobots.length == 0 || lastRoundNum < roundNum){
      currentRoundRobots = sort ? this.sort(rc.senseNearbyRobots()) : rc.senseNearbyRobots();
      lastRoundNum = roundNum;
    }
    return currentRoundRobots;
  }

  public <T> boolean contains(T[] ts, T t) {
    for (T item : ts) {
      if (item.equals(t)) {
        return true;
      }
    }
    return false;
  }


  boolean detectCorner(Direction dirGoing) throws GameActionException {
    if(rc.getLocation().equals(new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() - 1)) ||
            rc.getLocation().equals(new MapLocation(0, rc.getMapHeight() - 1)) ||
            rc.getLocation().equals(new MapLocation(rc.getMapWidth() - 1, 0)) ||
            rc.getLocation().equals(new MapLocation(0,0))){
      return true;
    }

    if(dirGoing != Direction.CENTER) {
      int[] walls = new int[8];
      int i = 0;
      for (Direction dir : directions) {
        if (rc.canSenseLocation(rc.getLocation().add(dir))) {
          if (!rc.sensePassability(rc.getLocation().add(dir))) {
            walls[i] = 1;
          }
        }
        i++;
      }

      if (walls[0] == 1 && walls[1] == 1 && walls[2] == 1 && dirGoing == Direction.NORTHEAST) { //corner northeast
        return true;
      }
      if (walls[2] == 1 && walls[3] == 1 && walls[4] == 1 && dirGoing == Direction.SOUTHEAST) { //corner southeast
        return true;
      }
      if (walls[4] == 1 && walls[5] == 1 && walls[6] == 1 && dirGoing == Direction.SOUTHWEST) { //corner southwest
        return true;
      }
      if (walls[6] == 1 && walls[7] == 1 && walls[0] == 1 && dirGoing == Direction.NORTHWEST) { //corner northwest
        return true;
      }
    }

    return false;
  }

  boolean detectCorner(MapLocation loc, int radius){
    if(loc.distanceSquaredTo(new MapLocation(0 ,0)) <= radius){
      return true;
    } else if(loc.distanceSquaredTo(new MapLocation(rc.getMapWidth()-1, 0)) <= radius){
      return true;
    } else if(loc.distanceSquaredTo(new MapLocation(0, rc.getMapHeight()-1)) <= radius){
      return true;
    } else if(loc.distanceSquaredTo(new MapLocation(rc.getMapWidth()-1, rc.getMapHeight()-1)) <= radius) {
      return true;
    }
    return false;
  }


  public Direction[] startDirList(int index, int offset){
    Direction[] dirs = new Direction[8];
    index = (index + offset) % 8;
    for(Direction dir : directions){
      dirs[index] = dir;
      index++;
      if(index == 8){
        index = 0;
      }
    }
    return dirs;
  }

  public Direction[] reverse(Direction[] dirs){ //meant for the hq
    if(getQuadrant() == 1 || getQuadrant() == 4){
      Direction[] newDirs = new Direction[dirs.length];
      int j = dirs.length-1;
      for(int i = 0; i < dirs.length; i++){
        newDirs[i] = dirs[j];
        j--;
      }
      return newDirs;
    }
    return dirs;
  }

  public int dirToIndex(Direction dir){
    switch(dir){
      case NORTH: return 0;
      case NORTHEAST: return 1;
      case EAST: return 2;
      case SOUTHEAST: return 3;
      case SOUTH: return 4;
      case SOUTHWEST: return 5;
      case WEST: return 6;
      case NORTHWEST: return 7;
    }
    return 0;
  }
/*

    Direction educatedGuess(MapLocation hq){
        return rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
    }
*/


  //doesn't actually sort anything, but what it does instead is put up the lowest health robot on the top
  public RobotInfo[] sort(RobotInfo[] items){
    if(items.length > 0) {
      RobotInfo lowest = items[0];
      int lowestIndex = 0;
      int i = 0;
      for (RobotInfo r : items) {
        if (rc.getTeam() != r.getTeam()){
          if(lowest.getTeam() == rc.getTeam()){
            lowest = r;
            lowestIndex = i;
          }
          if(lowest.getHealth() > r.getHealth()){
            lowest = r;
            lowestIndex = i;
          }
        }
        i++;
      }

      if(items.length > 1) {
        RobotInfo temp = items[0];
        items[0] = lowest;
        items[lowestIndex] = temp;
      }
    }
    return items;
  }


  //ok this will be a bit complicated so ill explain:
  //we know that if we sense an enemy spawn point we can determine where the center is by just one spawn point by:
  // - using rc.getLocation().direction(loc) and getting the opposite direction and then adding it to that loc
  //      - even if it is off by a couple, that doesn't matter because it will never be off by more than 1
  // - then setting that new added loc to the new enemy center
  // - once we have one we can use our spawn point locations to determine what symmetry type we have
  // - with rotational, we can simply rotate around the map center (to which I already have the code for)
  // - with mirrored symmetry along the y-axis, that is also easy by just calculating the distance and adding it from that origin
  // - same thing with x-axis
  // - what is really cool is that the code is the exact same, except the quadrants will be different depending on the symmetry


  // todo, somehow update this to bc2025
  private List<MapLocation> allySpawnZones() {
    return List.of();
  }

  private boolean isEnemyCenter(MapLocation loc) {
    return false;
  }

  private void setEnemyCenter(MapLocation loc) {

  }

  public void preliminaryAutofillEnemySpawnPoints() throws GameActionException {
    for(MapLocation spawns : allySpawnZones()) {
      int q = getQuadrant(spawns);
      MapLocation origin = getOrigin(q);
      int xOffset = spawns.x - origin.x;
      int yOffset = spawns.y - origin.y;
      int oppositeQ = 0;
      if(q == 1){
        oppositeQ = 3;
      }
      if(q == 2){
        oppositeQ = 4;
      }
      if(q == 3){
        oppositeQ = 1;
      }
      if(q == 4){
        oppositeQ = 2;
      }
      MapLocation otherOrigin = getOrigin(oppositeQ);
      int realX = 0;
      int realY = 0;
      switch (oppositeQ){
        case 1: realX = otherOrigin.x + Math.abs(xOffset); realY = otherOrigin.y + Math.abs(yOffset); break;
        case 2: realX = otherOrigin.x - Math.abs(xOffset); realY = otherOrigin.y + Math.abs(yOffset); break;
        case 3: realX = otherOrigin.x - Math.abs(xOffset); realY = otherOrigin.y - Math.abs(yOffset); break;
        case 4: realX = otherOrigin.x + Math.abs(xOffset); realY = otherOrigin.y - Math.abs(yOffset); break;
      }
      MapLocation enemyLoc = new MapLocation(realX, realY);
      if (!isEnemyCenter(enemyLoc)) {
        setEnemyCenter(enemyLoc);
      }
    }
  }
  public void autofillEnemySpawnPoints(MapLocation loc) throws GameActionException {
    if(horizontalCalc(loc.add(rc.getLocation().directionTo(loc))) != noLoc){
      System.out.println("Using Horizontal Calculations: " + loc.add(rc.getLocation().directionTo(loc)));
      for(MapLocation spawns : allySpawnZones()) {
        MapLocation enemyLoc = new MapLocation(spawns.x, rc.getMapHeight()-spawns.y);
        if (!isEnemyCenter(enemyLoc.add(rc.getLocation().directionTo(loc)))) {
          setEnemyCenter(enemyLoc.add(rc.getLocation().directionTo(loc)));
        }
      }
    }

    if(verticalCalc(loc.add(rc.getLocation().directionTo(loc))) != noLoc){
      System.out.println("Using Vertical   Calculations");
      for(MapLocation spawns : allySpawnZones()) {
        MapLocation enemyLoc = new MapLocation(rc.getMapHeight()-spawns.x, spawns.y);
        if (!isEnemyCenter(enemyLoc.add(rc.getLocation().directionTo(loc)))) {
          setEnemyCenter(enemyLoc.add(rc.getLocation().directionTo(loc)));
        }
      }
    }

    if(rotationalCalc(loc) != noLoc){
      System.out.println("Using Rotational Calculations");
      for(MapLocation spawns : allySpawnZones()) {
        int q = getQuadrant(spawns);
        MapLocation origin = getOrigin(q);
        int xOffset = spawns.x - origin.x;
        int yOffset = spawns.y - origin.y;
        int oppositeQ = 0;
        if(q == 1){
          oppositeQ = 3;
        }
        if(q == 2){
          oppositeQ = 4;
        }
        if(q == 3){
          oppositeQ = 1;
        }
        if(q == 4){
          oppositeQ = 2;
        }
        MapLocation otherOrigin = getOrigin(oppositeQ);
        int realX = 0;
        int realY = 0;
        switch (oppositeQ){
          case 1: realX = otherOrigin.x + Math.abs(xOffset); realY = otherOrigin.y + Math.abs(yOffset); break;
          case 2: realX = otherOrigin.x - Math.abs(xOffset); realY = otherOrigin.y + Math.abs(yOffset); break;
          case 3: realX = otherOrigin.x - Math.abs(xOffset); realY = otherOrigin.y - Math.abs(yOffset); break;
          case 4: realX = otherOrigin.x + Math.abs(xOffset); realY = otherOrigin.y - Math.abs(yOffset); break;
        }
        MapLocation enemyLoc = new MapLocation(realX, realY);
        if (!isEnemyCenter(enemyLoc)) {
          setEnemyCenter(enemyLoc);
        }
      }
    }
  }

  MapLocation horizontalCalc(MapLocation loc) throws GameActionException {
    for(MapLocation allySpawn : allySpawnZones()){
      MapLocation currentGuess = new MapLocation(allySpawn.x, rc.getMapHeight()-allySpawn.y);
      if(loc.distanceSquaredTo(currentGuess) < 3){
        return currentGuess;
      }
    }
    return noLoc;
  }

  MapLocation verticalCalc(MapLocation loc) throws GameActionException {
    for(MapLocation allySpawn : allySpawnZones()){
      MapLocation currentGuess = new MapLocation(rc.getMapHeight()-allySpawn.x, allySpawn.y);
      if(loc.distanceSquaredTo(currentGuess) < 3){
        return currentGuess;
      }
    }
    return noLoc;
  }

  MapLocation rotationalCalc(MapLocation loc) throws GameActionException {
    for(MapLocation allySpawn : allySpawnZones()){
      int q = getQuadrant(allySpawn);
      MapLocation origin = getOrigin(q);
      int xOffset = allySpawn.x - origin.x;
      int yOffset = allySpawn.y - origin.y;
      int oppositeQ = 0;
      if(q == 1){
        oppositeQ = 3;
      }
      if(q == 2){
        oppositeQ = 4;
      }
      if(q == 3){
        oppositeQ = 1;
      }
      if(q == 4){
        oppositeQ = 2;
      }
      MapLocation otherOrigin = getOrigin(oppositeQ);
      int realX = 0;
      int realY = 0;
      switch (oppositeQ){
        case 1: realX = otherOrigin.x + Math.abs(xOffset); realY = otherOrigin.y + Math.abs(yOffset); break;
        case 2: realX = otherOrigin.x - Math.abs(xOffset); realY = otherOrigin.y + Math.abs(yOffset); break;
        case 3: realX = otherOrigin.x - Math.abs(xOffset); realY = otherOrigin.y - Math.abs(yOffset); break;
        case 4: realX = otherOrigin.x + Math.abs(xOffset); realY = otherOrigin.y - Math.abs(yOffset); break;
      }
      if(new MapLocation(realX, realY).distanceSquaredTo(loc) < 3){
        return new MapLocation(realX, realY);
      }
    }
    return noLoc;
  }






  //returns all enemy ducks that are in the sight of this duck
  public RobotInfo[] enemiesInRadius() throws GameActionException{
    if(rc.getTeam() == Team.A){
      return rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, Team.B);
    }
    return rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, Team.A);
  }





  //Using the rc, returns a 0,1,2,3 if they are up, left, right, and down respectively depending on what border they are on
  // If they are on multiple, returns the first one in the above order. If on none, returns -1
  public int borderDetection() throws GameActionException {
    if(!rc.canMove(Direction.NORTH) && rc.canSenseLocation(rc.getLocation().add(Direction.NORTH))){
      return 0;
    }
    else if(!rc.canMove(Direction.WEST) && rc.canSenseLocation(rc.getLocation().add(Direction.WEST))){
      return 1;
    }
    else if(!rc.canMove(Direction.EAST) && rc.canSenseLocation(rc.getLocation().add(Direction.EAST))){
      return 2;
    }
    else if(!rc.canMove(Direction.SOUTH) && rc.canSenseLocation(rc.getLocation().add(Direction.SOUTH))){
      return 3;
    }
    else {
      return -1;
    }
  }

  /**
   * checks if it is a tower
   * @param unit unittype
   * @return bool
   */
  public boolean isTower(UnitType unit) {
    return !(unit == UnitType.SOLDIER || unit == UnitType.MOPPER || unit == UnitType.SPLASHER);
  }


  public boolean isEnemyPaint(MapLocation loc) throws GameActionException {
    return (rc.canSenseLocation(loc)
            && rc.senseMapInfo(loc).getPaint() == PaintType.ENEMY_PRIMARY
            && rc.senseMapInfo(loc).getPaint() == PaintType.ENEMY_SECONDARY);
  }


}