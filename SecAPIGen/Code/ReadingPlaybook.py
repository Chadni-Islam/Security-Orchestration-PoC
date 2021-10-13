# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
#  This file will read the playbooks and extract the details such as task, input and output from it. 
import yaml
import pandas as pd


def extractOutput(outFields, outputList, taskID):
    print('in extract output')
    outputs = pd.DataFrame(columns=outFields)
    outputs_temp = pd.DataFrame(columns=outFields)
    print(outputList)
    i = 0

    for k in outputList:
        # for key, values in v.items():#
        # print('\n in output', k, '==>',i, '\n')
        outputs_temp['id'] = taskID
        outputs_temp['seq'] = i
        for key in k:
            if key in outFields:
                outputs_temp[key] = k[key]
                print(key, '==', outputs_temp[key].values)
        output_frames = [outputs, outputs_temp]
        outputs = pd.concat(output_frames)
    print(outputs.shape)
    return outputs


def extractTask(taskFields, taskList):
    #  print('time to extract tasks')
    #  print(type(taskList))
    task_temp = pd.DataFrame(columns=taskFields)
    task = pd.DataFrame(columns=taskFields)
    # taskFile = open('task.csv', 'w')
    # taskWrite = csv.DictWriter(taskFile, fieldnames=taskFields)
    for k, v in taskList.items():
        #  print('first loop ', k, '=>', v, '\n')
        for key, values in v.items():
            print(key, '=>', values)
            if key in taskFields:
                #  print('yes', key, 'is in the task list')
                task_temp[key] = pd.Series(values)
            else:
                if key == "task":
                    for key2, values2 in values.items():
                        if key2 == 'id':
                            print()
                        elif key2 in taskFields:
                            # print('yes', key2, 'is a feature of the task') # task is another nested structure
                            task_temp[key2] = pd.Series(values2)
        # print('end of for loop in task ')
        # print(task_temp)
        frames = [task, task_temp]
        task = pd.concat(frames)
    #  print(task.shape)
    # print(task)
    # task.to_csv('task.csv')
    return task


def get_data_from_yamlFile(playbook, playbookFields, taskFields, outputFields):
    with open('playbook/' + playbook, 'r', encoding="utf8") as stream:
        # alternatives
        # stream = open("playbook-Access_Investigation_-_Generic.yml", 'r')
        df = pd.DataFrame(columns=playbookFields)
        df_temp = pd.DataFrame(columns=playbookFields)
        output_list = pd.DataFrame(columns=outputFields)
        output_list_temp = pd.DataFrame(columns=outputFields)
        #  print(df.shape)
        task_list_temp = pd.DataFrame(columns=taskFields)
        task = pd.DataFrame(columns=taskFields)
        try:
            docs = yaml.load_all(stream, Loader=yaml.SafeLoader)  # load the file in docs
        #  print('loading successful')
        except yaml.YAMLError as exc:
            print(exc)
        # get section
        # section = args[0]
        # playbookFile = open('incidentPlan.csv', 'w')
        # playbookWriter = csv.DictWriter(playbookFile, fieldnames=playbookFields)

        for doc in docs:
            # print(doc.items(), '\n')
            # print('item')
            for key, value in doc.items():
                # print(key, '=>', value)
                if key in playbookFields:
                    #  print(key, 'is in the playbook list')
                    df[key] = pd.Series(value)
                    if key == "tasks":
                        task_list_temp = extractTask(taskFields, value)
                    elif key == "outputs":
                        output_list_temp = extractOutput(outputFields, value, df['id'])
                        # print('\n taskId = ', df['id'])
                else:
                    # print(key, 'not in playbook list')
                    print()
            print('end of first for loop')
            frames = [task, task_list_temp]
            task = pd.concat(frames)
            output_frames = [output_list, output_list_temp]
            output_list = pd.concat(output_frames)
            df_frames = [df, df_temp]
            df = pd.concat(df_frames)

        df.to_csv('incidentPlan.csv')
        return task, df, output_list


print('finish here')
"""
try:
    # print(yaml.safe_load(stream)) # alternative
    print(yaml.load(stream, Loader=yaml.SafeLoader))
except yaml.YAMLError as exc:
    print(exc)
"""
###  Script to modify YAML file by taking a parameter  ###
