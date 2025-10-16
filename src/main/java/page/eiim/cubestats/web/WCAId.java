package page.eiim.cubestats.web;

public record WCAId(short year, byte name1, byte name2, byte name3, byte name4, byte count) implements Comparable<WCAId> {
	public WCAId(String id) {
		if(id.length() != 10) throw new IllegalArgumentException("WCA ID must be 10 characters long");
		short year = Short.parseShort(id.substring(0, 4));
		byte name1 = (byte) (id.charAt(4) & 0b01011111); // Convert to uppercase
		byte name2 = (byte) (id.charAt(5) & 0b01011111);
		byte name3 = (byte) (id.charAt(6) & 0b01011111);
		byte name4 = (byte) (id.charAt(7) & 0b01011111);
		byte count = Byte.parseByte(id.substring(8, 10));
		this(year, name1, name2, name3, name4, count);
	}
	@Override
	public String toString() {
		return String.format("%04d%c%c%c%c%02d", year, name1, name2, name3, name4, count);
	}
	@Override
	public int compareTo(WCAId o) {
		int cmp = this.year - o.year;
		if(cmp != 0) return cmp;
		cmp = this.name1 - o.name1;
		if(cmp != 0) return cmp;
		cmp = this.name2 - o.name2;
		if(cmp != 0) return cmp;
		cmp = this.name3 - o.name3;
		if(cmp != 0) return cmp;
		cmp = this.name4 - o.name4;
		if(cmp != 0) return cmp;
		return this.count - o.count;
	}
	
	public static WCAId minPrefix(String partialId) {
		if(partialId.length() < 4) {
			partialId = partialId + "0".repeat(4 - partialId.length());
		}
		if(partialId.length() < 8) {
			partialId = partialId + "A".repeat(8 - partialId.length());
		}
		if(partialId.length() < 10) {
			partialId = partialId + "0".repeat(10 - partialId.length());
		}
		return new WCAId(partialId);
	}
	
	public static boolean isValid(String id) {
		if(id.length() != 10) return false;
		char c0 = id.charAt(0);
		if(c0 < '0' || c0 > '9') return false;
		char c1 = id.charAt(1);
		if(c1 < '0' || c1 > '9') return false;
		char c2 = id.charAt(2);
		if(c2 < '0' || c2 > '9') return false;
		char c3 = id.charAt(3);
		if(c3 < '0' || c3 > '9') return false;
		char c4 = id.charAt(4);
		if((c4 < 'A' || c4 > 'Z') && (c4 < 'a' || c4 > 'z')) return false;
		char c5 = id.charAt(5);
		if((c5 < 'A' || c5 > 'Z') && (c5 < 'a' || c5 > 'z')) return false;
		char c6 = id.charAt(6);
		if((c6 < 'A' || c6 > 'Z') && (c6 < 'a' || c6 > 'z')) return false;
		char c7 = id.charAt(7);
		if((c7 < 'A' || c7 > 'Z') && (c7 < 'a' || c7 > 'z')) return false;
		char c8 = id.charAt(8);
		if(c8 < '0' || c8 > '9') return false;
		char c9 = id.charAt(9);
		if(c9 < '0' || c9 > '9') return false;
		return true;
	}
}