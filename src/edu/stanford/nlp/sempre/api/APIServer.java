package edu.stanford.nlp.sempre.api;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import edu.stanford.nlp.sempre.Master;
import edu.stanford.nlp.sempre.Session;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.Pair;
import fig.exec.Execution;

final class SecureIdentifiers {
  private SecureIdentifiers() {
  }

  private static SecureRandom random = new SecureRandom();

  public static String getId() {
    return new BigInteger(130, random).toString(32);
  }
}

public class APIServer implements Runnable {
  public static class Options {
    @Option
    public int port = 8400;
    @Option
    public int numThreads = 4;
    @Option
    public int verbose = 1;
    @Option
    public List<String> languages = Arrays.asList(new String[] { "en", "it", "es" });
    @Option
    public List<Pair<String, String>> onlineLearnFiles = new ArrayList<>();
    @Option
    public String accessToken = null;
  }

  public static Options opts = new Options();

  private final Map<String, Session> sessionMap = new HashMap<>();
  final Map<String, LanguageContext> langs = new ConcurrentHashMap<>();

  synchronized Session getSession(String sessionId) {
    if (sessionMap.containsKey(sessionId)) {
      return sessionMap.get(sessionId);
    } else {
      Session newSession = new Session(sessionId);
      sessionMap.put(sessionId, newSession);
      return newSession;
    }
  }

  private void recordOnlineLearnExampleInFile(String filename, String example, String targetJson) {
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(filename, true), "UTF-8")) {
      writer.write(example + "\t" + targetJson + "\n");
    } catch (IOException e) {
      LogInfo.logs("Failed to append online-learnt example: %s", e.getMessage());
    }
  }

  void recordOnlineLearnExample(String languageTag, String example, String targetJson) {
    // we don't want to write to the same file from two threads using two different buffered
    // writers (they would intermix and break the file format)
    // synchronizing on onlineLearnFiles achieves that and is as good as anything else
    synchronized (opts.onlineLearnFiles) {
      for (Pair<String, String> pair : opts.onlineLearnFiles) {
        if (pair.getFirst().equals(languageTag))
          recordOnlineLearnExampleInFile(pair.getSecond(), example, targetJson);
      }
    }
  }

  private class SessionGCTask extends TimerTask {
    @Override
    public void run() {
      gcSessions();
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

  private void addLanguage(String tag) {
    LanguageContext language = new LanguageContext(tag);
    langs.put(tag, language);
  }

  @Override
  public void run() {
    // Add supported languages
    for (String tag : opts.languages)
      addLanguage(tag);

    try {
      String hostname = fig.basic.SysInfoUtils.getHostName();
      HttpServer server = HttpServer.create(new InetSocketAddress(opts.port), 10);
      ExecutorService pool = Executors.newFixedThreadPool(opts.numThreads);
      server.createContext("/query", new HttpHandler() {
        @Override
        public void handle(HttpExchange exchange) {
          new QueryExchangeState(APIServer.this, exchange).run();
        }
      });
      server.createContext("/learn", new HttpHandler() {
        @Override
        public void handle(HttpExchange exchange) {
          new OnlineLearnExchangeState(APIServer.this, exchange).run();
        }
      });
      server.createContext("/admin/clear-cache", new HttpHandler() {
        @Override
        public void handle(HttpExchange exchange) {
          new ClearCacheExchangeState(APIServer.this, exchange).run();
        }
      });
      server.createContext("/admin/reload", new HttpHandler() {
        @Override
        public void handle(HttpExchange exchange) {
          new ReloadParametersExchangeState(APIServer.this, exchange).run();
        }
      });
      server.setExecutor(pool);
      server.start();
      LogInfo.logs("Server started at http://%s:%s/sempre", hostname, opts.port);

      Timer gcTimer = new Timer(true);
      gcTimer.schedule(new SessionGCTask(), 600000, 600000);

      try {
        while (!Thread.currentThread().isInterrupted())
          Thread.sleep(60000);
      } catch (InterruptedException e) {
      }

      server.stop(0);
      pool.shutdown();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    Execution.run(args, "Main", new APIServer(), Master.getOptionsParser());
  }
}
