import java.io.*;

/**
 * Represents an observation from our detection device. When a location on the
 * sensor triggers, the time and the location of the detected event are recorded
 * in one of these Observation objects.
 */
public class Observation implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final long EOF = Long.MAX_VALUE;  // our convention to mark EOF with a special object

    public long time; // number of milliseconds since turning on the detector device
    public double x, y; // location of the detected event on the detection grid

    public Observation(long time, double x, double y) {
        this.time = time;
        this.x = x;
        this.y = y;
    }

    public Observation() {
        this.time = EOF;
        this.x = this.y = 0.0;
    }

    public boolean isEOF() {
        return time == EOF;
    }

    public String toString() {
        return "Observation(" + time + ", " + x + ", " + y + ")";
    }
}
