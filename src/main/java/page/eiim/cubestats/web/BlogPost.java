package page.eiim.cubestats.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class BlogPost {
	
	final String title;
	final LocalDate date;
	final LocalDate updated;
	final String path;
	final String abstractText;
	final String bodyHTML;

	public BlogPost(File file) throws FileNotFoundException {
		Scanner s = new Scanner(file);
		title = s.nextLine();
		date = LocalDate.parse(s.nextLine(), DateTimeFormatter.ISO_LOCAL_DATE);
		String updatedLine = s.nextLine();
		if(updatedLine.isBlank()) {
			updated = null;
		} else {
			updated = LocalDate.parse(updatedLine, DateTimeFormatter.ISO_LOCAL_DATE);
		}
		path = s.nextLine();
		abstractText = s.nextLine();
		ArrayList<String> bodyLines = new ArrayList<>();
		while(s.hasNextLine()) {
			bodyLines.add(s.nextLine());
		}
		s.close();
		bodyHTML = markdownToHTML(bodyLines);
	}
	
	/*
	 * Somewhat custom markdown format:
	 * Each line is a paragraph, header, list item, block equation, or image
	 * Headers will start with #, ##, or ### for h1, h2, and h3 respectively
	 * List items will start with * followed by a space
	 * Block equations will be enclosed in $$...$$
	 * Images will be in the format ![caption](url)
	 * Everything else is a paragraph
	 * Inline equations will be enclosed in $...$
	 * Links will be in the format [text](url)
	 * Bold text will be enclosed in **...**
	 * Italic text will be enclosed in _..._
	 * Code will be enclosed in `...`
	 * Where special characters are needed, they will be escaped with a backslash (\)
	 */
	private String markdownToHTML(ArrayList<String> lines) {
		StringBuilder html = new StringBuilder("<article class=\"blogPost\">\n");
		
		html.append("<h1>").append(title).append("</h1>\n");
		String dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
		html.append("<h3><date class=\"date\" datetime=\"").append(dateString).append("\">").append(date).append("</date></h3>\n");
		if(updated != null) {
			html.append("<h3 style=\"margin-top:-1em\"><span class=\"blog-italics\">Updated <span class=\"date\">")
				.append(updated.format(DateTimeFormatter.ISO_LOCAL_DATE))
				.append("</span></span></h3>\n");
		}
		
		boolean inList = false;
		for(String line : lines) {
			if(!line.startsWith("* ") && inList) {
				html.append("</ul>\n");
				inList = false;
			}
			if(line.startsWith("#")) {
				// Header
				int headerLevel = 0;
				while(headerLevel < line.length() && line.charAt(headerLevel) == '#') {
					headerLevel++;
				}
				if(headerLevel > 6) headerLevel = 6;
				String headerText = unescape(line.substring(headerLevel).trim());
				html.append("<h").append(headerLevel).append(">").append(headerText).append("</h").append(headerLevel).append(">\n");
			} else if(line.startsWith("* ")) {
				// List item
				if(!inList) {
					html.append("<ul>\n");
					inList = true;
				}
				String listItemText = addInlines(line.substring(1).trim());
				html.append("<li>").append(listItemText).append("</li>\n");
			} else if(line.startsWith("$$") && line.endsWith("$$")) {
				// Block equation
				String equation = unescape(line.substring(2, line.length() - 2).trim());
				html.append("<div class=\"blog-equation-block\">").append(equation).append("</div>\n");
			} else if(line.startsWith("![")) {
				// Image
				int captionEnd = line.indexOf(']');
				int urlStart = line.indexOf('(', captionEnd);
				int urlEnd = line.indexOf(')', urlStart);
				if(captionEnd != -1 && urlStart != -1 && urlEnd != -1) {
					String caption = unescape(line.substring(2, captionEnd));
					String url = unescape(line.substring(urlStart + 1, urlEnd));
					html.append("<div class=\"blog-image-container\">\n")
						.append("<img src=\"").append(url).append("\" alt=\"").append(caption).append("\"/>\n")
						.append("<div class=\"blog-image-caption\">").append(caption).append("</div>\n")
						.append("</div>\n");
				} else {
					html.append("<p>").append(addInlines(line)).append("</p>\n");
				}
			} else {
				// Paragraph
				html.append("<p>").append(addInlines(line)).append("</p>\n");
			}
		}
		
		html.append("</article>\n");
		return html.toString();
	}
	
	private String unescape(String text) {
		return text.replace("\\#", "#")
				   .replace("\\*", "*")
				   .replace("\\$", "$")
				   .replace("\\[", "[")
				   .replace("\\]", "]")
				   .replace("\\(", "(")
				   .replace("\\)", ")")
				   .replace("\\_", "_")
				   .replace("\\`", "`")
				   .replace("\\<", "&lt;")
				   .replace("\\>", "&gt;")
				   .replace("\\&", "&amp;");
	}
	
	private String addInlines(String text) {
		// Process links first
		StringBuilder result = new StringBuilder();
		
		int linkMidpoint = text.indexOf("](");
		while(linkMidpoint != -1) {
			int linkStart = text.lastIndexOf("[", linkMidpoint);
			int linkEnd = text.indexOf(")", linkMidpoint);
			if(linkStart == -1 || linkEnd == -1) {
				linkMidpoint = text.indexOf("](", linkMidpoint + 2);
				continue;
			}
			
			String beforeLink = text.substring(0, linkStart);
			String linkText = text.substring(linkStart + 1, linkMidpoint);
			String linkURL = text.substring(linkMidpoint + 2, linkEnd);
			result.append(beforeLink)
				  .append("<a href=\"").append(linkURL).append("\">")
				  .append(linkText)
				  .append("</a>");
			
			text = text.substring(linkEnd + 1);
			linkMidpoint = text.indexOf("](");
		}
		
		result.append(text);
		
		// Now process inline equations, bold, italics, and code
		String text2 = result.toString();
		result.setLength(0);
		
		boolean inEquation = false;
		boolean inBold = false;
		boolean inItalics = false;
		boolean inCode = false;
		
		int index = 0;
		while(index < text2.length()) {
			char c = text2.charAt(index);
			if(c == '\\') {
				// Escaped character, skip it
				result.append(c);
				if(index + 1 < text2.length()) {
					result.append(text2.charAt(index + 1));
					index += 2;
				} else {
					index++;
				}
			} else if(c == '$' && !inCode) {
				if(inEquation) {
					result.append("</span>");
					inEquation = false;
				} else if(text2.indexOf("$", index + 1) != -1) { // Doesn't consider escaping, but hopefully it's fine
					result.append("<span class=\"blog-equation-inline\">");
					inEquation = true;
				}
				index++;
			} else if(c == '*' && index + 1 < text2.length() && text2.charAt(index + 1) == '*' && !inCode && !inEquation) {
				if(inBold) {
					result.append("</span>");
					inBold = false;
				} else if(text2.indexOf("**", index + 2) != -1) {
					result.append("<span class=\"blog-bold\">");
					inBold = true;
				}
				index += 2; // Skip the second *
			} else if(c == '_' && !inCode && !inEquation) {
				if(inItalics) {
					result.append("</span>");
					inItalics = false;
				} else if(text2.indexOf("_", index + 1) != -1) {
					result.append("<span class=\"blog-italics\">");
					inItalics = true;
				}
				index++;
			} else if(c == '`' && !inEquation) {
				if(inCode) {
					result.append("</code>");
					inCode = false;
				} else if(text2.indexOf("`", index + 1) != -1){
					result.append("<code>");
					inCode = true;
				}
				index++;
			} else {
				result.append(c);
				index++;
			}
		}
		
		return unescape(result.toString());
	}
}
