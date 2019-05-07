import java.util.Comparator;

public class FIFOComparator implements Comparator<Pair>{
	
	@Override
	public int compare(Pair o1, Pair o2) {
		if(o1.compareTo(o2)==1)
			return -1;
		if(o1.compareTo(o2)==-1)
			return 1;
		return 0;
	}
}
