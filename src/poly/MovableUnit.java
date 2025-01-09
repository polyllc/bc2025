package poly;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class MovableUnit extends Unit {

  protected boolean stopMoving = false;
  protected Direction directionGoing = Direction.CENTER;
  protected MapLocation locationGoing = Lib.noLoc;
  protected Nav nav;
  protected boolean lastMovement = false;
  protected int turnsMovingInDirection = 0;
  protected MapLocation lastLocationGoing = Lib.noLoc;

  protected final Random rng = new Random(6147);

  public MovableUnit(RobotController rc) {
    super(rc);
    nav = new Nav(rc);
  }

  protected void move() throws GameActionException {
    explore();
    if (locationGoing == Lib.noLoc) {
      if (directionGoing != Direction.CENTER) {
        nav.goTo(rc.getLocation().add(directionGoing));
        turnsMovingInDirection++;
      }
    } else {

      boolean goToResult = nav.goTo(locationGoing, false);
      lastMovement = goToResult; //if we need to save bytecode, well this is where we're saving it
      lastLocationGoing = locationGoing;
      if (!lastMovement) {
        lastMovement = goToResult;
        if (!lastMovement) {
            lastMovement = nav.navTo(locationGoing);
          if (!lastMovement) {
            lastMovement = nav.goTo(rc.getLocation().directionTo(locationGoing));
          }
        }
      }
    }
  }

  protected void explore() throws GameActionException {
    if (lib.detectCorner(directionGoing) || lib.detectCorner(rc.getLocation(), 5)) {
      // directionGoing = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
      if (rng.nextBoolean()) {
        directionGoing = directionGoing.rotateLeft().rotateLeft();
      } else {
        directionGoing = directionGoing.rotateRight().rotateRight();
      }
    }
    if (turnsMovingInDirection > (rc.getMapHeight() + rc.getMapWidth()) / 2.5) {
      turnsMovingInDirection = 0;
      if (rng.nextBoolean()) {
        directionGoing = directionGoing.rotateLeft().rotateLeft();
      } else {
        directionGoing = directionGoing.rotateRight().rotateRight();
      }
    }

  }

}
