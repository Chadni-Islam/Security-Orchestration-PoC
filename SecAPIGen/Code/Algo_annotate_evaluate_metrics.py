# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
# this code calcualte the evaluation metrics to show the performance of generation of declarative APIs

# https://towardsdatascience.com/precision-vs-recall-386cf9f89488
import pandas as pd


def evaluation_metric(ground_truth_file, auto_annotate_file, element):
    df_label = pd.read_csv(ground_truth_file)
    # print('filename: ', filename, 'score: ', score, 'increment: ', increment, 'upper range: ', upper_range)
    df_generated_label = pd.read_csv(auto_annotate_file)
    label = df_label['2'].values
    predicted_label = df_generated_label['2'].values
    length = len(label)
    print(length)
    print(len(predicted_label))
    true_positive = true_negative = false_negative = false_positive = 0
    for i in range(0, length):
        # print(i, label[i], predicted_label[i])
        if label[i] == predicted_label[i] == 1:
            true_positive += 1
        elif label[i] == 1 and predicted_label[i] == 0:
            false_negative += 1
        elif label[i] == 0 and predicted_label[i] == 1:
            false_positive += 1
        elif label[i] == 0 and predicted_label[i] == 0:
            true_negative += 1
    print(element)
    precision = round(true_positive / (true_positive + false_positive), 2)
    print('precision ', precision)
    recall = round(true_positive / (true_positive+false_negative), 2)
    print('recall ', recall)
    f1_score = round(2 * (precision * recall) / (precision+recall), 2)
    print('f1_score', f1_score)
    return element, precision, recall, f1_score


evaluation_score = []
generated_file = 'predicted/generated/generate_01_'
original_file = 'predicted/generated/original_01_'

original_file_1 = original_file + 'api1.csv'
generated_file_1 = generated_file + 'api1.csv'
evaluation_metrics_1 = evaluation_metric(original_file_1, generated_file_1, 'first part')
evaluation_score.append(evaluation_metrics_1)

original_file_2 = original_file + 'api2.csv'
generated_file_2 = generated_file + 'api2.csv'
evaluation_score.append(evaluation_metric(original_file_2, generated_file_2,'second part'))

original_file_3 = original_file + 'api3.csv'
generated_file_3 = generated_file + 'api3.csv'
evaluation_score.append((evaluation_metric(original_file_3, generated_file_3, 'third part')))

original_file_param2 = original_file + 'param2.csv'
generated_file_param2 = generated_file + 'param2.csv'
evaluation_score.append((evaluation_metric(original_file_param2, generated_file_param2, 'param_Second_part')))

original_file_param3 = original_file + 'param3.csv'
generated_file_param3 = generated_file + 'param3.csv'
evaluation_score.append(evaluation_metric(original_file_param3, generated_file_param3, 'param_third_part'))

df = pd.DataFrame(evaluation_score, columns=['element', 'precision', 'recall', 'f1-score'])
# df = pd.DataFrame(evaluation_metrics_2, columns=['api2', 'precision', 'recall', 'f1-score'])
df.to_csv('predicted/generated/evaluation_score.csv')
