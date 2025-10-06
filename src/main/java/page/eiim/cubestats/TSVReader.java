package page.eiim.cubestats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

public class TSVReader {
	
	int columns = 0;
	BufferedReader reader;
	
	public TSVReader(File file, Charset charset) {
		try {
			reader = new BufferedReader(new FileReader(file, charset));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean hasNext() {
		try {
			return reader.ready(); // I think this should work?
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public String[] readLine() throws IOException {
		String line = reader.readLine();
		if (line == null || line.isEmpty()) {
			return new String[0];
		}
		String[] parts = line.split("\t", -1);
		if(columns == 0) {
			columns = parts.length;
		} else if(parts.length != columns) {
			throw new IOException("Expected " + columns + " columns, but got " + parts.length + " in line: " + line);
		}
		String[] cols = new String[columns];
		for(int i = 0; i < columns; i++) {
			if(parts[i].isEmpty() || "NULL".equals(parts[i])) {
				cols[i] = null; // Handle NULL values
			} else {
				if(parts[i].contains("\\")) {
					// Handle escaped characters
					// These are rare, so it doesn't matter if the code is slow
					// Seemingly, only tab and backslash are escaped. Everything else is handled by markdown parsing.
					StringBuilder sb = new StringBuilder();
					int partLen = parts[i].length();
					for(int j = 0; j < partLen; j++) {
						char c = parts[i].charAt(j);
						if(c == '\\') {
							if(j + 1 < partLen) {
								j++; // Skip the escape character
								c = parts[i].charAt(j);
								switch(c) {
									case 't':
										sb.append('\t');
										break;
									case '\\':
										sb.append('\\');
										break;
									default:
										sb.append('\\').append(c); // Unknown, keep the escape sequence as is
								}
							} else {
								sb.append('\\'); // If it's the last character, just append it
							}
						} else {
							sb.append(c);
						}
					}
					cols[i] = sb.toString();
				} else {
					cols[i] = parts[i]; // Normal value
				}
			}
		}
		return cols;
	}
}
