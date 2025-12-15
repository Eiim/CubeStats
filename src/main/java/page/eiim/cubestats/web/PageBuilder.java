package page.eiim.cubestats.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class PageBuilder {
	
	static final String HTML_START = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			<meta charset="utf-8"/>
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<link rel="stylesheet" href="/main.css">
			<link rel="icon" type="image/svg+xml" sizes="any" href="/favicon.svg">
			<link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
			<link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
			<link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
			<script src="/main.js"></script>""";
	
	public static void setup(File resourcesRoot) {
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(resourcesRoot, "index.html")))) {
			writer.write(new Instance()
					.buildHead("CubeStats", "Some statistical analysis of WCA cubing results: under construction", ResourceCategory.NONE)
					.addSidebar()
					.startBody()
					.addLogo()
					.enterMain()
					.addRawHTML("<h1 style=\"margin-top:180px\">Coming soon!</h1>")
					.signAndClose()
					.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		File postsDir = new File(resourcesRoot, "posts/");
		File[] postFiles = postsDir.listFiles((_, name) -> name.endsWith(".txt"));
		BlogPost[] blogPosts;
		if(postFiles == null) {
			blogPosts = new BlogPost[0];
		} else {
			int n = postFiles.length;
			blogPosts = new BlogPost[n];
			for(int i = 0; i < n; i++) {
				try {
					blogPosts[i] = new BlogPost(postFiles[i]);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		
		// Create HTML for each blog post
		File blogRoot = new File(resourcesRoot, "blog/");
		blogRoot.mkdirs();
		for(BlogPost post : blogPosts) {
			File postFile = new File(blogRoot, post.path + ".html");
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(postFile))) {
				writer.write(new Instance()
						.buildHead(post.title + " - CubeStats", post.title, ResourceCategory.BLOG)
						.addSidebar()
						.startBody()
						.addLogo()
						.enterMain()
						.addRawHTML(post.bodyHTML)
						.signAndClose()
						.build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Sort posts by date descending
		Arrays.sort(blogPosts, (a, b) -> b.date.compareTo(a.date));
		
		StringBuilder blogListBuilder = new StringBuilder();
		for(BlogPost post : blogPosts) {
			blogListBuilder.append("<a class=\"blogEntryLink\" href=\"/blog/").append(post.path).append(".html\"><div class=\"blogEntry\">\n")
				.append("<h3>").append(post.title).append("</h3>\n")
				.append("<h5 class=\"date\">").append(post.date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</h5>\n")
				.append("<p>").append(post.abstractText).append("</p>\n")
				.append("</div></a>\n");
		}
		String blogPostListHTML = blogListBuilder.toString();
		
		// Create blog index page
		File blogIndexFile = new File(blogRoot, "index.html");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(blogIndexFile))) {
			writer.write(new Instance()
					.buildHead("Blog - CubeStats", "A data science blog about speedcubing analysis", ResourceCategory.BLOG)
					.addSidebar()
					.startBody()
					.addLogo()
					.enterMain()
					.addRawHTML(blogPostListHTML)
					.signAndClose()
					.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Built static pages");
	}

	public static Instance getInstance() {
		return new Instance();
	}
	
	static class Instance {
		private static StringBuilder sb = new StringBuilder();
		
		public Instance() {}
		
		public Instance buildHead(String title, String description, ResourceCategory category) {
			if(title == null) title = "CubeStats";
			sb.append(PageBuilder.HTML_START).append("<title>").append(title).append("</title>\n");
			if(description != null) sb.append("<meta name=\"description\" content=\"").append(description).append("\">\n");
			switch(category) {
				case NONE -> {}
				case BLOG -> sb.append("<link rel=\"stylesheet\" href=\"/blog.css\">\n");
				case PERSON -> {
					sb.append("<link rel=\"stylesheet\" href=\"/person.css\">\n");
					//sb.append("<link rel=\"stylesheet\" href=\"https://cdn.cubing.net/v0/css/@cubing/icons/css\">\n");
				}
			}
			sb.append("</head>\n<body>\n");
			return this;
		}
		
		public Instance addSidebar() {
			sb.append("""
					<div id="sidebarToggle">☰</div>
					<div id="sidebar">
						<div class="sidebarEntry"><a href="/">Coming Soon</a></div>
						<div class="sidebarEntry"><a href="/blog">Blog</a></div>
					</div>
					""");
			return this;
		}
		
		public Instance startBody() {
			sb.append("""
					<div id="bodyContainer">
					<div class="bg-tile"></div>
					""");
			return this;
		}
		
		public Instance addLogo() {
			sb.append("""
					<a href="/"><div id="logoBox">
						<img src="/logo_responsive.svg" alt="CubeStats Logo"/>
					</div></a>
					""");
			return this;
		}
		
		public Instance enterMain() {
			sb.append("<div id=\"main\">\n");
			return this;
		}
		
		public Instance signAndClose() {
			sb.append("""
					<footer>Made by <a href="https://eiim.page/">Ethan Chapman</a> (<a href="https://www.worldcubeassociation.org/persons/2024CHAP08">2024CHAP08</a>)</footer>
					</div>
					</div>
					</body>
					</html>
					""");
			return this;
		}
		
		public Instance addRawHTML(String html) {
			sb.append(html);
			return this;
		}
		
		public String build() {
			String result = sb.toString();
			sb.setLength(0);
			return result;
		}
	}
	
	public static enum ResourceCategory {
		NONE, BLOG, PERSON;
	}
}
