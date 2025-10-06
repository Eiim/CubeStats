package page.eiim.cubestats.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WordStorage {
	
	private HashMap<Integer, List<Entry>> wordStorage;
	private StringTree stringTree;
	
	public WordStorage(List<String> entries) {
		wordStorage = new HashMap<>();
		stringTree = new StringTree();
		for(var entryText : entries) {
			if(entryText.length() >= 4) {
				Entry entry = new Entry(entryText);
				stringTree.add(entry);
				for(String word : entry.words) {
					int wordInt = getWordInt(word);
					wordStorage.putIfAbsent(wordInt, new ArrayList<>());
					List<Entry> entryList = wordStorage.get(wordInt);
					entryList.add(entry);
				}
			} else {
				System.out.println("Ignoring entry (too short): " + entryText);
			}
		}
	}
	
	public List<String> query(String query, int maxResults) {
		if(query.length() < 4) return List.of();
		query = query.toLowerCase(Locale.ROOT);
		List<String> results = new ArrayList<>();
		
		// Search string tree for entries starting with the query
		stringTree.get(query, results);
		if(results.size() > 0) {
			List<String> resultStrings = new ArrayList<>(results.size()); // Assume few perfect matches
			List<String> perfectMatches = new ArrayList<>();
			for(String entry : results) {
				if(entry.equals(query)) {
					perfectMatches.add(entry);
				} else {
					resultStrings.add(entry);
				}
			}
			if(perfectMatches.size() >= maxResults) {
				return perfectMatches.subList(0, maxResults);
			} else {
				int remaining = maxResults - perfectMatches.size();
				if(resultStrings.size() > remaining) {
					resultStrings = resultStrings.subList(0, remaining);
					perfectMatches.addAll(resultStrings);
					return perfectMatches;
				} else {
					perfectMatches.addAll(resultStrings);
					results = perfectMatches;
				}
			}
		}
		// Search word storage for entries containing all words in the query
		getEntriesWithMatchingWords(query, results);
		if(results.size() > maxResults) {
			return results.subList(0, maxResults);
		} else {
			return results;
		}
	}
	
	public static int getWordInt(String word) {
		int result = 0;
		for(int i = 0; i < 26; i++) {
			if(word.indexOf((char)('a' + i)) != -1) {
				result |= (1 << i);
			}
		}
		return result;
	}
	
	private void getEntriesWithMatchingWords(String query, List<String> results) {
		String[] queryWords = query.split(" ");
		int[] invQueryWordShorts = new int[queryWords.length];
		for(int i = 0; i < queryWords.length; i++) {
			invQueryWordShorts[i] = ~getWordInt(queryWords[i]);
		}
		
		Set<Entry> candidates = new HashSet<>();
		
		for(int entryShort : wordStorage.keySet()) {
			for(int queryShort : invQueryWordShorts) {
				if((entryShort | queryShort) == 0xFFFFFFFF) {
					candidates.addAll(wordStorage.get(entryShort));
					break;
				}
			}
		}
		
		for(Entry entry : candidates) {
			boolean allMatch = true;
			for(String queryWord : queryWords) {
				if(entry.fullText.indexOf(queryWord) == -1) {
					allMatch = false;
					break;
				}
			}
			if(allMatch) {
				if(!results.contains(entry.fullText)) results.add(entry.fullText);
			}
		}
	}
	
	private class Entry {
		public String[] words;
		public String fullText;
		
		public Entry(String fullText) {
			this.fullText = fullText.toLowerCase(Locale.ROOT);
			words = this.fullText.split(" ");
		}
	}
	
	private class StringTree {
		private Map<Character, Map<Character, Map<Character, Map<Character, List<Entry>>>>> tree;
		
		public StringTree() {
			tree = new HashMap<>();
		}
		
		public void add(Entry entry) {
			String baseWord = entry.words[0];
			if(baseWord.length() < 4) baseWord = String.join(" ", entry.words);
			char c1 = baseWord.charAt(0);
			char c2 = baseWord.charAt(1);
			char c3 = baseWord.charAt(2);
			char c4 = baseWord.charAt(3);
			tree.putIfAbsent(c1, new HashMap<>());
			var level2 = tree.get(c1);
			level2.putIfAbsent(c2, new HashMap<>());
			var level3 = level2.get(c2);
			level3.putIfAbsent(c3, new HashMap<>());
			var level4 = level3.get(c3);
			level4.putIfAbsent(c4, new ArrayList<>());
			level4.get(c4).add(entry);
		}
		
		public void get(String query, List<String> results) {
			char c1 = query.charAt(0);
			char c2 = query.charAt(1);
			char c3 = query.charAt(2);
			char c4 = query.charAt(3);
			var level2 = tree.get(c1);
			if(level2 == null) return;
			var level3 = level2.get(c2);
			if(level3 == null) return;
			var level4 = level3.get(c3);
			if(level4 == null) return;
			var entries = level4.get(c4);
			if(entries == null) return;
			for(var entry : entries) {
				if(entry.fullText.startsWith(query)) {
					results.add(entry.fullText);
				}
			}
		}
	}

}
