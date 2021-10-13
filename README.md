# Security-Orchestration-PoC
A proof of concept implementation of security orchestration, automation and response platform.

**Project Title:**  AI enabled design and generation of Declarative APIs for security Orchestration

**Project Team:** Chadni Islam, M. Ali Babar, and Surya Nepal

# SecAPIGen

- Contains the code for automated generation of Declarative APIs
- Contains the code for evaluation of the performance of SecAPIGen and DecCOR in terms of effectiveness and efficiency
- The dataset folder of SecAPIGen contains the raw the of playbooks, the tasks description and the groundtruth with the annovation instruction to evaluate the performance of SecAPIGen. 

#SOAR implementation - Using orchestrator with the ontology

# SOAR + Ontology 

# Using orchestrator with the ontology:

– The ontology is designed to run on a seperate network server.
– The ontology is designed to execute commands on an 'execution server' (that has tools such as Splunk installed on it)

1. Edit 'ontology.py' to set the server location using the variable 'sparql_server_url'
2. Edit 'ontology.py' to set the server's private key location using the variable 'SERVER_CERT'
3. Edit 'ontology.py' to set the 'execution server' address using the variable 'SERVER_ADDR'
 
# Using orchestrator with the security tools for example MISP:
(the path will be  '~/MISP/ ...')
Put any new automation code folder in home directory 



# Baseline Implementation -  Static Integration of security tool in a SOAR


## Getting Started

### Overview
Security orchestration, automation and response platform (SOAR) platform provides a pipeline between security tools,  allowing for interoperability. 
In this experiment, Splunk Enterprise and LimaCharlie are supported with a limited set of features. These features and their resulting actions include:
- LimaCharlie log data => Splunk Log Management
- LimaCharlie FILE_CREATE detections => Splunk Reporting (harmfulFiles)
- LimaCharlie NEW_PROCESS detections => Splunk Reporting (harmfulProcesses)
- Splunk Report (harmfulFiles) => LimaCharlie file deletion
- Splunk Report (harmfulProcesses) => LimaCharlie Process termination

### Environment
While this is not the only viable environment for running a SOAR platform, this is what we recommend based on the most success on experiment. For initial testing and demonstration, it is ** recommended** to replicate this environment. 
We have used two machines:
#### Endpoint
One machine (can be Virtual Machine as well) running Windows 8 or higher. This machine acts as an endpoint and will need the LimaCharlie sensor installed on it (details below). It also requires internet access.
#### Splunk Server & SOAR
One machine (can also be a VM) running Linux. We had the most success with running Ubuntu 18. Sudo privileges on this machine is assumed and it is expected to have a GUI (so did not use a cloud-based VM) as it will be expected to run IntelliJ IDEA. This machine also requires internet access as this will act as Splunk server.

### Requirements
Before running SOAR platform, a couple of things need to be sorted. These requirements only act as a checklist of things to prepare and does not include all the necessary details. These details can be found in the corresponding pages provided at the end of each section.
#### Splunk
- Splunk Enterprise v7.1.3 or higher needs is installed and running on same machine as SOAR, ideally running Linux.
The orchestration process for two alerts `harmfulFiles` and `harmfulProcesses` are as follow:
- Login credentials for Splunk server need to be edited into `/src/main/resources/SplunkCredentials.json`.
- [Details on Splunk setup can be found here](https://github.com/Chadni-Islam/Security-Orchestration-PoC/wiki/Splunk-Setup)
#### LimaCharlie
- A LimaCharlie Organisation is created.
- A LimaCharlie sensor needs is installed on a Windows machine.
- Installation script run on Splunk/SOAR machine to setup SFTP ([can be found here](https://github.com/Chadni-Islam/Security-Orchestration-PoC/wiki/issues/166#issuecomment-433292702)).
- Two outputs are created: one for Events, one for Detections.
- Two D&R Rules are created: one for FILE_CREATE events, one for NEW_PROCESS events.
- API Key needs are created.
- API Key and OID need are edited into `/src/main/resources/LimaCharlieCredentials.json`.
- [Details on LimaCharlie setup can be found here](https://github.com/Chadni-Islam/Security-Orchestration-PoC/wiki/LimaCharlie-Setup)
#### SOAR
- We use IntelliJ IDEA Community 2017.2.1 for building and running SOAR in its current state.
- Ensure all dependencies are imported and IntelliJ is using a project SDK of Java 1.8.
- Edit run configurations for SOAR class to include `/var/sftp/uploads` and `/opt/splunk/etc/apps/search/lookups` as program arguments 0 and 1 respectively.
- [Details on SOAR setup can be found here](https://github.com/Chadni-Islam/Security-Orchestration-PoC/wiki/SOAR_Setup)

### Running SOAR
Once Splunk and LimaCharlie have been correctly configured, and SOAR is successfully built, it can be run. To successfully demonstrate it working, simply create a file called `hack.txt` and open `mspaint.exe` on the Windows machine with the LC sensor installed. After a short period, the file should be deleted and paint should be closed automatically. **When creating hack.txt, use notepad or some other text editor to save/create the file**. This is important because if a new file is created using other means, it may be created with the name `New Document` or something similar, resulting in Splunk not detecting it as a harmful file.
