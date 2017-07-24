package edu.stanford.nlp.sempre.ibase;

import edu.stanford.nlp.sempre.StringValue;
import edu.stanford.nlp.sempre.thingtalk.AbstractLexiconFn;
import fig.basic.LispTree;

/**
 * Created by silei on 7/24/17.
 */
public class iBaseLexiconFn extends AbstractLexiconFn<StringValue> {
    @Override
    public void init(LispTree tree) {
        super.init(tree);
        setLexicon(new iBaseLexicon());
    }
}
