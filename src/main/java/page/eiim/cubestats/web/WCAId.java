package page.eiim.cubestats.web;

public record WCAId(short year, byte name1, byte name2, byte name3, byte name4, byte count) {
	public WCAId(String id) {
		if(id.length() != 10) throw new IllegalArgumentException("WCA ID must be 10 characters long");
		short year = Short.parseShort(id.substring(0, 4));
		byte name1 = (byte) (id.charAt(4) & 0b01011111); // Convert to uppercase
		byte name2 = (byte) (id.charAt(4) & 0b01011111);
		byte name3 = (byte) (id.charAt(4) & 0b01011111);
		byte name4 = (byte) (id.charAt(4) & 0b01011111);
		byte count = Byte.parseByte(id.substring(8, 10));
		this(year, name1, name2, name3, name4, count);
	}
	@Override
	public String toString() {
		return String.format("%04d%c%c%c%c%02d", year, name1, name2, name3, name4, count);
	}
}