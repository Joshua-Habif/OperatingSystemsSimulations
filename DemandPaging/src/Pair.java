

public class Pair implements Comparable<Pair>{
		
	private int x;
	private int y;
	private int lastUsed;
	private int loadTime;
	
	public Pair(int x, int y) {
		this.x=x;
		this.y=y;
		this.lastUsed=-1;
		this.loadTime=-1;
	}
	
	public Pair() {
		this.x=-1;
	}
	
	public void setLastUsed(int n) {
		this.lastUsed = n;
	}
	
	public void setLoadTime(int n) {
		this.loadTime = n;
	}
	
	public void setX(int n) {
		this.x = n;
	}
	
	public void setY(int n) {
		this.y = n;
	}
	
	public int getLoadTime() {
		return loadTime;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getLastUsed() {
		return lastUsed;
	}
	
	public String toString() {
		return "("+this.x+","+this.y+"," + this.lastUsed+")";
	}

	@Override
	public boolean equals(Object o) {
		Pair pair = (Pair)o;
		if(this.x == pair.x && this.y == pair.y)
			return true;
		return false;
	}

	@Override
	public int compareTo(Pair o) {
		if(this.getLastUsed()>o.getLastUsed())
			return 1;
		else if(this.getLastUsed()<o.getLastUsed()){
			return -1;
		}
		return 0;
	}
	
	
}