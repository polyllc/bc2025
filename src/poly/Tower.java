package poly;

import battlecode.common.GameActionException;

public abstract class Tower implements Unit {
  public abstract void takeTurn() throws GameActionException;
}
