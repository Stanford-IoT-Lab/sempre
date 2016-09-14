package edu.stanford.nlp.sempre.api;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import com.sun.net.httpserver.HttpExchange;

import edu.stanford.nlp.sempre.*;
import fig.basic.LogInfo;
import fig.basic.Option;

public class OnlineLearnExchangeState extends AbstractHttpExchangeState {
  public static class Options {
    @Option(gloss = "Probability of storing the example in the test set instead of the train set.")
    public double testProbability = 0.1;
  }

  public static final Options opts = new Options();

  private final APIServer server;
  private final String sessionId;

  public OnlineLearnExchangeState(APIServer server, HttpExchange exchange) {
    super(exchange);
    this.server = server;

    if (reqParams.containsKey("sessionId"))
      sessionId = reqParams.get("sessionId");
    else
      sessionId = null;
  }

  @Override
  protected void doHandle() throws IOException {
    try {
      if (sessionId == null)
        throw new IllegalStateException("Missing session ID");

      String localeTag = reqParams.get("locale");
      LanguageContext language = localeToLanguage(server.langs, localeTag);

      String query = reqParams.get("q");
      if (query == null)
        throw new IllegalArgumentException("Missing query");

      String targetJson = reqParams.get("target");
      if (targetJson == null)
        throw new IllegalArgumentException("Missing target");

      // check if the target parses as JSON
      try {
        Json.readMapHard(targetJson);
      } catch (Exception e) {
        throw new IllegalArgumentException("Target is not valid JSON");
      }

      double diceRoll = ThreadLocalRandom.current().nextDouble();
      boolean storeAsTest = diceRoll > (1 - opts.testProbability);

      if (storeAsTest)
        LogInfo.logs("Storing %s as %s in the test set", query, targetJson);
      else
        LogInfo.logs("Learning %s as %s", query, targetJson);

      Session session = server.getSession(sessionId);
      Example ex;
      synchronized (session) {
        LogInfo.logs("session.lang %s, language.tag %s", session.lang, language.tag);
        if (session.lang != null && !session.lang.equals(language.tag))
          throw new IllegalArgumentException("Cannot change the language of an existing session");
        session.lang = language.tag;
        session.remoteHost = remoteHost;

        // we only learn in the ML sense if we still have the parsed example...
        if (!storeAsTest && session.lastEx != null && session.lastEx.utterance != null
            && session.lastEx.utterance.equals(query)) {
          ex = session.lastEx;

          ex.targetValue = new StringValue(targetJson);
          language.learner.onlineLearnExample(ex);
        }
      }

      if (storeAsTest) {
        language.testSetSaveQueue.add(new OnlineLearnEntry(query, targetJson));
      } else {
        // ... but we always save the example in the database, just in case
        // potentially this allows someone to DDOS our server with bad data
        // we just hope the ML model is resilient to that (and it should be)
        language.exactMatch.store(query, targetJson);
        language.onlineLearnSaveQueue.offer(new OnlineLearnEntry(query, targetJson));
      }

      // we would need to remove all entries from the cache that are affected by this learning step
      // (which potentially is all of them)
      // that would mean too many evictions
      // instead, we let the normal cache aging pick it up, and only remove the current utterance,
      // which we know for sure has changed
      language.cache.clear(query);
    } catch(IllegalStateException|IllegalArgumentException e) {
      returnError(400, e, sessionId);
      return;
    } catch(Exception e) {
      returnError(500, e, sessionId);
      e.printStackTrace();
      return;
    }

    returnOk("Learnt successfully", sessionId);
  }

}
