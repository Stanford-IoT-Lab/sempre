package edu.stanford.nlp.sempre;

import static fig.basic.LogInfo.logs;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class APIServer implements Runnable {
	public static class Options {
		@Option
		public int port = 8400;
		@Option
		public int ssl_port = -1;
		@Option
		public int numThreads = 4;
		@Option
		public int verbose = 1;
		@Option
		public String chuid = null;
	}

	public static Options opts = new Options();

	private final Map<String, Session> sessionMap = new HashMap<>();
	private final Builder builder;

	private class QueryHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) {
			try {
				new ExchangeState(exchange);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ExchangeState {
		static private final int MAX_ITEMS = 5;

		// Input
		private HttpExchange exchange;
		private Map<String, String> reqParams = new HashMap<>();
		private String remoteHost;

		// For header
		private HttpCookie cookie;
		private boolean isNewSession;

		public ExchangeState(HttpExchange exchange) throws IOException {
			this.exchange = exchange;

			URI uri = exchange.getRequestURI();
			this.remoteHost = exchange.getRemoteAddress().getHostName();

			// Don't use uri.getQuery: it can't distinguish between '+' and '-'
			String[] tokens = uri.toString().split("\\?");
			if (tokens.length == 2) {
				for (String s : tokens[1].split("&")) {
					String[] kv = s.split("=", 2);
					try {
						String key = URLDecoder.decode(kv[0], "UTF-8");
						String value = URLDecoder.decode(kv[1], "UTF-8");
						logs("%s => %s", key, value);
						reqParams.put(key, value);
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}
			}

			String cookieStr = exchange.getRequestHeaders().getFirst("Cookie");
			if (cookieStr != null) {  // Cookie already exists
				cookie = HttpCookie.parse(cookieStr).get(0);
				isNewSession = false;
			} else {
				cookie = new HttpCookie("sessionId", SecureIdentifiers.getId());
				isNewSession = true;  // Create a new cookie
			}

			String sessionId = null;
			if (cookie != null) sessionId = cookie.getValue();
			if (opts.verbose >= 2)
				LogInfo.logs("GET %s from %s (%ssessionId=%s)", uri, remoteHost, isNewSession ? "new " : "", sessionId);

			handleQuery(sessionId);

			exchange.close();
		}

		private void setHeaders(int status) throws IOException {
			Headers headers = exchange.getResponseHeaders();
			headers.set("Content-Type", "application/json;charset=utf8");
			if (isNewSession && cookie != null)
				headers.set("Set-Cookie", cookie.toString());
			exchange.sendResponseHeaders(status, 0);
		}

		private String makeJson(List<Derivation> response) {
			Map<String, Object> json = new HashMap<>();
			List<Object> items = new ArrayList<>();
			json.put("candidates", items);

			int nItems = MAX_ITEMS;
			for (Derivation deriv : response) {
				if (nItems == 0)
					break;
				nItems--;

				Map<String, Object> item = new HashMap<>();
				Value value = deriv.getValue();
				if (value == null)
					item.put("answer", null);
				else if (value instanceof StringValue)
					item.put("answer", ((StringValue) value).value);
				else if (value instanceof ListValue)
					item.put("answer", ((StringValue) ((ListValue) value).values.get(0)).value);
				item.put("score", deriv.score);
				item.put("prob", deriv.prob);
				items.add(item);
			}

			return Json.writeValueAsStringHard(json);
		}

		private String makeJson(Exception e) {
			Map<String, Object> json = new HashMap<>();
			json.put("error", e.getMessage());
			return Json.writeValueAsStringHard(json);
		}

		private List<Derivation> handleUtterance(Session session, String query) {
			session.updateContext();

			// Create example
			Example.Builder b = new Example.Builder();
			b.setId("session:" + session.id);
			b.setUtterance(query);
			b.setContext(session.context);
			Example ex = b.createExample();

			ex.preprocess();

			// Parse!
			builder.parser.parse(builder.params, ex, false);

			return ex.getPredDerivations();
		}

		private void handleQuery(String sessionId) throws IOException {
			logs("handleQuery");
			Session session = getSession(sessionId);
			synchronized (session) {
				handleQueryForSession(session);
			}
		}

		private void handleQueryForSession(Session session) throws IOException {
			logs("handleQueryForSession");
			session.remoteHost = remoteHost;

			String query = reqParams.get("q");
			logs("Server.handleQuery %s: %s", session.id, query);

			// Handle the request
			List<Derivation> derivations = null;
			int exitStatus;
			Exception error = null;

			try {
				if (query != null) {
					derivations = handleUtterance(session, query);
					exitStatus = 200;
				} else {
					exitStatus = 400;
					error = new RuntimeException("Bad Request");
				}
			} catch (Exception e) {
				exitStatus = 500;
				error = e;
			}

			// Print header
			setHeaders(exitStatus);
			// Print body
			PrintWriter out = new PrintWriter(new OutputStreamWriter(exchange.getResponseBody()));
			if (error != null)
				out.println(makeJson(error));
			else
				out.println(makeJson(derivations));

			out.close();
		}
	}

	private class SessionGCTask extends TimerTask {
		@Override
		public void run() {
			gcSessions();
		}
	}

	public APIServer() {
		builder = new Builder();
	}

	private synchronized Session getSession(String sessionId) {
		if (sessionMap.containsKey(sessionId)) {
			return sessionMap.get(sessionId);
		} else {
			Session newSession = new Session(sessionId);
			sessionMap.put(sessionId, newSession);
			return newSession;
		}
	}

	private synchronized void gcSessions() {
		Iterator<Session> iter = sessionMap.values().iterator();
		long now = System.currentTimeMillis();

		while (iter.hasNext()) {
			Session session = iter.next();
			synchronized (session) {
				long lastTime = session.getLastAccessTime();
				if (lastTime - now > 300 * 1000) // 5 min
					iter.remove();
			}
		}
	}

	@Override
	public void run() {
		try {
			HttpServer server = null;
			HttpsServer ssl_server = null;
			if (opts.port >= 0)
				server = HttpServer.create(new InetSocketAddress(opts.port), 10);
			if (opts.ssl_port >= 0) {
				try {
					SSLContext context = SSLContext.getDefault();
					ssl_server = HttpsServer.create(new InetSocketAddress(opts.ssl_port), 10);
					// use default configuration
					ssl_server.setHttpsConfigurator(new HttpsConfigurator(context));
				} catch (NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
			}
			if (server == null && ssl_server == null)
				throw new RuntimeException("Must specify one of port or ssl_port");

			if (opts.chuid != null)
				PosixHelper.setuid(opts.chuid);

			builder.build();

			Dataset dataset = new Dataset();
			dataset.read();

			Learner learner = new Learner(builder.parser, builder.params, dataset);
			learner.learn();

			String hostname = fig.basic.SysInfoUtils.getHostName();
			ExecutorService pool = Executors.newFixedThreadPool(opts.numThreads);
			
			if (server != null) {
				server.createContext("/query", new QueryHandler());
				server.setExecutor(pool);
				server.start();
				LogInfo.logs("Server started at http://%s:%s/", hostname, opts.port);
			}
			if (ssl_server != null) {
				ssl_server.createContext("/query", new QueryHandler());
				ssl_server.setExecutor(pool);
				ssl_server.start();
				LogInfo.logs("Server started at https://%s:%s/", hostname, opts.port);
			}

			Timer gcTimer = new Timer(true);
			gcTimer.schedule(new SessionGCTask(), 600000, 600000);

			try {
				while (!Thread.currentThread().isInterrupted())
					Thread.sleep(60000);
			} catch (InterruptedException e) {
			}

			if (server != null)
				server.stop(0);
			if (ssl_server != null)
				ssl_server.stop(0);
			pool.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Execution.run(args, "Main", new APIServer(), Master.getOptionsParser());
	}
}
