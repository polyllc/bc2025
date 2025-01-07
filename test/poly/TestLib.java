package poly;

import org.junit.*;

import battlecode.common.Direction;

public class TestLib {
    @Test
    public void testDirectionNumber() {
      Assert.assertEquals(1, Direction.WEST.getDirectionOrderNum());
    }
}
