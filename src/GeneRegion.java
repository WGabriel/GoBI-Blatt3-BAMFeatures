import java.util.Comparator;

public class GeneRegion implements Comparable<GeneRegion> {
    public String id;
    public int start;
    public int end;

    // Constructor
    public GeneRegion(String id, int start, int end) {
        super();
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public GeneRegion(GeneRegion another) {
        this.id = another.id;
        this.start = another.start;
        this.end = another.end;
    }

//	@Override
//	public int compare(GeneRegion gr1, GeneRegion gr2) {
//		// Sorts GeneRegions ascending to their start-points
//		if (gr1.start > gr2.start) {
//			return 1;
//		} else if (gr1.start < gr2.start) {
//			return -1;
//		} else {
//			return 0;
//		}
//	}

    @Override
    public int compareTo(GeneRegion gr2) {
        // Sorts GeneRegions ascending to their start-points
        if (this.start > gr2.start) {
            return 1;
        } else if (this.start < gr2.start) {
            return -1;
        } else {
            return 0;
        }
    }

}
