package edu.stanford.nlp.sempre.thingtalk;

import java.io.IOException;
import java.sql.*;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import edu.stanford.nlp.sempre.*;
import fig.basic.LogInfo;
import fig.basic.Option;

public class ThingpediaDataset extends AbstractDataset {
  public static class Options {
    @Option
    public String languageTag = "en";
    @Option
    public boolean includeIfttt = false;
  }

  public static Options opts = new Options();

  private final ThingpediaDatabase dataSource;

  private static final String CANONICAL_QUERY = "select dscc.canonical,ds.kind,dsc.name,dsc.channel_type,dsc.argnames,dscc.argcanonicals,dsc.types from device_schema_channels dsc, device_schema ds, "
      + " device_schema_channel_canonicals dscc where dsc.schema_id = ds.id and dsc.version = ds.developer_version and "
      + " dscc.schema_id = dsc.schema_id and dscc.version = dsc.version and dscc.name = dsc.name and language = ? "
      + " and canonical is not null and ds.kind_type <> 'primary'";
  private static final String FULL_EXAMPLE_QUERY = "select id, type, utterance, target_json from example_utterances where not is_base and language = ?";

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
          String name = set.getString(3);
          String channelType = set.getString(4);
          List<String> argnames = Json.readValueHard(set.getString(5), typeRef);
          List<String> argcanonicals = Json.readValueHard(set.getString(6), typeRef);
          List<String> argtypes = Json.readValueHard(set.getString(7), typeRef);
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

  private void readFullExamples(Connection con) throws SQLException {
    int trainMaxExamples = getMaxExamplesForGroup("train");
    List<Example> trainExamples = getOrCreateGroup("train");
    int testMaxExamples = getMaxExamplesForGroup("test");
    List<Example> testExamples = getOrCreateGroup("test");

    try (PreparedStatement stmt = con.prepareStatement(FULL_EXAMPLE_QUERY)) {
      stmt.setString(1, opts.languageTag);
      try (ResultSet set = stmt.executeQuery()) {

        while (set.next() && (trainExamples.size() < trainMaxExamples || testExamples.size() < testMaxExamples)) {
          int id = set.getInt(1);
          String type = set.getString(2);
          String utterance = set.getString(3);
          String targetJson = set.getString(4);
          Value targetValue = new StringValue(targetJson);

          if (type.equals("ifttt") && !opts.includeIfttt)
            continue;

          List<Example> group;
          int maxGroup;
          if (type.equals("test")) {
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
}
