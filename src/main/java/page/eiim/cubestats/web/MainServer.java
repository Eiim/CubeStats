package page.eiim.cubestats.web;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;

import org.eclipse.jetty.compression.server.CompressionConfig;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.google.gson.JsonObject;

import page.eiim.cubestats.Settings;
import page.eiim.cubestats.web.PageBuilder.ResourceCategory;

public class MainServer {
	
	private Server server;
	
	public MainServer(Settings settings, JsonObject networking) {
		QueuedThreadPool threadPool = new QueuedThreadPool(settings.maxThreadPoolSize, settings.minThreadPoolSize);
		threadPool.setName("serversPool");
		
		PageBuilder.setup(settings.resourcesRoot);
		
		String hostname = settings.hostname;
		WebServerBuilder serverBuilder = new WebServerBuilder(threadPool, hostname);
		
		// Set up HTTP/HTTPS/QUIC
		server = handleNetworking(networking, serverBuilder);
		
		// Enable request logging
		// TODO: allow customization of logging location
		if(settings.enableRequestLogging) {
			server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));
		}
		
		// Enable compression
		CompressionHandler compressionHandler = new CompressionHandler();
		CompressionConfig compressionConfig = CompressionConfig.builder().build();
		compressionHandler.putConfiguration("/*", compressionConfig);
		server.setHandler(compressionHandler);
		
		// Handle static resources
		ResourceHandler staticHandler = new ResourceHandler();
		staticHandler.setBaseResource(ResourceFactory.of(staticHandler).newResource(settings.resourcesRoot.getPath()));
		staticHandler.setDirAllowed(false);
		staticHandler.setWelcomeFiles(List.of("index.html"));
		staticHandler.setAcceptRanges(true);
		staticHandler.setCacheControl("max-age=86400");
		staticHandler.setPrecompressedFormats(CompressedContentFormat.GZIP);
		
		Handler.Sequence pageHandlers = new Handler.Sequence();
		ContextHandlerCollection contextHandlers = new ContextHandlerCollection();
		
		try {
			CubeSearch cubeSearch = new CubeSearch(DatabaseConnector.getConnection(settings.dbUserName, settings.dbPassword, settings.liveSchema.url()));
			ContextHandler searchHandler = new ContextHandler(new SearchHandler(cubeSearch), "/searchapi");
			searchHandler.setAllowNullPathInContext(true);
			contextHandlers.addHandler(searchHandler);
			PersonHandler personHandler = new PersonHandler(DatabaseConnector.getConnection(settings.dbUserName, settings.dbPassword, settings.liveSchema.url()));
			ContextHandler cph = new ContextHandler(personHandler, "/person");
			contextHandlers.addHandler(cph);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		pageHandlers.addHandler(contextHandlers);
		pageHandlers.addHandler(staticHandler); // Default to static handler if nothing else matches
		compressionHandler.setHandler(pageHandlers);
		
		ErrorHandler errorHandler = new ErrorHandler() {
			@Override
			protected void writeErrorHtml(Request request, Writer writer, Charset charset, int code, String message, Throwable cause) throws IOException {
				if (message == null)
					message = HttpStatus.getMessage(code);
				writer.write(PageBuilder.getInstance()
							.buildHead(message, code+" "+message, ResourceCategory.NONE)
							.startBody()
							.addLogo()
							.enterMain()
							.addRawHTML("<h1>" + code + " " + message + "</h1>")
							.signAndClose()
							.build());
			}
		};
		server.setErrorHandler(errorHandler);
	}
	
	public void start() {
		System.out.println("Starting web server");
		try {
			server.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean shutdown() {
		System.out.println("Shutting down web server");
		try {
			server.stop();
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
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