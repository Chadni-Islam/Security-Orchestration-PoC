# Author: Chadni Islam
# This source code evaluate the performance of SecAPIGen
# when wp = 0.5. here we only consider the the similarity metrics wup and
# similarity socre = 0.5


# https://spacy.io/usage/linguistic-features
# https://www.dataquest.io/blog/tutorial-text-classification-in-python-using-spacy/
# https://nlpforhackers.io/complete-guide-to-spacy/
import spacy
import pandas as pd
import algorithm1_secAPIGen as semDApi
import word_similarity as wordnet_similarity
import numpy as np
# for visualization of Entity detection importing displacy from spacy
# https://www.analyticsvidhya.com/blog/2019/02/stanfordnlp-nlp-library-python
# sentence tokenisation Load English tokenizer, tagger, parser, NER and word vectors

from spacy.pipeline import Sentencizer
sentencizer = Sentencizer()
# Create the pipeline 'sentencizer' component
# sbd = nlp.create_pipe('sentencizer')
# Add the component to the pipeline
# nlp.add_pipe(sbd)
nlp = spacy.load("en_core_web_lg")
nlp.add_pipe(sentencizer, first=True)
# print("Number of processors: ", mp.cpu_count())


def label_rest_api_task(task_1, generated_api, temp_list, predicted_data):
    for api in temp_list:
        if api == generated_api:
            predicted_data.append([task_1, generated_api, 1])
        else:
            predicted_data.append([task_1, api, 0])
    return predicted_data


if __name__ == '__main__':
    # read playbook details
    # ground_truth()
    print('reading   ................... ')
    filename = 'groundTruth_true_label.csv'
    df = pd.read_csv(filename)
    task_list = df['task']
    api1_list = df['firstmethod']
    api2_list = df['secondmethod']
    api3_list = df['thirdmethod']
    temp_api_ = set(api1_list)
    api1_list_unique = list(temp_api_)
    i = 0
    predicted_data_api1 = []
    score = 0.3
    while score < 1:
        predicted_data_api1 = []
        for task in task_list:
            # print('generating .......... ')
            data = semDApi.main_api_generate(task)
            # print('\n', task)
            # print(data[0], api1_list[i])
            # print('finding ............... ')
            if data[0] == api1_list[i]:
                # print('case 1 ', data[0])
                predicted_data_api1 = label_rest_api_task(task, data[0], api1_list_unique, predicted_data_api1)
            elif data[0] in api1_list_unique:
                # print('case 2 ', data[0])
                predicted_data_api1 = label_rest_api_task(task, data[0], api1_list_unique, predicted_data_api1)
            else:
                wp_score = wordnet_similarity.find_similarity_wp(data[0], api1_list_unique)

                a = np.array(wp_score)
                # print(a)
                idx = np.argmax(a)
                # print(idx)
                if float(wp_score[idx]) > score:
                    poss_api = api1_list_unique[idx]
                else:
                    poss_api = 'na'
                # print('case 3', api1_list_unique[idx])
                predicted_data_api1 = label_rest_api_task(task, poss_api, api1_list_unique, predicted_data_api1)
        i = i + 1
        # print('total', len(predicted_data_api1))
        df = pd.DataFrame(sorted(predicted_data_api1))
        df.to_csv(('predicted/api1_generated_label'+str(score)+'.csv'))
        score = round(score + 0.1, 1)


