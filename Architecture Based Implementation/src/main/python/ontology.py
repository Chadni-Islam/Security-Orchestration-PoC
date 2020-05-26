# import html
import urllib.parse
import subprocess
import sys
import argparse
import os

# private key to login to the ontology server & the execution server
SERVER_CERT = "~/Desktop/Ontology/miow2.pem"
# address of server for executing LimaCharlie commands (OSX is causing problems for this)
SERVER_ADDR = "ubuntu@10.33.184.82"
# address of ontology server
sparql_server_url = "http://10.33.184.85:8000/sparql/"

argstring = ""
for arg in sys.argv:
    argstring += " [" + arg + "]"
print("argstring = " + argstring)

parser = argparse.ArgumentParser(
        description="dynamically generates code using queries to ontology SPARQL server"
    )

parser.add_argument('--recursive',     help="Run report command after each command",   default = "True")
parser.add_argument('--non_funcs',     help="Non-Functional requirements",   default = "")
parser.add_argument('--function',      help="Function to call",   default = "KillProcess")
parser.add_argument('--pid',           help="Process ID number",  default = "123")
parser.add_argument('--sensorId',      help="Sensor ID string",   default = "example_sensor_id")
parser.add_argument('--triggerActions',help="Process ID number",  default = "example_triggeraction")
parser.add_argument('--searchName',    help="Name to search for", default = "example_searchname")
parser.add_argument('--fileName',      help="File name",          default = "example_filename")
parser.add_argument('--rule',          help="Rule",               default = "example_rule")
parser.add_argument('--sinkhole',      help="boolean",            default = "false")
parser.add_argument('--toolFrom',      help="Tool from ...",      default = "example_tool")
parser.add_argument('--format',        help="data format",        default = "example_format")
parser.add_argument('--nodeIdentifier',help="Node ID string",     default = "example_node_id")
parser.add_argument('--dependent',     help="dependent variable to correlate against",     default = "example_dependent")
parser.add_argument('--independents',  help="list of independent variables to determine correlation coefficient",     default = "example_independent")
parser.add_argument('--filePath',      help="Path to file",       default = "example/file/path")

# Classifier args
parser.add_argument('--inputFilePath', help="Path to input file", default = "~/MISP-Classifier/MISP-Classifier/example_input.csv")
parser.add_argument('--outputFilePath',help="Path to output file",default = "~/MISP-Classifier/MISP-Classifier/output/asn_owner_country_attrdate_comment_netRatio_payRatio_extRatio_artRatio_miscRatio_blacklist-rid-threat_actor.sav example_input.csv")
parser.add_argument('--algorithm',     help="Classifier algo",    default = "rid")
parser.add_argument('--label',         help="feature label",      default = "tools")
parser.add_argument('--features',      help="column headings",    default = "asn owner country attr_date comment netRatio payRatio extRatio artRatio miscRatio blacklist")

# Parse the arguments
arguments = parser.parse_args()

testinputs = {
	"pid":            arguments.pid,
	"sensorId":       arguments.sensorId,
	"triggerActions": arguments.triggerActions, # boolean type
	"searchName":     arguments.searchName,
	"fileName":       arguments.fileName,
	"rule":           arguments.rule,
	"filePath":       arguments.filePath,
	"sinkhole":       arguments.sinkhole,
	"toolFrom":       arguments.toolFrom,
	"format":         arguments.format,
	"nodeIdentifier": arguments.nodeIdentifier,
	"dependent":      arguments.dependent,
	"independents":   arguments.independents,

	# Classifiers
	"inputFilePath":  arguments.inputFilePath,
	"outputFilePath": arguments.outputFilePath,
	"algorithm":   	  arguments.algorithm,
	"label":  		  arguments.label,
	"features":   	  arguments.features,
}

BLUE   = '\033[94m'
GREEN  = '\033[92m'
ORANGE = '\033[93m'
RED    = '\033[91m'
ENDC   = '\033[0m'

errorString = "NULL"

# replace this part of the query from the file with 'purpose'
replace_tool = "KillProcess"
# replace this part of the query with the variable specified
replace_var = "Pid"

# for adding and stripping from ontology queries
uriprefix = "http://webprotege.stanford.edu/"

def parseResults(output):
	resultarray = []
	for result in parseXML(output):
		if len(result.strip()) > 0:
			print("[" + GREEN + result + ENDC + "]")

			# if "<literal datatype" in result:
			if "~/" in result:
				resultarray.append(result.split("<literal>")[1].split("</literal>")[0]) #.split(">")[-1])
			elif "<literal>" in result: # can be '<literal datatype...'
				resultarray.append(result.split("<literal>")[1].split("</literal>")[0].split("/")[-1]) #.split(">")[-1])
			elif "<literal datatype" in result:
				resultarray.append(result.split("<literal ")[1].split("</literal>")[0].split("/")[-1].split(">")[-1])
			else:
				resultarray.append(result.split("<uri>")[1].split("</uri>")[0].split("/")[-1]) #.split(">")[-1])

	return resultarray

# returns results ARRAY
def parseXML(xml):
	print(xml)
	return xml.split("<results>")[1].split("</results>")[0].strip().split("<result>")

def urlencode(str):
	return urllib.parse.quote(str)
def urldecode(str):
	return urllib.parse.unquote(str)

fp_query_tool              = open(os.path.join(sys.path[0], "query_tool.txt"))
fp_query_nonfunc           = open(os.path.join(sys.path[0], "query_nonfunc.txt"))
fp_query_syntax_command    = open(os.path.join(sys.path[0], "query_syntax_command.txt"))

query_tool              = fp_query_tool.read().strip()
query_nonfunc           = fp_query_nonfunc.read().strip()
query_syntax_command    = fp_query_syntax_command.read().strip()

# query_tool: what tool can delete the file given these env conditions 
# e.g the file is on a Linux box?
# input: tool purpose eg. 'killprocess'
# Output: name/s of tools that can do that function
def func_query_tool(functional_req):
	query_tool2 = query_tool.replace(replace_tool, functional_req)
	print(BLUE + query_tool2 + ENDC)
	output = subprocess.getoutput("curl '" + sparql_server_url + "' --data 'query=" + query_tool2 + "'")
	return parseResults(output)

# query_nonfunc: does the tool meeet the nonfunctional requirements?
# output: list of tools that meet the nonfunctional requirements
def func_query_nonfunc(functional_req, security_tools, nonfunctional_reqs):
	
	nonfunctional_reqs_array = nonfunctional_reqs.split(",")

	working_tools = [] # tools that meet the nonfuncs
	broken_tools  = [] # tools that NOT meet the nonfuncs
	
	for tool in security_tools:
		working = True
		query_nonfunc2 = query_nonfunc.replace(replace_tool, functional_req)
		print(BLUE + query_nonfunc2 + ENDC)
		output = subprocess.getoutput("curl '" + sparql_server_url + "' --data 'query=" + query_nonfunc2 + "'")
		
		nonfuncs_required = parseResults(output)

		print("nonfuncs_for_tool [" + ORANGE + tool + ENDC + "]= ", ORANGE, nonfuncs_required, ENDC)

		if working:
			working_tools.append(tool)
		else:
			broken_tools.append(tool)

	return working_tools, broken_tools


# input: var name
# output: var type (e.g. so you can generate 'kill_process(123)' vs 'kill_process("123") etc')
def func_query_param(param_name):
	query_syntax_vartype2 = query_syntax_vartype.replace(replace_var, param_name)
	print(BLUE + query_syntax_vartype2 + ENDC)

	output = subprocess.getoutput("curl '" + sparql_server_url + "' --data 'query=" + query_syntax_vartype2 + "'")
	return parseResults(output)[0] # no var can have multiple types


def func_query_syntax(functional_req, securityTool):

	query_syntax_command2    = query_syntax_command.replace(replace_tool, functional_req)
	output = subprocess.getoutput("curl '" + sparql_server_url + "' --data 'query=" + query_syntax_command2 + "'")

	print(BLUE + query_syntax_command2 + ENDC)

	command = parseResults(output)[0]
	return command




# the part of the query is replaced with 'purpose'
tool_array = func_query_tool(arguments.function)

print(ORANGE, tool_array, ENDC)

# for each tool, check if it meets the nonfunctional requirements
# test: 'IsolateNode' does not 'workonlinux' but 'KillProcess' does
non_funcs = ""
if len(sys.argv) < 2:
	print(RED, "Usage: <security tool>")
	sys.exit(1)
if len(sys.argv) == 3:
	non_funcs = arguments.non_funcs
working_tools, broken_tools = func_query_nonfunc(arguments.function, tool_array, non_funcs)

print("broken tools = ",  ORANGE, broken_tools,  ENDC)
print("working tools = ", ORANGE, working_tools, ENDC)

# can all the tools be integrated into a single python
# API in the style of the LimaCharlie api?

# test: 'LogManagement' has exceptions, 'killprocess' does not
command = func_query_syntax(arguments.function, tool_array[0])

print("command = ["          + ORANGE, command,           ENDC, "]")

# replace function
newcommand = command
for word in [command for command in command.split() if command.startswith('$')]:
	cleanword = word.strip("$").replace('"', '')
	print("cleanword = [" + GREEN + cleanword + ENDC + "]")
	newcommand = newcommand.replace("$" + cleanword,'"' + testinputs[cleanword] + '"')

print(ORANGE, newcommand, ENDC)

print("@@@@@@@@@ Output from execution server @@@@@@@@@@")
# hardcoding ip is not important, BUT TODO  move key to java project:
os.system("ssh  -i " + SERVER_CERT + " " + SERVER_ADDR + " '" + newcommand + "'")

### Also query the ontology server to submit a report of the above process to
### each SIEM tool

# recursively call this script, just once!
# if arguments.recursive == "True":
# 	os.system("")


