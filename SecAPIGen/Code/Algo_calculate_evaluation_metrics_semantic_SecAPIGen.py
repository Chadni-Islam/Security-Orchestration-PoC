# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
# this code perform the evaluation of SecAPIgen to see the performance variation with Resnik similarity metrics for API2
# https://towardsdatascience.com/precision-vs-recall-386cf9f89488
import pandas as pd


def evaluation_metric(inputfile, filename, score, increment, upper_range, file_initial):
    df_label = pd.read_csv(inputfile)

    evaluation_score_wp = []

    while score < upper_range:
        # print('filename: ', filename, 'score: ', score, 'increment: ', increment, 'upper range: ', upper_range)
        df_predicted_label = pd.read_csv(filename)
        label = df_label['2']
        predicted_label = df_predicted_label['2']
        length = len(label)
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

        precision = round(true_positive / (true_positive + false_positive), 2)
        print('precision ', precision)
        recall = round(true_positive / (true_positive+false_negative), 2)
        print('recall ', recall)
        f1_score = round(2 * (precision * recall) / (precision+recall), 2)
        print('f1_score', f1_score)
        evaluation_score_wp.append([score, precision, recall, f1_score])
        print(score, '\n \n ')
        score = round(score+increment, 1)
        filename = file_initial+str(score)+'.csv'
    return evaluation_score_wp


score_wp = 0.2
filename_wp = 'predicted/api1_generated_label'+str(score_wp)+'.csv'
filename_wp_2 = 'predicted/api2_generated_label'+str(score_wp)+'.csv'
initial = "predicted/api1_generated_label"
initial_2 = "predicted/api2_generated_label"
inputfile_1 = 'ground_truth_api1_label.csv'
inputfile_2 = 'ground_truth_api2_label.csv'
evaluation_metrics_wp1 = evaluation_metric(inputfile_1, filename_wp, score_wp, 0.1, 1, initial)
evaluation_metrics_wp2 = evaluation_metric(inputfile_2, filename_wp_2, score_wp, 0.1, 1, initial_2)

df1 = pd.DataFrame(evaluation_metrics_wp1, columns=['score', 'precision', 'recall', 'f1-score'])
df2 = pd.DataFrame(evaluation_metrics_wp2, columns=['score', 'precision', 'recall', 'f1-score'])
df1.to_csv('predicted/evaluation_score_wp1.csv')
print("evaluation_metrics_wp1", evaluation_metrics_wp1)
df2.to_csv('predicted/evaluation_score_wp2.csv')
print("evaluation_metrics_wp2", evaluation_metrics_wp2)
score_res = 2.0
filename_res = 'predicted/resnik_api1_generated_label'+str(score_res)+'.csv'
initial = 'predicted/resnik_api1_generated_label'
evaluation_metrics_res1 = evaluation_metric(inputfile_1, filename_res, score_res, 1, 10, initial)
print("evaluation_metrics_res1", evaluation_metrics_res1)
df = pd.DataFrame(evaluation_metrics_res1, columns=['score', 'precision', 'recall', 'f1-score'])
df.to_csv('predicted/evaluation_score_res1.csv')

filename_res = 'predicted/resnik_api2_generated_label'+str(score_res)+'.csv'
initial = 'predicted/resnik_api2_generated_label'
evaluation_metrics_res2 = evaluation_metric(inputfile_2, filename_res, score_res, 1, 10, initial)
print("evaluation_metrics_res2 ", evaluation_metrics_res2)
df = pd.DataFrame(evaluation_metrics_res2, columns=['score', 'precision', 'recall', 'f1-score'])
df.to_csv('predicted/evaluation_score_res_2.csv')
