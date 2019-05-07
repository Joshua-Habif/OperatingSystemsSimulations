import java.util.Comparator;

public class LRUComparator implements Comparator<Pair>{

	@Override
	public int compare(Pair o1, Pair o2) {
		return o1.compareTo(o2);
	}

}
