package page.eiim.cubestats.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import page.eiim.cubestats.tasks.TaskBayesEval.Parameters;

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
	
	public static Instance getInstance(PersonData personData) {
		return new Instance(personData);
	}
	
	static class Instance {
		private static final String ORDER = "222333444555666777pyramskewbsq1minxclock333oh222bf333bf444bf555bf";
		private StringBuilder sb = new StringBuilder();
		private PersonData currentPersonData = null;
		private String[] orderedEvents = null;
		private String defaultEvent = null;
		
		public Instance() {}
		
		public Instance(PersonData personData) {
			currentPersonData = personData;
			Set<String> events = personData.eventParams.keySet();
			orderedEvents = events.stream().sorted((a, b) -> {
				int indexA = ORDER.indexOf(a);
				int indexB = ORDER.indexOf(b);
				if(indexA == -1) indexA = Integer.MAX_VALUE;
				if(indexB == -1) indexB = Integer.MAX_VALUE;
				return Integer.compare(indexA, indexB);
			}).toArray(String[]::new);
			defaultEvent = events.contains("333") ? "333" : orderedEvents[0]; // Always select 3x3 as default if available
		}
		
		public Instance buildHead(String title, String description, ResourceCategory category) {
			if(category == ResourceCategory.PERSON && currentPersonData == null) {
				throw new IllegalStateException("PersonData must be provided to build a person page");
			}
			if(category == ResourceCategory.PERSON) {
				if(title == null) title = currentPersonData.name + " - CubeStats";
				if(description == null) description = "Statistical analysis of WCA results for " + currentPersonData.name;
			}
			
			if(title == null) title = "CubeStats";
			sb.append(PageBuilder.HTML_START).append("<title>").append(title).append("</title>\n");
			if(description != null) sb.append("<meta name=\"description\" content=\"").append(description).append("\">\n");
			
			if(category == ResourceCategory.PERSON) {
				// Add person-specific JS variables
				sb.append("<script>\nconst params = {\n");
				for(int i = 0; i < orderedEvents.length; i++) {
					String event = orderedEvents[i];
					Parameters p = currentPersonData.eventParams.get(event);
					sb.append("\t\"").append(event).append("\": {\n")
						.append("\t\tmu: ").append(p.mu_a()).append(",\n")
						.append("\t\tm: ").append(p.m()).append(",\n")
						.append("\t\talpha: ").append(p.alpha()).append(",\n")
						.append("\t\ttheta: ").append(p.theta()).append(",\n")
						.append("\t\talep: ").append(p.alep()).append(",\n")
						.append("\t\tbet: ").append(p.bet()).append("\n")
						.append("\t}");
					if(i < orderedEvents.length - 1) sb.append(",\n");
					else sb.append("\n");
				}
				sb.append("};\ncurEvent = \"").append(defaultEvent).append("\";\n</script>\n");
			}
			
			switch(category) {
				case NONE -> {}
				case BLOG -> sb.append("<link rel=\"stylesheet\" href=\"/blog.css\">\n");
				case PERSON -> {
					sb.append("<link rel=\"stylesheet\" href=\"/person.css\">\n<script src=\"/tcdf.js\"></script>\n<script src=\"/person.js\"></script>\n");
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
		
		public Instance addPersonBody() {
			if(currentPersonData == null) {
				throw new IllegalStateException("PersonData must be provided to build a person page");
			}
			sb.append("<div id=\"personHeader\">\n<div id=\"personImg\"><img src=\"")
				.append(currentPersonData.avatarURL)
				.append("\"/></div>\n<div id=\"personRef\"><div id=\"personName\">")
				.append(currentPersonData.name)
				.append("</div>\n<div id=\"personId\">")
				.append(currentPersonData.wcaID)
				.append("</div></div>\n<a id=\"personLink\" href=\"https://worldcubeassociation.org/persons/")
				.append(currentPersonData.wcaID)
				.append("\"><img src=\"/wca_responsive.svg\"/></a>\n</div>\n<div id=\"personEventSelector\">\n");
			for(String event : orderedEvents) {
				sb.append("<button class=\"selectorEvent selEv");
				sb.append(event.substring(0, 1).toUpperCase());
				sb.append(event.substring(1));
				if(event.equals(defaultEvent)) {
					sb.append(" selected");
				}
				sb.append("\" onclick=\"setEvent('").append(event).append("')\"></button>\n");
			}
			sb.append("""
						</div>
						<div id="cumulativeDiv">
							<input type="checkbox" id="cumulativeInput">
							<label for="cumulativeInput">Cumulative distribution</label>
						</div>
						<div id="timeGraph">
							<div id="tgAxisY">
								<div id="tgAxisYLabel">Probability of getting time</div>
								<div id="tgAxisYTicks">
									<div class="tgAxisYTick"><span>0.04</span><div></div></div>
									<div class="tgAxisYTick"><span>0.03</span><div></div></div>
									<div class="tgAxisYTick"><span>0.02</span><div></div></div>
									<div class="tgAxisYTick"><span>0.01</span><div></div></div>
									<div class="tgAxisYTick"><span>0</span><div></div></div>
								</div>
							</div>
							<div id="tgAxisX">
								<div id="tgAxisXTicks">
									<div class="tgAxisXTick"><div></div><span>0s</span></div>
									<div class="tgAxisXTick"><div></div><span>10s</span></div>
									<div class="tgAxisXTick"><div></div><span>20s</span></div>
									<div class="tgAxisXTick"><div></div><span>30s</span></div>
									<div class="tgAxisXTick"><div></div><span>40s</span></div>
								</div>
							</div>
							<div id="tgChartArea">
								<svg viewBox="0 0 1000 600" style="display:block">
									<polyline fill="none" stroke="var(--brand-orange") stroke-width="3px" points="0,600 100,500 175,100 220,0 265,100 320,300 425,500 500,550 700,585 1000,600"/>
									<polygon fill="var(--brand-orange)" fill-opacity=".25" points="0,600 100,500 175,100 220,0 265,100 320,300 425,500 500,550 700,585 1000,600 1000,610 0,610"/>
								</svg>
								<div id="tgChartTooltip">hi</div>
							</div>
						</div>
						<div id="stats">
							<div><div>Median Time</div><div id="medianTime"></div></div>
							<div><div>Est. DNF Rate</div><div id="dnfRate"></div></div>
						</div>
					""");
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
	
	public static record PersonData(String name, String wcaID, String avatarURL, Map<String, Parameters> eventParams) {}
}
