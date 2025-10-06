package page.eiim.cubestats.web;

import java.nio.file.Path;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory;
import org.eclipse.jetty.quic.quiche.server.QuicheServerConnector;
import org.eclipse.jetty.quic.quiche.server.QuicheServerQuicConfiguration;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ThreadPool;

public class WebServerBuilder {
	
	private final Server server;
	private final String host;
	private SslContextFactory.Server sslContextFactory;

	public WebServerBuilder(ThreadPool threadPool, String host) {
		server = new Server(threadPool);
		this.host = host;
	}
	
	public void setupSSL(String keyStorePath, String keyStorePassword) {
		sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(keyStorePath); // Note that certbot doesn't provide a PKCS12 file, so you may need to create it with openssl
		sslContextFactory.setKeyStorePassword(keyStorePassword);
	}
	
	public void addHTTP(int port, boolean useHttp11, boolean useHttp2) {
		if(!useHttp11 && !useHttp2) {
			throw new IllegalArgumentException("At least one of HTTP/1.1 or HTTP/2 must be enabled");
		}
		
		HttpConfiguration http = new HttpConfiguration();
		ConnectionFactory[] connections = new ConnectionFactory[(useHttp11 ? 1 : 0) + (useHttp2 ? 1 : 0)];
		int idx = 0;
		if(useHttp11) {
			connections[idx++] = new HttpConnectionFactory(http);
		}
		if(useHttp2) {
			connections[idx++] = new HTTP2CServerConnectionFactory(http);
		}
		ServerConnector connectorHTTP = new ServerConnector(server, connections);
		connectorHTTP.setPort(port);
		connectorHTTP.setHost(host);
		server.addConnector(connectorHTTP);
	}
	
	public void addHTTPS(int port, boolean useHttp11, boolean useHttp2) {
		if(!useHttp11 && !useHttp2) {
			throw new IllegalArgumentException("At least one of HTTP/1.1 or HTTP/2 must be enabled");
		}
		if(sslContextFactory == null) {
			throw new IllegalStateException("SSL context not set up. Call setupSSL() first.");
		}
		
		HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());
		
		ConnectionFactory[] connections = null;
		if(useHttp11 && useHttp2) {
			HttpConnectionFactory https11 = new HttpConnectionFactory(https);
			HTTP2ServerConnectionFactory https2 = new HTTP2ServerConnectionFactory(https);
			ALPNServerConnectionFactory httpsAlpn = new ALPNServerConnectionFactory(https11.getProtocol(), https2.getProtocol());
			httpsAlpn.setDefaultProtocol(https11.getProtocol());
			connections = new ConnectionFactory[] {null, httpsAlpn, https2, https11};
		} else if(useHttp2) {
			HTTP2ServerConnectionFactory https2 = new HTTP2ServerConnectionFactory(https);
			connections = new ConnectionFactory[] {null, https2};
		} else {
			HttpConnectionFactory https11 = new HttpConnectionFactory(https);
			connections = new ConnectionFactory[] {null, https11};
		}
		
		SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, connections[1].getProtocol());
		connections[0] = tls;

		ServerConnector connectorHTTPS = new ServerConnector(server, connections);
		connectorHTTPS.setPort(443); // TCP port 443
		connectorHTTPS.setHost("cube-stats.com");
		server.addConnector(connectorHTTPS);
	}
	
	public void addQUIC(int port, String pemWorkDir) {
		if(sslContextFactory == null) {
			throw new IllegalStateException("SSL context not set up. Call setupSSL() first.");
		}
		
		Path pemDir = Path.of(pemWorkDir);
		QuicheServerQuicConfiguration serverQuicConfig = new QuicheServerQuicConfiguration(pemDir);
		QuicheServerConnector quicConnector = new QuicheServerConnector(server, sslContextFactory, serverQuicConfig, new HTTP3ServerConnectionFactory());
		quicConnector.setPort(443); // UDP port 443
		server.addConnector(quicConnector);
	}
	
	public Server build() {
		return server;
	}

}
