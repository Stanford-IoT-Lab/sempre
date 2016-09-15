package edu.stanford.nlp.sempre;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import fig.basic.LispTree;

/**
 * Represents the discourse context (time, place, history of exchanges).
 * This is part of an Example and used by ContextFn.
 *
 * @author Percy Liang
 */
public class ContextValue extends Value {
  // A single exchange between the user and the system
  // Note: we are not storing the entire derivation right now.
  public static class Exchange {
    public final String utterance;
    public final Value value;

    public Exchange(String utterance, Value value) {
      this.utterance = utterance;
      this.value = value;
    }
    public Exchange(LispTree tree) {
      utterance = tree.child(1).value;
      value = Values.fromLispTree(tree.child(3));
    }
    public LispTree toLispTree() {
      LispTree tree = LispTree.proto.newList();
      tree.addChild("exchange");
      tree.addChild(utterance);
      tree.addChild(value.toLispTree());
      return tree;
    }
    @Override public String toString() { return toLispTree().toString(); }
  }

  public final String user;
  public final DateValue date;
  public final List<Exchange> exchanges;  // List of recent exchanges with the user

  public ContextValue withDate(DateValue newDate) {
    return new ContextValue(user, newDate, exchanges);
  }

  public ContextValue withNewExchange(List<Exchange> newExchanges) {
    return new ContextValue(user, date, newExchanges);
  }

  public ContextValue(String user, DateValue date, List<Exchange> exchanges) {
    this.user = user;
    this.date = date;
    this.exchanges = exchanges;
  }

  // Example:
  //   (context (user pliang)
  //            (date 2014 4 20)
  //            (exchange "when was chopin born" (!fb:people.person.date_of_birth fb:en.frederic_chopin) (date 1810 2 22))
  //            (graph NaiveKnowledgeGraph ((string Obama) (string "born in") (string Hawaii)) ...))
  public ContextValue(LispTree tree) {
    String user = null;
    DateValue date = null;
    exchanges = new ArrayList<>();
    for (int i = 1; i < tree.children.size(); i++) {
      String key = tree.child(i).child(0).value;
      if (key.equals("user")) {
        user = tree.child(i).child(1).value;
      } else if (key.equals("date")) {
        date = new DateValue(tree.child(i));
      } else if (key.equals("exchange")) {
        exchanges.add(new Exchange(tree.child(i)));
      } else {
        throw new RuntimeException("Invalid: " + tree.child(i));
      }
    }
    this.user = user;
    this.date = date;
  }

  @Override
  public LispTree toLispTree() {
    LispTree tree = LispTree.proto.newList();
    tree.addChild("context");
    if (user != null)
      tree.addChild(LispTree.proto.newList("user", user));
    if (date != null)
      tree.addChild(date.toLispTree());
    for (Exchange e : exchanges)
      tree.addChild(LispTree.proto.newList("exchange", e.toLispTree()));
    return tree;
  }

  @Override
  public Map<String,Object> toJson() {
    Map<String,Object> json = new HashMap<>();
    if(user != null)
      json.put("user", user);
    if(date != null)
      json.put("date", date.toJson());
    List<Object> exchangeJson = new ArrayList<>();
    json.put("exchange", exchangeJson);
    for (Exchange e : exchanges)
      exchangeJson.add(e.toLispTree());
    return json;
  }

  @Override public int hashCode() {
    int hash = 0x7ed55d16;
    hash = hash * 0xd3a2646c + user.hashCode();
    hash = hash * 0xd3a2646c + date.hashCode();
    hash = hash * 0xd3a2646c + exchanges.hashCode();
    return hash;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ContextValue that = (ContextValue) o;
    if (!this.user.equals(that.user)) return false;
    if (!this.date.equals(that.date)) return false;
    if (!this.exchanges.equals(that.exchanges)) return false;
    return true;
  }

  @Override
  @JsonValue
  public String toString() { return toLispTree().toString(); }

  @JsonCreator
  public static ContextValue fromString(String str) {
    return new ContextValue(LispTree.proto.parseFromString(str));
  }
}
