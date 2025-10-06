package page.eiim.cubestats.web;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.jetty.compression.server.CompressionConfig;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MainServer {
	public static void run(String[] args) {
		// Load configuration from json file
		String configPath = args.length > 0 ? args[0] : "config.json";
		System.out.println("Loading configuration from " + configPath);
		JsonObject root;
		try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
			root = JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		int minThreadPoolSize = root.has("min_thread_pool_size") ? root.get("min_thread_pool_size").getAsInt() : 8;
		int maxThreadPoolSize = root.has("max_thread_pool_size") ? root.get("max_thread_pool_size").getAsInt() : 200;
		QueuedThreadPool threadPool = new QueuedThreadPool(maxThreadPoolSize, minThreadPoolSize);
		threadPool.setName("serversPool");
		
		String resourcesRoot = root.get("resources_root").getAsString();
		PageBuilder.setup(resourcesRoot);
		
		String hostname = root.get("hostname").getAsString();
		WebServerBuilder serverBuilder = new WebServerBuilder(threadPool, hostname);
		
		// Set up HTTP/HTTPS/QUIC
		JsonObject networking = root.has("networking") ? root.getAsJsonObject("networking") : new JsonObject();
		Server server = handleNetworking(networking, serverBuilder);
		
		// Enable request logging
		// TODO: allow customization of logging location
		boolean enableRequestLogging = root.has("enable_request_logging") ? root.get("enable_request_logging").getAsBoolean() : true;
		if(enableRequestLogging) {
			server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));
		}
		
		// Enable compression
		CompressionHandler compressionHandler = new CompressionHandler();
		CompressionConfig compressionConfig = CompressionConfig.builder().build();
		compressionHandler.putConfiguration("/*", compressionConfig);
		server.setHandler(compressionHandler);
		
		// Handle static resources
		ResourceHandler handler = new ResourceHandler();
		handler.setBaseResource(ResourceFactory.of(handler).newResource(resourcesRoot));
		handler.setDirAllowed(false);
		handler.setWelcomeFiles(List.of("index.html"));
		handler.setAcceptRanges(true);
		handler.setCacheControl("max-age=86400");
		handler.setPrecompressedFormats(CompressedContentFormat.GZIP);
		
		compressionHandler.setHandler(handler);
		
		ErrorHandler errorHandler = new ErrorHandler() {
			@Override
			protected void writeErrorHtml(Request request, Writer writer, Charset charset, int code, String message, Throwable cause) throws IOException {
				if (message == null)
					message = HttpStatus.getMessage(code);
				writer.write(PageBuilder.getInstance()
							.buildHead(message, code+" "+message, false)
							.startBody()
							.addLogo()
							.enterMain()
							.addRawHTML("<h1>" + code + " " + message + "</h1>")
							.signAndClose()
							.build());
			}
		};
		server.setErrorHandler(errorHandler);

		// Start the Server to start accepting connections from clients.
		try {
			server.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static Server handleNetworking(JsonObject networking, WebServerBuilder serverBuilder) {
		if(networking.has("http")) {
			JsonObject http = networking.getAsJsonObject("http");
			int port = http.get("port").getAsInt();
			boolean useHttp11 = http.has("http/1.1") ? http.get("http/1.1").getAsBoolean() : true;
			boolean useHttp2 = http.has("http/2") ? http.get("http/2").getAsBoolean() : false;
			serverBuilder.addHTTP(port, useHttp11, useHttp2);
		}
		
		boolean hasSSL = networking.has("ssl");
		if(hasSSL) {
			JsonObject ssl = networking.getAsJsonObject("ssl");
			String keyStorePath = ssl.get("keystore_path").getAsString();
			String keyStorePassword = ssl.get("keystore_password").getAsString();
			serverBuilder.setupSSL(keyStorePath, keyStorePassword);
		}
		
		if(networking.has("https")) {
			if(!hasSSL) {
				System.err.println("HTTPS configured but no SSL settings found");
			} else {
				JsonObject https = networking.getAsJsonObject("https");
				int port = https.get("port").getAsInt();
				boolean useHttp11 = https.has("http/1.1") ? https.get("http/1.1").getAsBoolean() : true;
				boolean useHttp2 = https.has("http/2") ? https.get("http/2").getAsBoolean() : true;
				serverBuilder.addHTTPS(port, useHttp11, useHttp2);
			}
		}
		
		if(networking.has("quic")) {
			if(!hasSSL) {
				System.err.println("QUIC configured but no SSL settings found");
			} else {
				JsonObject quic = networking.getAsJsonObject("quic");
				int port = quic.get("port").getAsInt();
				String pemWorkDir = quic.get("pem_work_dir").getAsString();
				serverBuilder.addQUIC(port, pemWorkDir);
			}
		}
		
		return serverBuilder.build();
	}
}