package edu.stanford.nlp.sempre.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.thingtalk.ThingpediaDataset;
import fig.basic.LogInfo;

public class OnlineLearnExchangeState extends AbstractHttpExchangeState {
  public static enum LearnType {
    no, automatic, online
  };

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

  private static final Pattern NAME_REGEX = Pattern.compile("^tt:([^\\.]+)\\.(.+)$");

  private static void extractSchema(List<String> into, Map<?, ?> invocation) {
    if (invocation == null)
      return;

    Object name = invocation.get("name");

    CharSequence fullName;
    if (name instanceof CharSequence)
      fullName = (CharSequence) name;
    else
      fullName = (CharSequence) ((Map<?, ?>) name).get("id");

    Matcher matcher = NAME_REGEX.matcher(fullName);
    if (!matcher.matches())
      return;

    into.add(matcher.group(1));
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
      Map<String, Object> parsed;
      try {
        parsed = Json.readMapHard(targetJson);
      } catch (Exception e) {
        throw new IllegalArgumentException("Target is not valid JSON");
      }

      // figure out what schemas are involved in this example
      List<String> schemas = new ArrayList<>();
      try {
        if (parsed.containsKey("rule")) {
          Map<?, ?> rule = (Map<?, ?>) parsed.get("rule");
          extractSchema(schemas, (Map<?, ?>) rule.get("trigger"));
          extractSchema(schemas, (Map<?, ?>) rule.get("query"));
          extractSchema(schemas, (Map<?, ?>) rule.get("action"));
        } else {
          extractSchema(schemas, (Map<?, ?>) parsed.get("trigger"));
          extractSchema(schemas, (Map<?, ?>) parsed.get("query"));
          extractSchema(schemas, (Map<?, ?>) parsed.get("action"));
        }
      } catch (ClassCastException e) {
        throw new IllegalArgumentException("Target is not valid SEMPRE JSON");
      }

      LearnType storeAs = LearnType.valueOf(reqParams.getOrDefault("store", "automatic"));
      
      if (storeAs != LearnType.no)
        LogInfo.logs("Storing %s as %s in the %s set", query, targetJson, storeAs.toString());
      else
        LogInfo.logs("Not storing %s", query);

      Session session = server.getSession(sessionId);
      Example ex = null;
      synchronized (session) {
        LogInfo.logs("session.lang %s, language.tag %s", session.lang, language.tag);
        if (session.lang != null && !session.lang.equals(language.tag))
          throw new IllegalArgumentException("Cannot change the language of an existing session");
        session.lang = language.tag;
        session.remoteHost = remoteHost;

        // we only learn in the ML sense if we still have the parsed example
        // and this is not something coming from the app automatically
        if (LearnType.automatic != storeAs && session.lastEx != null && session.lastEx.utterance != null
            && session.lastEx.utterance.equals(query)) {
          ex = session.lastEx;

          ex.targetValue = new StringValue(targetJson);
          //language.learner.onlineLearnExample(ex);
        }
      }

      // reuse the CoreNLP analysis if possible
      if (storeAs == LearnType.online) {
        if (ex != null)
          language.exactMatch.store(ex, targetJson);
        else
          language.exactMatch.store(query, targetJson);
      }

      if (storeAs != LearnType.no) {
        ThingpediaDataset.storeExample(query, targetJson, language.tag, storeAs.toString(), schemas);
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
