package edu.stanford.nlp.sempre.thingtalk;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import edu.stanford.nlp.sempre.*;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.Pair;

public class ThingpediaDataset extends AbstractDataset {
  public static class Options {
    @Option
    public String languageTag = "en";
    @Option
    public boolean includeCanonical = true;
    @Option
    public boolean includeTest = true;
    @Option
    public Set<String> testTypes = Sets.newHashSet("test");
    @Option
    public Set<String> trainTypes = Sets.newHashSet("thingpedia", "online");

    @Option
    public List<Pair<String, Double>> trainWeights = Collections.emptyList();
  }

  public static Options opts = new Options();

  private final ThingpediaDatabase dataSource;

  private static final String CANONICAL_QUERY = "select dscc.canonical,ds.kind,dsc.name,dsc.channel_type,dsc.argnames,dscc.argcanonicals,dsc.types from device_schema_channels dsc, device_schema ds, "
      + " device_schema_channel_canonicals dscc where dsc.schema_id = ds.id and dsc.version = ds.developer_version and ds.approved_version is not null and "
      + " dscc.schema_id = dsc.schema_id and dscc.version = dsc.version and dscc.name = dsc.name and language = ? "
      + " and canonical is not null and ds.kind_type <> 'global'";
  private static final String FULL_EXAMPLE_QUERY = "select id, type, utterance, target_json from example_utterances where not is_base and language = ?";
  private static final String RAW_EXAMPLE_QUERY = "select id, type, utterance, target_json from example_utterances where not is_base and language = ? "
      + "and type in ('online', 'test')";
  private static final String INSERT_QUERY = "insert into example_utterances(type, language, utterance, target_json) values (?, ?, ?, ?)";
  private static final String INSERT_SCHEMA_REF_QUERY = "insert into example_rule_schema(example_id, schema_id) select ?, id from device_schema where kind = ?";

  public ThingpediaDataset() {
    dataSource = ThingpediaDatabase.getSingleton();
  }

  private void readCanonicals(Connection con) throws SQLException {
    int maxExamples = getMaxExamplesForGroup("train");
    List<Example> examples = getOrCreateGroup("train");

    try (PreparedStatement stmt = con.prepareStatement(CANONICAL_QUERY)) {
      stmt.setString(1, opts.languageTag);
      try (ResultSet set = stmt.executeQuery()) {
        TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
        };

        while (set.next() && examples.size() < maxExamples) {
          String canonical = set.getString(1);
          String kind = set.getString(2);
          if (!ThingpediaLexicon.opts.subset.isEmpty() && !ThingpediaLexicon.opts.subset.contains(kind))
            continue;

          String name = set.getString(3);
          String channelType = set.getString(4);
          List<String> argnames = Json.readValueHard(set.getString(5), typeRef);
          List<String> argcanonicals = Json.readValueHard(set.getString(6), typeRef);
          List<Type> argtypes = Json.readValueHard(set.getString(7), typeRef).stream().map((s) -> Type.fromString(s))
              .collect(Collectors.toList());
          Value inner;
          switch (channelType) {
          case "action":
            inner = ThingTalk.actParam(new ChannelNameValue(kind, name, argnames, argcanonicals, argtypes));
            break;
          case "trigger":
            inner = ThingTalk.trigParam(new ChannelNameValue(kind, name, argnames, argcanonicals, argtypes));
            break;
          case "query":
            inner = ThingTalk.queryParam(new ChannelNameValue(kind, name, argnames, argcanonicals, argtypes));
            break;
          default:
            throw new RuntimeException("Invalid channel type " + channelType);
          }
          Value targetValue = ThingTalk.jsonOut(inner);
          if (channelType.equals("trigger"))
            canonical = "monitor when " + canonical;

          Example ex = new Example.Builder()
              .setId("canonical_" + kind + "_" + name)
              .setUtterance(canonical)
              .setTargetValue(targetValue)
              .createExample();

          addOneExample(ex, maxExamples, examples);
        }
      }
    }
  }

  private Set<String> findAllKinds(String targetJson) {
    Map<String, Object> map = Json.readMapHard(targetJson);
    Set<String> ret = new HashSet<>();

    if (map.containsKey("rule")) {
      Map<?, ?> rule = (Map<?, ?>) map.get("rule");
      findKinds(ret, (Map<?, ?>) rule.get("trigger"));
      findKinds(ret, (Map<?, ?>) rule.get("query"));
      findKinds(ret, (Map<?, ?>) rule.get("action"));
    } else {
      findKinds(ret, (Map<?, ?>) map.get("trigger"));
      findKinds(ret, (Map<?, ?>) map.get("query"));
      findKinds(ret, (Map<?, ?>) map.get("action"));
    }
    return ret;
  }

  private String getId(Object obj) {
    if (obj instanceof String)
      return (String) obj;
    Map<?, ?> map = (Map<?, ?>) obj;
    if (map.containsKey("value"))
      return (String) map.get("value");
    return (String) map.get("id");
  }

  private void findKinds(Set<String> ret, Map<?, ?> map) {
    if (map == null)
      return;
    String function = getId(map.get("name"));
    if (function.startsWith("tt:"))
      function = function.substring("tt:".length());

    String[] kindAndNames = function.split("\\.");
    ret.add(Joiner.on('.').join(Arrays.asList(kindAndNames).subList(0, kindAndNames.length - 1)));
  }

  private boolean maybeFilterSubset(String targetJson) {
    if (ThingpediaLexicon.opts.subset.isEmpty())
      return true;

    Set<String> kinds = findAllKinds(targetJson);
    return ThingpediaLexicon.opts.subset.containsAll(kinds);
  }

  private void readFullExamples(Connection con) throws SQLException {
    int trainMaxExamples = getMaxExamplesForGroup("train");
    List<Example> trainExamples = getOrCreateGroup("train");
    int testMaxExamples = getMaxExamplesForGroup("test");
    List<Example> testExamples = getOrCreateGroup("test");

    // fast path running manually with no training
    if (trainMaxExamples == 0 && testMaxExamples == 0)
      return;

    Map<String, Double> weights = new HashMap<>();
    boolean applyWeights = false;

    if (opts.trainWeights.size() > 0) {
      applyWeights = true;
      for (Pair<String, Double> pair : opts.trainWeights)
        weights.put(pair.getFirst(), pair.getSecond());
    }

    try (PreparedStatement stmt = con.prepareStatement(FULL_EXAMPLE_QUERY)) {
      stmt.setString(1, opts.languageTag);
      try (ResultSet set = stmt.executeQuery()) {

        while (set.next() && (trainExamples.size() < trainMaxExamples || testExamples.size() < testMaxExamples)) {
          int id = set.getInt(1);
          String type = set.getString(2);
          String utterance = set.getString(3);
          String targetJson = set.getString(4);
          if (!maybeFilterSubset(targetJson))
            continue;
          Value targetValue = new StringValue(targetJson);

          boolean isTest;
          if (opts.testTypes.contains(type)) {
            if (!opts.includeTest)
              continue;
            isTest = true;
          } else {
            if (!opts.trainTypes.contains(type))
              continue;
            isTest = false;
          }

          List<Example> group;
          int maxGroup;
          if (isTest) {
            group = testExamples;
            maxGroup = testMaxExamples;
          } else {
            group = trainExamples;
            maxGroup = trainMaxExamples;
          }
          if (group.size() >= maxGroup)
            continue;

          Example ex = new Example.Builder()
              .setId(type + "_" + Integer.toString(id))
              .setUtterance(utterance)
              .setTargetValue(targetValue)
              .createExample();

          if (applyWeights)
            ex.weight = weights.getOrDefault(type, 1.0);

          addOneExample(ex, maxGroup, group);
        }
      }
    }
  }

  @Override
  public void read() throws IOException {
    LogInfo.begin_track_printAll("ThingpediaDataset.read");

    // assume all examples are train for now

    try (Connection con = dataSource.getConnection()) {
      // we initially train with just the canonical forms
      // this is to "bias" the learner towards learning actions with
      // parameters
      // if we don't do that, with true examples the correct parse
      // always falls off the beam and we don't learn at all
      if (opts.includeCanonical)
        readCanonicals(con);

      readFullExamples(con);
    } catch (SQLException e) {
      throw new IOException(e);
    }

    if (Dataset.opts.splitDevFromTrain)
      splitDevFromTrain();

    collectStats();

    LogInfo.end_track();
  }

  public static void storeExample(String utterance, String targetJson, String languageTag, String type, List<String> schemas) {
    DataSource dataSource = ThingpediaDatabase.getSingleton();
    try (Connection con = dataSource.getConnection()) {
      con.setAutoCommit(false);
      
      int exampleId;
      try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
        stmt.setString(1, type);
        stmt.setString(2, languageTag);
        stmt.setString(3, utterance);
        stmt.setString(4, targetJson);
        
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();

        exampleId = rs.getInt(1);
      }

      try (PreparedStatement stmt2 = con.prepareStatement(INSERT_SCHEMA_REF_QUERY)) {
        for (String schema : schemas) {
          stmt2.setInt(1, exampleId);
          stmt2.setString(2, schema);
          stmt2.executeUpdate();
        }
      }

      con.commit();
    } catch (SQLException e) {
      LogInfo.logs("Failed to store example in the DB: %s", e.getMessage());
    }
  }

  public interface ExampleConsumer {
    public void accept(String utterance, String targetJson);
  }

  public static void getRawExamples(String languageTag, ExampleConsumer consumer) throws IOException {
    DataSource dataSource = ThingpediaDatabase.getSingleton();

    try (Connection con = dataSource.getConnection();
        PreparedStatement stmt = con.prepareStatement(RAW_EXAMPLE_QUERY)) {
      stmt.setString(1, languageTag);
      try (ResultSet set = stmt.executeQuery()) {
        while (set.next()) {
          // 1: id
          // 2: type
          String utterance = set.getString(3);
          String targetJson = set.getString(4);

          consumer.accept(utterance, targetJson);
        }
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
}
