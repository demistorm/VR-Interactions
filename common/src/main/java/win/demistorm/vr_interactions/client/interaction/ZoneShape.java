package win.demistorm.vr_interactions.client.interaction;

import org.joml.Vector3dc;

// World-space geometry of an InteractZone
public interface ZoneShape {

    boolean contains(Vector3dc pos);

    // Center for debug rendering and distance sorting
    Vector3dc center();

    record Sphere(Vector3dc center, double radius) implements ZoneShape {

        @Override
        public boolean contains(Vector3dc pos) {
            return pos.distanceSquared(center) <= radius * radius;
        }
    }
}
