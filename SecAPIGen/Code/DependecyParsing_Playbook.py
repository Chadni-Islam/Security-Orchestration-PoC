# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
# Example code to show the dependecy parsign tree

# https://spacy.io/usage/linguistic-features
# https://www.dataquest.io/blog/tutorial-text-classification-in-python-using-spacy/
# https://nlpforhackers.io/complete-guide-to-spacy/
import spacy

import pandas as pd
#for visualization of Entity detection importing displacy from spacy:
from spacy import displacy

from nltk import Tree
# Tokenization
# Multi-word token (MWT) expansion
# Lemmatization
# Parts-of-speech (POS) and morphological feature tagging
# Dependency Parsing
# https://www.analyticsvidhya.com/blog/2019/02/stanfordnlp-nlp-library-python/

df = pd.DataFrame(columns=['TEXT', 'LEMMA', 'POS', 'TAG', 'DEP', 'SHAPE', 'ALPHA', 'STOP'])
nlp = spacy.load('en_core_web_sm')
# doc = nlp(u'scan for malicious file and host')
df['TEXT'].append(list(1,2))

# doc = nlp(u'The source IP address from which the incident originated.')
# doc = nlp(u'I shot an elephant in my sleep')
# spacy.explain('ADP')
df.insert(1,'Text', 1)
# read playbook name
df_playbook = pd.read_csv('Playbook.csv')
df_playbook.head()
playbook_name = df_playbook['name']
playbook_name = playbook_name.dropna()
playbook_name = playbook_name.drop_duplicates()

playbook_name.head()
# documents.head()

df_task = pd.read_csv('Task_playbook.csv')
df_task.head()
playbok_plan = df_task['description']
playbook_plan = playbok_plan.dropna()
playbook_plan = playbook_plan.drop_duplicates()
playbook_plan.head()

# sentence tokenization
# Load English tokenizer, tagger, parser, NER and word vectors
# nlp = English()
import en_core_web_sm
from spacy.pipeline import Sentencizer
sentencizer = Sentencizer()

nlp = spacy.load("en_core_web_sm")

# Create the pipeline 'sentencizer' component
# sbd = nlp.create_pipe('sentencizer')
# Add the component to the pipeline
# nlp.add_pipe(sbd)

nlp.add_pipe(sentencizer, first=True)

sentence_list = []
# text = """When learning data science, you shouldn't get discouraged!
# Challenges and setbacks aren't failures, they're just part of the journey. You've got this!"""
for sentences in playbook_plan.values:
    # print(sentences)
    # print(type(sentences))
    # "nlp" Object is used to create documents with linguistic annotations.
    doc = nlp(sentences)
    # create list of sentence tokens
    for sent in doc.sents:
        sentence_list.append(sent.text)
        #print(len(sentence_list))
print(len(sentence_list))
doc = nlp(u'scan for malicious file and remove host')
for token in doc:
    print("{0}/{1} <--{2}-- {3}/{4}".format(
        token.text, token.pos_, token.dep_, token.head.text, token.head.tag_))
    for token in doc:
        if (token.dep_ == 'ROOT'):
            print(token.text)
        else:
            print(token.head.text, '.', token.text)
    token_list = {}
    for token in doc:
        if (token.dep_ == 'ROOT'):
            print(token.text)
            token_list[token.text] = token.text
        elif token.pos_ == 'DET':
            pass
        else:
            print(token.head.text, '.', token.text)
            token_list[token.head.text] = token.text
    for token in doc:
        if (token.dep_ == 'ROOT'):
            print(token.text)
        else:
            print(token.head.text, '.', token.text)
    token_list = {}
    for token in doc:
        if (token.dep_ == 'ROOT'):
            print(token.text)
            token_list[token.text] = token.text
        elif token.pos_ == 'DET':
            pass
        else:
            print(token.head.text, '.', token.text)
            token_list[token.head.text] = token.text
    print('lemma  pos tag dep  is_alpha is_stop idx ')
    for token in doc:
        print(token.lemma_, token.pos_, token.tag_, token.dep_, token.is_alpha, token.is_stop, token.idx)
    # displacy.render(doc, style="dep", page=True, jupyter=True)
    list_token_tag = [token.tag_ for token in doc]
    print(list_token_tag)

def visialise_dependecy():
    from IPython.core.display import display, HTML
    list_sentence = []
    from pathlib import Path
    # first separate the sentence in the playbok
    def tok_format(tok):
        return "_".join([tok.orth_, tok.tag_, tok.dep_])

    def to_nltk_tree(node):
        if node.n_lefts + node.n_rights > 0:
            return Tree(tok_format(node), [to_nltk_tree(child) for child in node.children])
        else:
            return tok_format(node)

    for sentence in sentence_list:
        doc = nlp(sentence)
        print('\n', sentence, '\n')
        # list_sentence.append("sentence: "+sentence)
        # list_pos = [token.pos_ for token in doc]
        # list_sentence.append('pos '+str(list_pos))
        [to_nltk_tree(sent.root).pretty_print() for sent in doc.sents]
        for token in doc:
            print(token.text, '(', token.dep_, ')', token.head.text, '(', token.head.pos_, ')', 'child: ',
                  [child for child in token.children])
        for token in doc:
            if (token.dep_ == 'ROOT'):
                print('Root: ', token.lemma_)
                child_list = token.children
                child = [t.text for t in child_list]
                print('children ', child)
        # token.text, token.pos_, token.dep_, token.head.text, token.head.tag_
        for chunk in doc.noun_chunks:
            print(chunk.text, " -- >", chunk.root.text, chunk.root.dep_, chunk.root.head.text)
            chunk_root = chunk.root.lemma_
            chunk_root_dep = chunk.root.dep_
            chunk_root_head = chunk.root.head.lemma_
            direct_obj = 'dobj'
            nominal_subj = 'nsubj'
            if chunk_root_dep == direct_obj:
                print(chunk_root_head, '.', chunk_root)
            elif chunk_root_dep == nominal_subj:
                print(chunk_root, '.', chunk_root_head)
            else:
                pass
            # list_sentence.append("noun_chunk: "+chunk.text)
            # list_sentence.append("root text: "+chunk.root.text+', root dependency: '+chunk.root.dep_+', head text:'+chunk.root.head.text)
        displacy.render(doc, style="dep", page=True, jupyter=True)
        # display(HTML(html))
        # svg = displacy.render(doc, style="dep", jupyter=False)
        # file_name = '-'.join([w.text for w in doc if not w.is_punct]) + ".svg"
        # output_path = Path("images/" + file_name)
        # output_path.open("w", encoding="utf-8").write(svg)

        # list_sentence.append('\n')