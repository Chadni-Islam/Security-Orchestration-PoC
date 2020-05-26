
# Cybersecurity 2

**Project Title:**  MiDSoC: A Middleware for Data Fusion in Information Security Operations Centre

**Project Supervisory Team:** UoA, Data61, Jemena, Datacom TSS

**Collaborators:** Jemena

**Contributors:** Rhys Brailsford, Mohammad Rezai, Joseph Tripodi

## Getting Started

### Overview
MiDSoC is a tool that provides a pipeline between SIEM and EDR tools, allowing for interoperability. Currently, only Splunk Enterprise and LimaCharlie are supported with a limited set of features. These features and their resulting actions include:
- LimaCharlie log data => Splunk Log Management
- LimaCharlie FILE_CREATE detections => Splunk Reporting (harmfulFiles)
- LimaCharlie NEW_PROCESS detections => Splunk Reporting (harmfulProcesses)
- Splunk Report (harmfulFiles) => LimaCharlie file deletion
- Splunk Report (harmfulProcesses) => LimaCharlie Process termination

### Environment
While this is not the only viable environment for running MiDSoC, this is what we recommend as it is what we had the most success on. For initial testing and demonstration, it is **highly recommended** you attempt to replicate this environment. You will need two machines as such:
#### Endpoint
One machine (can be Virtual Machine as well) running Windows 8 or higher. This machine acts as your endpoint and will need the LimaCharlie sensor installed on it (details on this later). It also requires internet access.
#### Splunk Server & MiDSoC
One machine (can also be a VM) running Linux. We had the most success with running Ubuntu 18. Sudo privileges on this machine is assumed and it is expected to have a GUI (so use a cloud-based VM at your own risk) as it will be expected to run IntelliJ IDEA. This machine also requires internet access as this will act as your Splunk server.

### Requirements
Before running MiDSoC, a couple of things need to be sorted. These requirements only act as a checklist of things to prepare and does not include all the necessary details. These details can be found in the corresponding pages provided at the end of each section.
#### Splunk
- Splunk Enterprise v7.1.3 or higher needs to be installed and running on same machine as MiDSoC, ideally running Linux.
- Two Alerts need to be created: `harmfulFiles` and `harmfulProcesses`.
- Login credentials for Splunk server need to be edited into `/src/main/resources/SplunkCredentials.json`.
- [Details on Splunk setup can be found here](https://github.com/serp2018/cybersecurity2/wiki/Splunk-Setup)
#### LimaCharlie
- A LimaCharlie Organisation needs to be created.
- A LimaCharlie sensor needs to be installed on a Windows machine.
- Installation script needs to be run on Splunk/MiDSoC machine to setup SFTP ([can be found here](https://github.com/serp2018/cybersecurity2/issues/166#issuecomment-433292702)).
- Two outputs need to be created: one for Events, one for Detections.
- Two D&R Rules need to be created: one for FILE_CREATE events, one for NEW_PROCESS events.
- API Key needs to be created.
- API Key and OID need to be edited into `/src/main/resources/LimaCharlieCredentials.json`.
- [Details on LimaCharlie setup can be found here](https://github.com/serp2018/cybersecurity2/wiki/LimaCharlie-Setup)
#### MiDSoC
- It is recommended to use IntelliJ IDEA Community 2017.2.1 for building and running MiDSoC in its current state.
- Ensure all dependencies are imported and IntelliJ is using a project SDK of Java 1.8.
- Edit run configurations for MiDSoC class to include `/var/sftp/uploads` and `/opt/splunk/etc/apps/search/lookups` as program arguments 0 and 1 respectively.
- [Details on MiDSoC setup can be found here](https://github.com/serp2018/cybersecurity2/wiki/MiDSoC-Setup)

### Running MiDSoC
Once Splunk and LimaCharlie have been correctly configured, and MiDSoC is successfully built, it can be run. To successfully demonstrate it working, simply create a file called `hack.txt` and open `mspaint.exe` on the Windows machine with the LC sensor installed. After a short period, the file should be deleted and paint should be closed automatically. **When creating hack.txt, use notepad or some other text editor to save/create the file**. This is important because if a new file is created using other means, it may be created with the name `New Document` or something similar, resulting in Splunk not detecting it as a harmful file.
