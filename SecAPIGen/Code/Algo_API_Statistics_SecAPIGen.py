# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
# This code shows the statics of the API data in terms of wordcout and APi counts 

def statistical_analysis(sentence):
    # https://www.geeksforgeeks.org/finding-mean-median-mode-in-python-without-libraries/
    num_of_words = []
    print('total task', len(sentence))
    print('validation', len(sentence)*0.7)
    print('testing', len(sentence)*0.3)
    for value in sentence:
        # print(len(value))
        values = value.split()
        num_of_words.append(len(values))
    get_sum = sum(num_of_words)
    n = len(num_of_words)
    mean = get_sum/n
    num_of_words.sort()
    if n % 2 == 0:
        median1 = num_of_words[n//2]
        median2 = num_of_words[n//2 - 1]
        median = (median1 + median2) / 2
    else:
        median = num_of_words[n//2]
    print("mean/ average is "+str(mean))
    print("median is " + str(median))
    data = Counter(num_of_words)
    get_mode = dict(data)
    mode = [k for k, v in get_mode.items() if v == max(list(data.values()))]
    if len(mode) == n:
        get_mode = "No mode found"
    else:
        get_mode = "Mode is / are: " + ', '.join(map(str, mode))
    print(get_mode)


if __name__ == '__main__':
    import pandas as pd
    from collections import Counter
    filename = 'annotated_API_Chadni.csv'
    df_content = pd.read_csv(filename, encoding='mac_roman')
    task = df_content['task'].values
    statistical_analysis(task)
    df_content.drop_duplicates()
    api1 = df_content['first_method'].values
    print(len(set(api1)))
    # generate the negative and positive combination of ground truth
    print(Counter(api1))
    api2 = df_content['second_method'].values
    print(len(set(api2)))
    # generate the negative and positive combination of ground truth
    print(Counter(api2))
    api3 = df_content['third_method'].values
    print(len(set(api3)))
    print(Counter(api3))
    # param1 =
    temp_param1 = df_content['param2'].values
    param1 = []
    for item in temp_param1:
        if '.' in item:
            temp = item.split('.')
            param1 = param1 + temp
        else:
            param1.append(item)
    print(len(set(param1)))
    print(Counter(param1))
    temp_param2 = df_content['param3'].values
    param2 = []

    for item in temp_param2:
        if '.' in item:
            temp = item.split('.')
            param2 = param2 + list(temp)
        else:
            param2.append(item)
    print(len(set(param2)))
    print(Counter(param2))
    combined = list(api2) + list(api3) + list(param1) + list(param2)
    print(len(set(combined)))
    print(Counter(combined))
    api3_unique = list(set(api3) - set(api2) - set(param1))
    print('unique api3', len(api3_unique))
    print('unique param2', len(list(set(api2) - set(param1))))
    print('unique param 3', len(list(set(api2) - set(api3) - set(param1))))
