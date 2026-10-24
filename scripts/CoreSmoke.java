import com.eteks.sweethome3d.io.HomeFileRecorder;
import com.eteks.sweethome3d.model.*;

public class CoreSmoke {
    public static void main(String[] args) throws Exception {
        Home home = new Home();
        Wall a = new Wall(0, 0, 400, 0, 10, 250);
        Wall b = new Wall(400, 0, 400, 300, 10, 250);
        a.setWallAtEnd(b);
        b.setWallAtStart(a);
        home.addWall(a);
        home.addWall(b);
        home.addRoom(new Room(new float[][] {{0, 0}, {400, 0}, {400, 300}, {0, 300}}));
        String file = args[0];
        HomeFileRecorder recorder = new HomeFileRecorder(0, false, null, false, true);
        recorder.writeHome(home, file);
        Home loaded = recorder.readHome(file);
        System.out.println("walls=" + loaded.getWalls().size() + " rooms=" + loaded.getRooms().size());
        Wall first = loaded.getWalls().iterator().next();
        System.out.println("joined=" + (first.getWallAtEnd() != null));
        for (float[] p : first.getPoints()) {
            System.out.println(p[0] + "," + p[1]);
        }
        System.out.println("area=" + loaded.getRooms().get(0).getArea());
    }
}
