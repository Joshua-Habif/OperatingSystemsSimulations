import java.util.Comparator;

public class FIFOComparator implements Comparator<Pair>{
	
	@Override
	public int compare(Pair o1, Pair o2) {
		return (-1*o1.compareTo(o2));
	}
}
