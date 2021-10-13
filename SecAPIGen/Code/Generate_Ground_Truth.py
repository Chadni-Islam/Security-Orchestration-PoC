# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
# this code will generate the combination of task and API from the ground truth,
# for each task we have the API that can execute that task
# The ground truth or comparison dataset will contain task and api pairs and for each pair will have a value of 1 or 0
# the ground truth contain for each time the API that can execute the task and the APIs that cannot execute the task.

import pandas as pd
from collections import Counter


def generate_task_api_pairs(task1, api, api_method):
    # read task and corresponding API
    # df_content = pd.read_csv(filename1, encoding='mac_roman')
    # task1 = df_content['task'].values
    # api = df_content[api_method].values

    # label = df_content[api_label].values
    # df_content = df_content.set_index('index', inplace=True)
    # list = [[i, j] for i in task1 for j in api]
    data = []
    for i in range(0, len(task1)):
        data.append([task1[i], api[i], 1])
    print(len(data))
    api_unique = list(set(api))
    len_api_list = len(api_unique)
    # t = sorted(task)
    #for t1 in t:
    #   print(t1)
    # print(api_unique)
    for i in range(0, len_api_list):
        for j in range(0, len(task1)):
            if api_unique[i] != api[j]:
                # print(api_unique[i], ' ', print(api[j]))
                data.append([task1[j], api_unique[i], 0])

    print('number of api for: ', api_method, 'is ', len_api_list, '. number of data', len(data))

    # print(sorted(api_unique))
    # statistical_analysis(data, api_unique)
    return data


if __name__ == '__main__':
    # read API details
    filename = 'semantic_API_validation_Chadni.csv'
    df_content = pd.read_csv(filename, encoding='mac_roman')
    df_content.drop_duplicates()
    task = df_content['task'].values
    api1 =df_content['first_method'].values
    api2 = df_content['second_method'].values
    # generate the negative and positive combination of ground truth
    data_api_1 = generate_task_api_pairs(task, api1, 'first')
    data_api_2 = generate_task_api_pairs(task, api2, 'second')
    # data_api_3 = generate_task_api_pairs(task, 'third_method')
    # data_param_api_2 = generate_task_api_pairs(task, 'param2')
    # data_param_api_3 = generate_task_api_pairs(task, 'param3')

    df_api1 = pd.DataFrame(sorted(data_api_1))
    df_api1.to_csv('ground_truth_api1_label.csv')
    df_api2 = pd.DataFrame(sorted(data_api_2))
    # df_api3 = pd.DataFrame(sorted(data_api_3))
    # df_1 = pd.read_csv('api1_label.csv')
    # df_api1.to_csv('ground_truth_api1_label.csv')
    df_api2.to_csv('ground_truth_api2_label.csv')
    # df_api3.to_csv('ground_truth_ap3_label.csv')
    # df_param_api_2 = pd.DataFrame(sorted(data_param_api_2))
    # df_param_api_3 = pd.DataFrame(sorted(data_param_api_3))
    # df_param_api_2.to_csv('ground_truth_param2_label.csv')
    # df_param_api_3.to_csv('ground_truth_param3_label.csv')

