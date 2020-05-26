# MidSoC + Ontology 

# Using MidSoC with the ontology:

– The ontology is designed to run on a seperate network server.
– The ontology is designed to execute commands on an 'execution server' (that has tools such as Splunk installed on it)

1. Edit 'ontology.py' to set the server location using the variable 'sparql_server_url'
2. Edit 'ontology.py' to set the server's private key location using the variable 'SERVER_CERT'
3. Edit 'ontology.py' to set the 'execution server' address using the variable 'SERVER_ADDR'
 
# Using MidSoC with the MISP Classifier:

Put Classifier code folder in home directory, (the path will be  '~/MISP/ ...')

