# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# this algorithm takes input a text and generate the declarative APIs leveraging dependency parsing
# https://spacy.io/usage/linguistic-features
# https://www.dataquest.io/blog/tutorial-text-classification-in-python-using-spacy/
# https://nlpforhackers.io/complete-guide-to-spacy/
import spacy
#for visualization of Entity detection importing displacy from spacy
from spacy import displacy
from nltk.corpus import wordnet_ic

brown_ic = wordnet_ic.ic('ic-brown.dat')
# brown_ic = wordnet_ic.ic('ic-semcor.dat')
# https://www.analyticsvidhya.com/blog/2019/02/stanfordnlp-nlp-library-python
# sentence tokenisation Load English tokenizer, tagger, parser, NER and word vectors
# nlp = English()
# import en_core_web_sm
# import en_core_web_lg
from spacy.pipeline import Sentencizer
from nltk.corpus import wordnet  # for identifying semantically similar API
import string

# Create the pipeline 'sentencizer' component
# sbd = nlp.create_pipe('sentencizer')
# Add the component to the pipeline
# nlp.add_pipe(sbd)
sentencizer = Sentencizer()
nlp = spacy.load("en_core_web_lg")
nlp.add_pipe(sentencizer, first=True)

# dependency
root = 'root'
direct_obj = 'dobj'
nominal_subj = 'nsubj'
prep = 'prep'
clausal_complement = ['ccomp', 'advcl']
compound = 'compound'
list_dep = ['compound', 'pobj', root, nominal_subj, direct_obj]
# preposition
verb = 'VERB'
noun = 'NOUN'
root = 'ROOT'
adp = 'ADP'
verb_noun = [verb, noun, 'PRON', 'AUX', 'PROPN']
verb_noun_adj = [verb, noun, 'PRON', 'AUX', 'ADJ', 'PROPN']


def calculate_api_param(token, token_comp):
    # token_comp = token.lemma_
    for child in token.children:
        if child.dep_ == 'compound':
            new_str = str(child.lemma_ + '.' + token.lemma_)
            old_str = str(token.lemma_)
            token_comp = token_comp.replace(old_str, new_str)
            # print('compound:', token_comp)
            for token1 in child.children:
                if token1.dep_ == 'compound':
                    token_comp = calculate_api_param(child, token_comp)
    return token_comp


def calculate_compound(token, token_comp):
    # token_comp = token.lemma_
    for child in token.children:
        if child.dep_ in ['compound', 'amod', 'npadvmod']:
            # token_comp = str(child.lemma_ + '.' + token_comp)
            new_str = str(child.lemma_ + '.' + token.lemma_)
            token_comp = token_comp.replace(str(token.lemma_), new_str)
            # print('compound:', token_comp)
            for token1 in child.children:
                if token1.dep_ in ['compound', 'amod', 'npadvmod']:
                    token_comp = calculate_compound(child, token_comp)
    return token_comp


# identify the elements of an API - the task that need to be done
def generate_api(doc):
    first_api = second_api = third_api = first_api_compound = second_api_compound = third_api_compound = ''
    for token in doc:
        f = s = t = v = 0.00
        token_dep = token.dep_
        token_pos = token.pos_
        child = [i for i in token.children]
        # print('text ', token.lemma_, 'token_dep: ', token_dep, 'token_pos: ', token_pos)
        # rules for identifying first API
        if token_dep == root and token_pos in verb_noun_adj:  # rule 1
            first_api = token.lemma_
            # print('rule 1: ', first_api)
            f = 1
            for child in token.children:
                if child.dep_ == compound and child.pos_ in verb_noun:  # identify the compound of root
                    first_api = child.lemma_
                    first_api_compound = calculate_compound(child, child.lemma_)
                    first_api_compound = first_api_compound.replace(child.lemma_, "")
                    second_api = token.lemma_
                    second_api_compound = calculate_compound(token, token.lemma_)
                    second_api_compound = second_api_compound.replace(token.lemma_, "")
                    # print('rule 2.1: ', first_api, '.', second_api)
                    # print('first_api_param', first_api_compound, ' second_api_param ', second_api_compound)
                    f = s = 2.1
                elif child.dep_ in ['nsubj', 'amod'] and child.pos_ in [verb_noun_adj]:
                    # identify the subject of root
                    children_child = [i for i in child.children]
                    if len(children_child) == 0:
                        first_api = child.lemma_
                        first_api_compound = calculate_compound(child, child.lemma_)
                        first_api_compound = first_api_compound.replace(child.lemma_, "")
                        second_api = token.lemma_
                        second_api_compound = calculate_compound(token, token.lemma_)
                        second_api_compound = second_api_compound.replace(token.lemma_, "")
                        # print('rule 2.2: ', first_api, '.', second_api)
                        # print('first_api_param', first_api_compound, ' second_api_param ', second_api_compound)
                        f = s = 2.2
                elif child.dep_ == 'dep' and child.pos_ in verb_noun:  # identify the subject of root
                    children_child = [i for i in child.children]
                    if len(children_child) == 0:
                        third_api = second_api
                        third_api_compound = second_api_compound
                        second_api = first_api
                        second_api_compound = first_api_compound
                        first_api = child.lemma_
                        first_api_compound = calculate_compound(child, child.lemma_)
                        first_api_compound = first_api_compound.replace(child.lemma_, "")
                        # print('rule 2.3: ', first_api, '.', second_api)
                        # print('first_api_param', first_api_compound, ' second_api_param ', second_api_compound,
                        #      'third_api_param ', third_api_compound)
                        f = s = 2.3
                elif child.dep_ in clausal_complement and child.pos_ == verb:
                    for token1 in child.children:
                        if token1.dep_ == nominal_subj:
                            second_api = token1.lemma_
                            second_api_compound = calculate_compound(token1, token1.lemma_)
                            second_api_compound = second_api_compound.replace(token1.lemma_, "")
                            # print('rule 2.5: ', first_api, '. ', second_api)
                            # print('second_api_param', second_api_compound)
                            f = s = 2.5
                        elif token1.dep_ == direct_obj:
                            third_api = token1.lemma_
                            third_api_compound = calculate_compound(token1, token1.lemma_)
                            third_api_compound = third_api_compound.replace(token1.lemma_, "")
                            # print('rule 3.5: ', first_api, '. ', second_api, '. ', third_api)
                            # print('third_api_param', third_api_compound)
                            t = 3.5
                elif child.dep_ == direct_obj and child.pos_ in verb_noun:
                    if s == 0:
                        second_api = child.lemma_
                        second_api_compound = calculate_compound(child, child.lemma_)
                        second_api_compound = second_api_compound.replace(child.lemma_, "")
                        # print('rule 2.4: ', first_api, '. ', second_api)  # rule 2
                        # print('second_api_param', second_api_compound)
                        s = 2.4
                        for token1 in child.children:
                            if token1.dep_ == 'prep' or 'adative' and token1.pos_ == 'ADP':
                                for token2 in token1.children:
                                    third_api = token2.lemma_
                                    t = 3.41
                                    # print('rule 3.1: ', first_api, '. ', second_api, '.', third_api)
                                    third_api_compound = calculate_compound(token2, token2.lemma_)
                                    third_api_compound = third_api_compound.replace(token2.lemma_, "")
                                    # print('third_api_param', third_api_compound)
                    else:
                        third_api = child.lemma_
                        third_api_compound = calculate_compound(child, child.lemma_)
                        third_api_compound = third_api_compound.replace(child.lemma_, "")
                        # print('rule 3.4: ', first_api, '. ', second_api, '.', third_api)  # rule 2
                        # print('third_api_param', third_api_compound)
                        t = 3.4
                elif child.dep_ == 'prep' or 'adative' and child.pos_ == 'ADP':
                    for token1 in child.children:
                        if token1.dep_ == 'pobj' and token1.pos_ in verb_noun:
                            if s == 0:
                                second_api = token1.lemma_
                                second_api_compound = calculate_compound(token1, token1.lemma_)
                                second_api_compound = second_api_compound.replace(token1.lemma_, "")
                                # print('rule 2.7: ', first_api, '. ', second_api)
                                s = 2.7
                                # print('second_api_param', second_api_compound)
                            else:
                                third_api = token1.lemma_
                                t = 3.1
                                # print('rule 3.1: ', first_api, '. ', second_api, '.', third_api)
                                third_api_compound = calculate_compound(token1, token1.lemma_)
                                third_api_compound = third_api_compound.replace(token1.lemma_, "")
                                # print('third_api_param', third_api_compound)
                elif child.dep_ in ['pobj', 'advcl'] and child.pos_ in ['ADJ', 'NOUN', 'PRON']:
                    if t == 0:
                        third_api = child.lemma_
                        # print('pobject', third_param)
                        t = 3.6
                        # print('rule 3.6: ', first_api, '. ', second_api, '.', third_api)
                        third_api_compound = calculate_compound(child, child.lemma_)
                        third_api_compound = third_api_compound.replace(child.lemma_, "")
                        # print('third_api_param', third_api_compound)

    return first_api, second_api, third_api, second_api_compound, third_api_compound


# https://www.geeksforgeeks.org/nlp-synsets-for-a-word-in-wordnet/
# find_similar_api identify semantically similar APIs find_similar_API
# This algorithm takes an new_api and list of API and check which whether the API is semantically
# similar or related with any API in the list.
# this has used wordnet to identify the similarity between two words.
def find_similar_api(api_list, new_api, sim_score):
    found = 0
    max_similarity_score = 0
    # print('finding...... ')
    for api in api_list:
        # print(api, new_api)
        synsets_api = wordnet.synsets(api)
        # print(synsets_api, '\n')
        synsets_new_api = wordnet.synsets(new_api)
        # print(synsets_new_api, '\n')
        similar_api = []
        for s1 in synsets_api:
            for s2 in synsets_new_api:
                # print(s1.name(), s2.name())
                if s1.pos() == s2.pos():
                    similarity = (wordnet.wup_similarity(s1, s2) or 0)
                # print(similarity)
                    if similarity > sim_score:
                        found = 1
                       # print(new_api, ' similar to API: ', api, 'with score: ', similarity)
                        # print(similarity, s1.name(), s2.name(), '\n')
                        similar_api.append(api)
                        if similarity > max_similarity_score:
                            max_similarity_score = similarity
                            max_similar_api = api
                            # print('max simiar API ', max_similar_api)
    if found == 1:
        # print('final: ', new_api, 'similar: ', max_similarity_score, max_similar_api)
        return max_similar_api
    else:
        # print('None')
        return 'na'


def find_similar_api_res(api_list, new_api, sim_score):
    found = 0
    max_similarity_score = 0
    # print('finding...... ')
    for api in api_list:
        # print(api, new_api)
        synsets_api = wordnet.synsets(api)
        # print(synsets_api, '\n')
        synsets_new_api = wordnet.synsets(new_api)
        # print(synsets_new_api, '\n')
        similar_api = []
        for s1 in synsets_api:
            for s2 in synsets_new_api:
                # print(s1.name(), s2.name())
                if s1.pos() == s2.pos() == 'v':
                    similarity = (wordnet.res_similarity(s1, s2, brown_ic) or 0)
               # print(similarity)
                    if similarity > sim_score:
                        found = 1
                       # print(new_api, ' similar to API: ', api, 'with score: ', similarity)
                        # print(similarity, s1.name(), s2.name(), '\n')
                        similar_api.append(api)
                        if similarity > max_similarity_score:
                            max_similarity_score = similarity
                            max_similar_api = api
                            # print('max simiar API ', max_similar_api)

    if found == 1:
        # print('final: ', new_api, 'similar: ', max_similarity_score, max_similar_api)
        return max_similar_api
    else:
        # print('None')
        return 'na'


def main_api_generate(sentence1):
    table = str.maketrans(dict.fromkeys(string.punctuation))  # OR {key: None for key in string.punctuation}
    sentence = sentence1.translate(table)
    # print(sentence)
    doc = nlp(sentence)
    first_api = second_api = third_api = first_api_feature = second_api_feature = third_api_feature = ""
    first_api, second_api, third_api, second_api_param, third_api_param = generate_api(doc)
    # print(first_api, second_api, third_api)

    second_api_param = second_api_param.translate(table)
    third_api_param = third_api_param.translate(table)
    if first_api == "":
        first_api = 'na'
    if second_api == "":
        second_api = 'na'
    if third_api == "" or third_api == float('nan'):
        third_api = 'na'
    if second_api_param == "":
        second_api_param = 'na'
    if third_api_param == "":
        third_api_param = 'na'
    return sentence, first_api, second_api, third_api, second_api_param,  third_api_param
    # generate_api_param(doc, first_api, second_api, third_api)


# main_api_generate("verify the source user account associated email address.")
