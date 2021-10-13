# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
# this code perform the evaluation/ testin of SecAPIGen
# https://spacy.io/usage/linguistic-features
# https://www.dataquest.io/blog/tutorial-text-classification-in-python-using-spacy/
# https://nlpforhackers.io/complete-guide-to-spacy/
import spacy
import pandas as pd
import Algo1_algo2_DecOr as semDApi
import word_similarity as wordnet_similarity
from nltk.corpus import wordnet_ic
from spacy.pipeline import Sentencizer

brown_ic = wordnet_ic.ic('ic-brown.dat')
# brown_ic = wordnet_ic.ic('ic-semcor.dat')
import numpy as np
#for visualization of Entity detection importing displacy from spacy
# https://www.analyticsvidhya.com/blog/2019/02/stanfordnlp-nlp-library-python
# sentence tokenisation Load English tokenizer, tagger, parser, NER and word vectors
sentencizer = Sentencizer()
# Create the pipeline 'sentencizer' component
# sbd = nlp.create_pipe('sentencizer')
# Add the component to the pipeline
# nlp.add_pipe(sbd)
nlp = spacy.load("en_core_web_lg")
nlp.add_pipe(sentencizer, first=True)
# print("Number of processors: ", mp.cpu_count())
import time
def generate_01_task_api():
    filename = 'TestSet_SecAPIGen.csv'
    df_content = pd.read_csv(filename, encoding='mac_roman')
    df_content.drop_duplicates()
    task = df_content['task'].values
    api1 = df_content['first_method'].values
    api2 = df_content['second_method'].values
    # generate the negative and positive combination of ground truth
    data_api_1 = generate_task_api_pairs(task, api1, 'first')
    data_api_2 = generate_task_api_pairs(task, api2, 'second')
    # data_api_3 = generate_task_api_pairs(task, 'third_method')
    # data_param_api_2 = generate_task_api_pairs(task, 'param2')
    # data_param_api_3 = generate_task_api_pairs(task, 'param3')
    df_api1 = pd.DataFrame(sorted(data_api_1))
    df_api1.to_csv('ground_truth_testing_api1_label.csv')
    df_api2 = pd.DataFrame(sorted(data_api_2))
    # df_api3 = pd.DataFrame(sorted(data_api_3))
    # df_1 = pd.read_csv('api1_label.csv')
    # df_api1.to_csv('ground_truth_api1_label.csv')
    df_api2.to_csv('ground_truth_testing_api2_label.csv')
    # df_api3.to_csv('ground_truth_ap3_label.csv')
    # df_param_api_2 = pd.DataFrame(sorted(data_param_api_2))
    # df_param_api_3 = pd.DataFrame(sorted(data_param_api_3))
    # df_param_api_2.to_csv('ground_truth_param2_label.csv')
    # df_param_api_3.to_csv('ground_truth_param3_label.csv')


def generate_task_api_pairs(task1, api, api_method):
    # read task and corresponding API
    # df_content = pd.read_csv(filename1, encoding='mac_roman')
    # task1 = df_content['task'].values
    # api = df_content[api_method].values

    # label = df_content[api_label].values
    # df_content = df_content.set_index('index', inplace=True)
    # list = [[i, j] for i in task1 for j in api]
    data = []
    for ii in range(0, len(task1)):
        data.append([task1[ii], api[ii], 1])
    print(len(data))
    api_unique = list(set(api))
    len_api_list = len(api_unique)
    # t = sorted(task)
    #for t1 in t:
    #   print(t1)
    # print(api_unique)
    for ii in range(0, len_api_list):
        for j in range(0, len(task1)):
            if api_unique[ii] != api[j]:
                # print(api_unique[i], ' ', print(api[j]))
                data.append([task1[j], api_unique[ii], 0])

    print('number of api for: ', api_method, 'is ', len_api_list, '. number of data', len(data))
    # print(sorted(api_unique))
    # statistical_analysis(data, api_unique)
    return data


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
    generate_01_task_api()
    print('reading   ................... ')
    filename = 'TestSet_SecAPIGen.csv'
    df = pd.read_csv(filename)
    task_list = df['task']
    api1_list = df['first_method']
    api2_list = df['second_method']
    api1_list_unique = sorted(list(set(api1_list)))
    api2_list_unique = list(set(api2_list))
    # print(api1_list_unique)
    predicted_data_api1 = []
    predicted_data_api2 = []
    predicted_data_api2_res = []
    score = 0.5
    score_res = 4
    i = 0
    print('generating .......... ')
    processing_time = []
    processing_wordnet_time = []
    for task in task_list:
        start_time = time.time()
        data = semDApi.main_api_generate(task)
        stop_time = time.time() - start_time
        # print(stop_time)
        wordnet_time = 0
        # print('\n', task)
        #print('generated API: ', data)
        #print(api1_list[i])
        # print('finding ............... ')
        if data[1] == api1_list[i]:
            # print('case 1 ', data[0])
            predicted_data_api1 = label_rest_api_task(task, data[1], api1_list_unique, predicted_data_api1)
        elif data[1] in api1_list_unique:
            # print('case 2 ', data[0])
            predicted_data_api1 = label_rest_api_task(task, data[1], api1_list_unique, predicted_data_api1)
        else:
            wordnet_start_time = time.time()
            poss_api = semDApi.find_similar_api(api1_list_unique, data[1], 0.4)
            wp_score = wordnet_similarity.find_similar_wp(data[1], api1_list_unique)
            # a = np.array(wp_score)
            # print(a)
            # idx = np.argmax(a)
            # value = 0
            # idx = 0
            # print(a)
            # for i in range(0, len(a)):
              #  if value <= float(a[i]):
               #     value = float(a[i])
                #    idx = i
                    # print(data[1], float(wp_score[idx]), api1_list_unique[idx])
            # print(idx)
            # idx = np.argmax(a)
           # if float(wp_score[idx]) >= score:
            #    poss_api = api1_list_unique[idx]
            #else:
             #   poss_api = 'na'
            wordnet_time = time.time() - wordnet_start_time

            processing_time.append(stop_time + wordnet_time)
            processing_wordnet_time.append(wordnet_time)
            predicted_data_api1 = label_rest_api_task(task, poss_api, api1_list_unique, predicted_data_api1)

        if data[2] == api2_list[i]:
            # print('case 1 ', data[0])
            predicted_data_api2 = label_rest_api_task(task, data[2], api2_list_unique, predicted_data_api2)
        elif data[2] in api2_list_unique:
            # print('case 2 ', data[0])
            predicted_data_api2 = label_rest_api_task(task, data[2], api2_list_unique, predicted_data_api2)
        else:
            wordnet_start_time = time.time()
            poss_api = semDApi.find_similar_api(api2_list_unique, data[2], 0.4)

        if data[2] == api2_list[i]:
            # print('case 1 ', data[0])
            predicted_data_api2_res = label_rest_api_task(task, data[2], api2_list_unique, predicted_data_api2_res)
        elif data[2] in api2_list_unique:
            # print('case 2 ', data[0])
            predicted_data_api2_res = label_rest_api_task(task, data[2], api2_list_unique, predicted_data_api2_res)
        else:
            predicted_data_api2 = label_rest_api_task(task, poss_api, api2_list_unique, predicted_data_api2)
            poss_api = semDApi.find_similar_api_res(api2_list_unique, data[2], 3)
            wordnet_time = time.time() - wordnet_start_time
            # print(wordnet_time)
            processing_time.append(stop_time + wordnet_time)
            processing_wordnet_time.append(wordnet_time)
            predicted_data_api2_res = label_rest_api_task(task, poss_api, api2_list_unique, predicted_data_api2_res)
        i = i + 1
    df1 = pd.DataFrame(sorted(predicted_data_api1))
    df2 = pd.DataFrame(sorted(predicted_data_api2))
    df3 = pd.DataFrame(sorted(predicted_data_api2_res))
    #print(len(df1))
    #print(len(df2))
    #print(len(df3))
    df1.to_csv('predicted/generated/testing_api1_generated_label.csv')

    df2.to_csv('predicted/generated/testing_api2_generated_label.csv')

    df3.to_csv('predicted/generated/testing_api2_res_generated_label.csv')

    print("average time --- %s seconds ---" % ((sum(processing_time) / len(processing_time))*1000))
    print('max_time ', round(max(processing_time), 2), 'min_time ', min(processing_time))

    print("average wordnet time --- %s seconds ---" % ((sum(processing_wordnet_time) / len(processing_wordnet_time))*1000))
    print('max_time ', round(max(processing_wordnet_time), 2), 'min_time ', min(processing_wordnet_time))
