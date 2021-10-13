# https://towardsdatascience.com/precision-vs-recall-386cf9f89488
import pandas as pd


def evaluation_metric(inputfile, filename):
    df_label = pd.read_csv(inputfile)
    # print('filename: ', filename, 'score: ', score, 'increment: ', increment, 'upper range: ', upper_range)
    df_predicted_label = pd.read_csv(filename)
    label = df_label['2']
    predicted_label = df_predicted_label['2']
    length = len(label)
    print('groundTruth', length)
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

    precision_ = round(true_positive / (true_positive + false_positive), 2)
    print('precision ', precision_)
    recall_ = round(true_positive / (true_positive+false_negative), 2)
    print('recall ', recall_)
    f1_score_ = round(2 * (precision_ * recall_) / (precision_+recall_), 2)
    print('f1_score', f1_score_)

    return precision_, recall_, f1_score_


evaluation_score = []
filename_wp1 = 'predicted/generated/testing_api1_generated_label.csv'
filename_wp2 = 'predicted/generated/testing_api2_generated_label.csv'
filename_res2 = 'predicted/generated/testing_api2_res_generated_label.csv'
inputfile_1 = 'ground_truth_testing_api1_label.csv'
inputfile_2 = 'ground_truth_testing_api2_label.csv'

precision, recall, f1_score = evaluation_metric(inputfile_1, filename_wp1)
evaluation_score.append(['firstpart', precision, recall, f1_score])
precision, recall, f1_score = evaluation_metric(inputfile_2, filename_wp2)
evaluation_score.append(['secondpart_wp', precision, recall, f1_score])
precision, recall, f1_score = evaluation_metric(inputfile_2, filename_res2)
evaluation_score.append(['secondpart_res', precision, recall, f1_score])
df1 = pd.DataFrame(evaluation_score, columns=['type', 'precision', 'recall', 'f1-score'])
df1.to_csv('predicted/generated/evaluation_testing_score.csv')
print("evaluation_metrics ", evaluation_score)

