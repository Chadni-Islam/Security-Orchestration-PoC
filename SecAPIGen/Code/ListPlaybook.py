# Author: Chadni Islam, University of Adelaide, Adelaide, SA, Australia
# Documented Date: 13.10.2021
# This file read the playbooks for analysis

import os
import ReadingPlaybook
import pandas as pd


def readingPlaybookDirectory():
    playbookFields = ['id', 'name', 'description', 'tasks', 'starttaskid', 'inputs', 'outputs']
    taskFields = ['id', 'taskid', 'type', 'name', 'description', 'scriptName ', 'tags', 'condition', 'scriptarguments', 'nexttasks', 'conditions']
    outputFields = ['id', 'seq', 'contextPath', 'description', 'type']
    entries = os.listdir('playbook/')
    taskList = pd.DataFrame(columns=taskFields)
    playbookList = pd.DataFrame(columns=playbookFields)
    outputList = pd.DataFrame(columns=outputFields)
    i = 0
    for entry in entries:
       # print(entry)
        i = i+1
        tasklist_temp, playbooklist_temp, output_list_temp = ReadingPlaybook.get_data_from_yamlFile(entry, playbookFields, taskFields,outputFields)
        #  print('================================= ***** ==========================')
        #  print(playbooklist_temp)
        # print('==================**************===================')
        # print(tasklist_temp)
        task_frames = [taskList, tasklist_temp]
        taskList = pd.concat(task_frames)
        df_frames = [playbookList, playbooklist_temp]
        playbookList = pd.concat(df_frames)
        output_frames = [outputList, output_list_temp]
        outputList = pd.concat(output_frames)

    print('Total playbook ', i)
    taskList.to_csv('Task_playbook.csv')
    playbookList.to_csv('Playbook.csv')
    outputList.to_csv('Output.csv')


readingPlaybookDirectory()