'''
Created on Mar 16, 2017

@author: gcampagn, rakeshr
'''

import tensorflow as tf

from util.model import Model
from util.seq2seq import grammar_decoder_fn_inference
from util.loader import load_dictionary, load_embeddings

from util.seq2seq import SimpleGrammar
from thingtalk.grammar import ThingtalkGrammar

class Config(object):
    max_length = 60
    dropout = 0.7
    #dropout = 1
    embed_size = 300
    hidden_size = 300
    batch_size = 256
    #batch_size = 5
    n_epochs = 40
    lr = 0.001
    train_input_embeddings = False
    train_output_embeddings = False
    output_embed_size = 50
    rnn_cell_type = "lstm"
    rnn_layers = 1
    apply_attention = True
    grammar = None
    l2_regularization = 0.005

def length_limited_softmax(values, lengths, num_decoder, num_encoder):
    '''
    A softmax operation that, for row `i` of the matrix, only considers
    elements up to `lengths[i]`.
    The result has the same shape as `values`, but elements past the
    length are undefined.
    '''
    exp = tf.exp(values)
    #assert exp.get_shape() == values.get_shape()
    
    mask = tf.to_float(tf.sequence_mask(lengths, num_encoder))
    # pack num_decoder copies of the mask
    mask = tf.stack([mask for _ in xrange(num_decoder)], axis=1)
    sum = tf.reduce_sum(exp, axis=2, keep_dims=True)
    return exp/sum

class LSTMAligner(Model):
    def add_placeholders(self):
        # batch size x number of words in the sentence
        self.input_placeholder = tf.placeholder(tf.int32, shape=(None, self.config.max_length))
        self.input_length_placeholder = tf.placeholder(tf.int32, shape=(None,))
        self.output_placeholder = tf.placeholder(tf.int32, shape=(None, self.config.max_length,))
        self.output_length_placeholder = tf.placeholder(tf.int32, shape=(None,))
        self.dropout_placeholder = tf.placeholder(tf.float32, shape=())

    def create_feed_dict(self, inputs_batch, input_length_batch, labels_batch=None, label_length_batch=None, dropout=1):
        feed_dict = dict()
        feed_dict[self.input_placeholder] = inputs_batch
        feed_dict[self.input_length_placeholder] = input_length_batch
        if labels_batch is not None:
            feed_dict[self.output_placeholder] = labels_batch
        if label_length_batch is not None:
            feed_dict[self.output_length_placeholder] = label_length_batch
        feed_dict[self.dropout_placeholder] = dropout
        return feed_dict

    def add_prediction_op(self, training):
        xavier = tf.contrib.layers.xavier_initializer(seed=1234)

        with tf.variable_scope('embed', reuse=not training):
            # first the embed the input
            if self.config.train_input_embeddings:
                input_embed_matrix = tf.get_variable('input_embedding',
                                                     shape=(self.config.dictionary_size, self.config.embed_size),
                                                     initializer=tf.constant_initializer(self.pretrained_embeddings))    
            else:
                input_embed_matrix = tf.constant(self.pretrained_embeddings)

            # dictionary size x embed_size
            assert input_embed_matrix.get_shape() == (self.config.dictionary_size, self.config.embed_size)

            # now embed the output
            if self.config.train_output_embeddings:
                output_embed_matrix = tf.get_variable('output_embedding',
                                                      shape=(self.config.output_size, self.config.output_embed_size),
                                                      initializer=xavier)
            else:
                output_embed_matrix = tf.eye(self.config.output_size)
                
            assert output_embed_matrix.get_shape() == (self.config.output_size, self.config.output_embed_size)

        inputs = tf.nn.embedding_lookup([input_embed_matrix], self.input_placeholder)
        # batch size x max length x embed_size
        assert inputs.get_shape()[1:] == (self.config.max_length, self.config.embed_size)
        
        def make_rnn_cell(id):
            if self.config.rnn_cell_type == "lstm":
                cell = tf.contrib.rnn.LSTMCell(self.config.hidden_size)
            elif self.config.rnn_cell_type == "gru":
                cell = tf.contrib.rnn.GRUCell(self.config.hidden_size)
            elif self.config.rnn_cell_type == "basic-tanh":
                cell = tf.contrib.rnn.BasicRNNCell(self.config.hidden_size)
            else:
                raise ValueError("Invalid RNN Cell type")
            cell = tf.contrib.rnn.DropoutWrapper(cell, output_keep_prob=self.dropout_placeholder, seed=8 + 33 * id)
            return cell
        
        # the encoder
        with tf.variable_scope('RNNEnc', initializer=xavier, reuse=not training) as scope:

            cell_enc = tf.contrib.rnn.MultiRNNCell([make_rnn_cell(id) for id in xrange(self.config.rnn_layers)])
            #cell_enc = tf.contrib.rnn.AttentionCellWrapper(cell_enc, 5, state_is_tuple=True)

            enc_hidden_states, enc_final_state = tf.nn.dynamic_rnn(cell_enc, inputs, sequence_length=self.input_length_placeholder,
                                                                   dtype=tf.float32, scope=scope)
            enc_hidden_states.set_shape((None, self.config.max_length, self.config.hidden_size))
            # assert enc_preds.get_shape()[1:] == (self.config.max_length, self.config.hidden_size)
            # if self.config.input_cell == "lstm":
            #     assert enc_final_state[0][0].get_shape()[1:] == (self.config.hidden_size,)
            #     assert enc_final_state[0][1].get_shape()[1:] == (self.config.hidden_size,)
            # else:
            #     assert enc_final_state.get_shape()[1:] == (self.config.hidden_size,)
        
        # the decoder
        with tf.variable_scope('RNNDec', initializer=xavier, reuse=not training) as scope:
            cell_dec = tf.contrib.rnn.MultiRNNCell([make_rnn_cell(id) for id in xrange(self.config.rnn_layers)])
            
            U = tf.get_variable('U', shape=(self.config.hidden_size, self.config.output_size), initializer=xavier)
            tf.add_to_collection(tf.GraphKeys.WEIGHTS, U)
            if self.config.apply_attention:
                V1 = tf.get_variable('V1', shape=(self.config.hidden_size, self.config.hidden_size), initializer=xavier)
                tf.add_to_collection(tf.GraphKeys.WEIGHTS, V1)
                V2 = tf.get_variable('V2', shape=(self.config.hidden_size, self.config.hidden_size), initializer=xavier)
                tf.add_to_collection(tf.GraphKeys.WEIGHTS, V2)
            b_y = tf.get_variable('b_y', shape=(self.config.output_size,), initializer=tf.constant_initializer(0, tf.float32))
            
            if training:
                go_vector = tf.ones((tf.shape(self.output_placeholder)[0], 1), dtype=tf.int32) * self.config.sos
                output_ids_with_go = tf.concat([go_vector, self.output_placeholder], axis=1)
                outputs = tf.nn.embedding_lookup([output_embed_matrix], output_ids_with_go)
                #assert outputs.get_shape()[1:] == (self.config.max_length+1, self.config.output_size)

                decoder_fn = tf.contrib.seq2seq.simple_decoder_fn_train(enc_final_state)
                dec_hidden_states, dec_final_state, _ = tf.contrib.seq2seq.dynamic_rnn_decoder(cell_dec, decoder_fn,
                    inputs=outputs, sequence_length=self.output_length_placeholder, scope=scope)
                dec_hidden_states.set_shape((None, self.config.max_length, self.config.hidden_size))

                # hidden_dec_final_state = dec_final_state
                # if self.config.output_cell == "lstm":
                #     assert dec_final_state[0].get_shape()[1:] == (self.config.hidden_size,)
                #     assert dec_final_state[1].get_shape()[1:] == (self.config.hidden_size,)
                #     hidden_dec_final_state = dec_final_state[1]
                # else:
                #     assert dec_final_state.get_shape()[1:] == (self.config.hidden_size,)
                #
                # hidden_enc_final_state = enc_final_state
                # if self.config.input_cell == "lstm":
                #     hidden_enc_final_state = enc_final_state[1]

                # Attention mechanism
                if self.config.apply_attention:
                    raw_att_score = tf.matmul(dec_hidden_states, enc_hidden_states, transpose_b=True)
                    assert raw_att_score.get_shape()[1:] == (self.config.max_length, self.config.max_length)
                
                    norm_att_score = length_limited_softmax(raw_att_score, self.input_length_placeholder, self.config.max_length, self.config.max_length)
                    assert norm_att_score.get_shape()[1:] == (self.config.max_length, self.config.max_length)
                
                    context_vectors = tf.matmul(norm_att_score, enc_hidden_states)
                    assert context_vectors.get_shape()[1:] == (self.config.max_length, self.config.hidden_size)

                    dec_preds = tf.tanh(tf.tensordot(dec_hidden_states, V1, [[2], [0]]) +
                                        tf.tensordot(context_vectors, V2, [[2], [0]]))
                    dec_preds.set_shape((None, self.config.max_length, self.config.hidden_size))
                    #assert dec_preds.get_shape()[1:] == (self.config.max_length, self.config.hidden_size)
                else:
                    dec_preds = dec_hidden_states
                    assert dec_preds.get_shape()[1:] == (self.config.max_length, self.config.hidden_size)
                
                preds = tf.tensordot(dec_preds, U, [[2], [0]]) + b_y
                preds.set_shape((None, self.config.max_length, self.config.output_size))
                #assert preds.get_shape()[1:] == (self.config.max_length, self.config.output_size)

            else:
                def output_fn(cell_output, dec_cell_state, batch_size):
                    assert cell_output.get_shape()[1:] == (self.config.hidden_size,)
                    #hidden_final_state = enc_final_state
                    #if self.config.input_cell == "lstm":
                    #    assert enc_final_state[0].get_shape()[1:] == (self.config.hidden_size,)
                    #    assert enc_final_state[1].get_shape()[1:] == (self.config.hidden_size,)
                    #    hidden_final_state = enc_final_state[1]
                    #else:
                    #    assert enc_final_state.get_shape()[1:] == (self.config.hidden_size,)

                    ## Attention mechanism
                    if self.config.apply_attention:
                        raw_att_score = tf.matmul(tf.reshape(cell_output, (batch_size, 1, self.config.hidden_size)), enc_hidden_states, transpose_b=True)
                        assert raw_att_score.get_shape()[1:] == (1, self.config.max_length)
                    
                        norm_att_score = length_limited_softmax(raw_att_score, self.input_length_placeholder, 1, self.config.max_length)
                        norm_att_score.set_shape((None, 1, self.config.max_length))
                        #assert norm_att_score.get_shape()[1:] == (1, self.config.max_length)
                    
                        context_vector = tf.matmul(norm_att_score, enc_hidden_states)
                        assert context_vector.get_shape()[1:] == (1, self.config.hidden_size)
                    
                        #result = tf.matmul(cell_output, U) + tf.matmul(att_context, V) + b_y
                        result = tf.tanh(tf.matmul(cell_output, V1) +
                            tf.matmul(tf.reshape(context_vector, (batch_size, self.config.hidden_size)), V2))
                    else:
                        result = cell_output
                    assert result.get_shape()[1:] == (self.config.hidden_size,)
                    result = tf.matmul(result, U) + b_y
                    assert result.get_shape()[1:] == (self.config.output_size,)
                    return result

                #decoder_fn = tf.contrib.seq2seq.simple_decoder_fn_inference(output_fn, enc_final_state,
                #    output_embed_matrix, self.config.sos, self.config.eos, self.config.max_length-1, self.config.output_size)
                decoder_fn = grammar_decoder_fn_inference(output_fn, enc_final_state, output_embed_matrix,
                                                          self.config.max_length-1, self.config.grammar)
                dec_preds, dec_final_state, _ = tf.contrib.seq2seq.dynamic_rnn_decoder(cell_dec, decoder_fn, scope=scope)

                assert dec_preds.get_shape()[2:] == (self.config.output_size,)
                #if self.config.rnn_cell_type == "lstm":
                #    assert dec_final_state[0].get_shape()[1:] == (self.config.hidden_size,)
                #    assert dec_final_state[1].get_shape()[1:] == (self.config.hidden_size,)
                #else:
                #    assert dec_final_state.get_shape()[1:] == (self.config.hidden_size,)
                preds = dec_preds
            #print preds.get_shape()
            #assert preds.get_shape()[2:] == (self.config.output_size,)

        return preds

    def add_loss_op(self, preds):
        length_diff = tf.reshape(self.config.max_length - tf.shape(preds)[1], shape=(1,))
        padding = tf.reshape(tf.concat([[0, 0, 0], length_diff, [0, 0]], axis=0), shape=(3, 2))
        preds = tf.pad(preds, padding, mode='constant')
        #labels = tf.slice(self.output_placeholder, [0, 0], [-1, self.output_length_placeholder])
        #labels = self.output_placeholder[:,:self.output_length_placeholder]
        labels = self.output_placeholder
        loss = tf.nn.sparse_softmax_cross_entropy_with_logits(logits=preds, labels=labels)
        assert loss.get_shape()[1:] == (self.config.max_length,)
        output_mask = tf.sequence_mask(self.output_length_placeholder, self.config.max_length)
        loss = tf.boolean_mask(loss, output_mask)
        asserts = [tf.Assert(tf.reduce_any(loss > 0), [loss], name='loss_gt_0'),
                   #tf.Assert(tf.shape(preds)[1:] == [self.config.max_length, self.config.output_size], [preds, tf.shape(preds)[1:]], name='shape_of_preds'),
                   tf.Assert(tf.reduce_any(output_mask != False), [output_mask], name='output_mask'),
                   tf.Assert(tf.reduce_all(tf.argmax(preds[:,0,:], axis=1) != self.config.eos), [preds[:,0,:]], name='assert_not_empty')]
        with tf.control_dependencies(asserts):
            loss = tf.reduce_sum(loss)
            assert loss.get_shape() == ()
        
        if self.config.l2_regularization > 0:
            weights = tf.get_collection(tf.GraphKeys.WEIGHTS) + filter(lambda v : v.name.endswith('/weights:0'), tf.trainable_variables())
            loss += tf.contrib.layers.apply_regularization(tf.contrib.layers.l2_regularizer(self.config.l2_regularization), weights)
            
        return loss

    def add_training_op(self, loss):
        #optimizer = tf.train.AdamOptimizer(self.config.lr)
        #optimizer = tf.train.AdagradOptimizer(self.config.lr)
        optimizer = tf.train.RMSPropOptimizer(self.config.lr, decay=0.95)
        train_op = optimizer.minimize(loss)
        return train_op

    def __init__(self, config, pretrained_embeddings):
        self.config = config
        self.pretrained_embeddings = pretrained_embeddings
        self.build()


def initialize(benchmark, input_words, embedding_file):
    config = Config()

    if benchmark == "tt":
        print "Loading ThingTalk Grammar"
        config.grammar = ThingtalkGrammar()
    elif benchmark == "geo":
        print "Loading Geoqueries Grammar"
        config.grammar = SimpleGrammar("geoqueries/output_tokens.txt")
    else:
        raise ValueError("Invalid benchmark %s" % (benchmark,))

    words, reverse = load_dictionary(input_words, benchmark)
    config.dictionary_size = len(words)
    print "%d words in dictionary" % (config.dictionary_size,)
    embeddings_matrix = load_embeddings(embedding_file, words, config)

    config.output_size = config.grammar.output_size
    if not config.train_output_embeddings:
        config.output_embed_size = config.output_size
    print "%d output tokens" % (config.output_size,)
    config.sos = config.grammar.start
    config.eos = config.grammar.end
    
    return config, words, reverse, embeddings_matrix
