# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
# This algorithm takes an new_api and a list of APIs and check the new API is semantically
# similar or related with any API in the list.
# this has used wordnet to identify the similarity between two words.
# nltk.download has used to download all the packages and text corpora

import nltk
from nltk.corpus import wordnet
# nltk.download()
from nltk.corpus import wordnet_ic
import numpy as np
# API_list[1] = list['quarantine', 'remove', 'enrich', 'confine', detach']
brown_ic = wordnet_ic.ic('ic-brown.dat')

# semcor_ic = wordnet_ic.ic('ic-semcor.dat')
# brown_ic = wordnet_ic.ic('ic-semcor.dat')


def find_similarity(new_api, api_list):
    path_score = []
    wp_score = []
    lch_score = []
    res_score = []
    jcn_score = []
    lin_score = []
    for api in api_list:
        path_similar = []
        wup_similar = []
        lch_similar = []
        res_similar = []
        jcn_similar = []
        lin_similar = []
        syn1 = wordnet.synsets(new_api)
        syn2 = wordnet.synsets(api)
        # print(syn1, syn2)
        # print('\n', new_api, api)
        for s1 in syn1:
            for s2 in syn2:
                if s1.pos() == s2.pos() == 'v':
                    path_similar.append(s1.path_similarity(s2))
                    wup_similar.append(s1.wup_similarity(s2))
                    lch_similar.append(s1.lch_similarity(s2))
                    res_similar.append(s1.res_similarity(s2, brown_ic))
                    jcn_similar.append(s1.jcn_similarity(s2, brown_ic))
                    lin_similar.append(s1.lin_similarity(s2, brown_ic))
        if len(path_similar) > 0:
            path_score.append("{0:.2f}".format(max(path_similar)))
        else:
            path_score.append(0)
        if len(wup_similar) > 0:
            wp_score.append("{0:.2f}".format(max(wup_similar)))
        else:
            wp_score.append(0)
        if len(lch_similar) > 0:
            lch_score.append("{0:.2f}".format(max(lch_similar)))
        else:
            lch_score.append(0)
        if len(res_similar) > 0:
            res_score.append("{0:.2f}".format(max(res_similar)))
        else:
            res_score.append(0)
        if len(jcn_similar) > 0:
            jcn_score.append("{0:.2f}".format(max(jcn_similar)))
        else:
            jcn_score.append(0)
        if len(lin_similar) > 0:
            lin_score.append("{0:.2f}".format(max(lin_similar)))
        else:
            lin_score.append(0)

    return path_score, wp_score, lch_score, res_score, jcn_score, lin_score


api_list1 = ['block', 'verify', 'host', 'retrieve', 'check', 'enrich']
new_api1 = 'hunt'
confuse = ''
# statistic()
print(api_list1)
path_score1, wp_score1, lch_score1, res_score1, jcn_score1, lin_score1 = find_similarity(new_api1, api_list1)
print('path_score: ', path_score1)
print('wp_score: ', wp_score1)
# print('lch_score: ', lch_score1)
print('res_score: ', res_score1)
# print('jcn_score: ', jcn_score1)
# print('lin_score: ', lin_score1)
a = np.array(wp_score1)
value = 0.0
idx = 0
for i in range(0, len(a)):
    if value <= float(a[i]):
        value = float(a[i])
        idx = i
print(api_list1[idx])
print(wp_score1[idx])

idx = np.argmax(a)
print(float(wp_score1[idx]))
if float(wp_score1[idx]) > 0.4:
    poss_api = api_list1[idx]
else:
    poss_api = 'na'

print('possible api of ', new_api1, "' with WP_score is :", poss_api)

a = np.array(res_score1)
idx = np.argmax(a)
print(float(res_score1[idx]))
if float(res_score1[idx]) > 2:
    poss_api = api_list1[idx]
else:
    poss_api = 'na'

print('possible api of ', new_api1, " is :", poss_api)

if float(res_score1[idx]) > 3:
    poss_api = api_list1[idx]
else:
    poss_api = 'na'

print('possible api of ', new_api1, " is :", poss_api)