package br.com.square.scene;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o IncrementalSceneTracker desacoplado de dependencias nativas.
 */
public class IncrementalSceneTrackerTest {

    private static class MockTransformTarget implements IncrementalSceneTracker.SceneTransformTarget {
        private double tx, ty, tz, angleY;

        @Override
        public void setTransform(double tx, double ty, double tz, double angleY) {
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.angleY = angleY;
        }

        public double getTx() { return tx; }
        public double getTy() { return ty; }
        public double getTz() { return tz; }
        public double getAngleY() { return angleY; }
    }

    @Test
    public void testSceneTrackerIncrementalUpdate() {
        IncrementalSceneTracker tracker = new IncrementalSceneTracker();
        assertEquals(0, tracker.getTrackedCount());

        String itemMock = "Movel_1";
        MockTransformTarget target = new MockTransformTarget();

        tracker.trackNode(itemMock, target);
        assertEquals(1, tracker.getTrackedCount());
        assertFalse(tracker.isDirty(itemMock));

        // Aplica transformacao incremental
        boolean updated = tracker.updateTransform(itemMock, 150.0f, 200.0f, 40.0f, 1.57f);
        assertTrue("Transformacao incremental deve ter sucesso", updated);
        assertFalse("Item deve estar com estado limpo apos atualizacao", tracker.isDirty(itemMock));

        assertEquals(150.0, target.getTx(), 0.001);
        assertEquals(40.0, target.getTy(), 0.001);
        assertEquals(200.0, target.getTz(), 0.001);
        assertEquals(1.57, target.getAngleY(), 0.001);

        // Marca dirty manualmente
        tracker.markDirty(itemMock, IncrementalSceneTracker.DirtyType.GEOMETRY);
        assertTrue(tracker.isDirty(itemMock));

        // Untrack
        tracker.untrackNode(itemMock);
        assertEquals(0, tracker.getTrackedCount());
    }
}
