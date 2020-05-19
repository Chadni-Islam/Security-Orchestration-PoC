import sys
import json
import uuid
import argparse
import re
import binascii
import string

# if LimaCharlie package is not installed, exit immediately
try:
    import limacharlie
except ImportError, e:
    sys.stdout.write("unable to import LimaCharlie package.")
    sys.exit(0)


# given an absolute path, extracts the file name
def extract_basename(path):
    """Extracts basename of a given path. Should Work with any OS Path on any
    OS """
    basename = re.search(r'[^\\/]+(?=[\\/]?$)', path)
    if basename:
        return basename.group(0)


# used for get_file method, as it returns hex of non .txt files
# if the content of file is hex then returns true
def is_hex(s):
    hex_digits = set(string.hexdigits)
    # if s is long, then it is faster to check against a set
    return all(c in hex_digits for c in s)


# WORKING COMMAND, always use doubles slashes for FILE_PATH for WINDOWS, FILE_
# PATH
# sensor.task('file_del "C:\\Users\\cybersecurity\\Documents\\tack.rtf"',
# inv_id="splunk_inv_test")
def get_sensor(manager, sensor_id):
    sensors = manager.sensors()
    for s in sensors:
        if s.sid == sensor_id:
            return s
    return None


# Takes in SID and filePath which needs to be deleted
def delete_file(manager, sensor_id, file_path):
    sensor = get_sensor(manager, sensor_id)
    if sensor is not None:
        # file paths have to be encloses in " " for LC commands
        file_path = '"' + file_path + '"'
        sensor.task('file_del ' + file_path)
        print "Successfully deleted"
    else:
        print "Sensor ID does not exist."
        sys.exit(0)


# Send kill_process command to LimaCharlie
# Kills process with process id pid on sensor with sensor_id
def kill_process(manager, sensor_id, pid):
    # Ensure sensor is online before sending command
    sensor = get_sensor(manager, sensor_id)
    if sensor is not None:
        sensor.task("os_kill_process -p " + pid)
        print "Successfully killed"
    else:
        print "Sensor ID does not exist."
        sys.exit(0)


# Get processes running on sensor
# Writes processes to stdout
def get_processes(manager, sensor_id):
    # Ensure sensor is online before sending command
    sensor = get_sensor(manager, sensor_id)

    if sensor is not None:
        try:
            future = sensor.request('os_processes')
        except limacharlie.utils.LcApiException as exp:
            print 'Failed to connect to host'
            print exp
            sys.exit(0)
        else:
            responses = future.getNewResponses(timeout=10)
            if len(responses) == 0:
                print "Never got response"
            else:
                for res in responses:
                    res_json = json.loads(json.dumps(res))

                    # Extract:
                    # Process Id
                    # Command Line
                    # Parent Process Id
                    for p in res_json.get('event').get('PROCESSES'):
                        process = {"PROCESS_ID": p.get("PROCESS_ID"),
                                   "PARENT_PROCESS_ID": p.get(
                                           "PARENT_PROCESS_ID"),
                                   "COMMAND_LINE": p.get("COMMAND_LINE")}
                        print process
    else:
        print "Sensor ID does not exist."
        sys.exit(0)


# Gets file information for a given file path
# Prints the file information to stdout
def get_file_info(manager, sensor_id, file_path):
    # Ensure sensor is online before sending command
    sensor = get_sensor(manager, sensor_id)

    # file paths have to be encloses in " " for LC commands
    file_path = '"' + file_path + '"'

    if sensor is not None:
        # Using an interactive manager, futures and requests
        try:
            future = sensor.request('file_info ' + file_path)
        except limacharlie.utils.LcApiException as exp:
            print 'Failed to connect to host'
            print exp
            sys.exit(0)
        else:
            # Ask for response with 10s timeout
            responses = future.getNewResponses(timeout=10)
            if len(responses) == 0:
                print "Never got response"
            else:
                for res in responses:
                    # We only need to print the event hash since this contains
                    # all the file information, skipkeys=True will not print
                    # the event key and just the file information
                    res_json = json.loads(json.dumps(res))
                    print json.dumps(res_json.get('event'), skipkeys=True)
    else:
        print "Sensor ID does not exist."
        sys.exit(0)


# file_path: absolute file path on endpoint
# output_file_path: absolute directory path where fetched file is saved
def get_file(manager, sensor_id, file_path, output_file_path):
    # Ensure sensor is online before sending command
    sensor = get_sensor(manager, sensor_id)

    # file paths have to be encloses in " " for LC commands
    file_path = '"' + file_path + '"'

    output_file = extract_basename(file_path)
    # [:-1] for excluding the qoute "
    output_file = output_file_path + output_file[:-1]

    if sensor is not None:
        # Using an interactive manager, futures and requests
        try:
            future = sensor.request('file_get ' + file_path)
        except limacharlie.utils.LcApiException as exp:
            print 'Failed to connect to host'
            print exp
            sys.exit(0)
        else:
            responses = future.getNewResponses(timeout=10)
            if len(responses) == 0:
                print "Never got response"
            else:
                for res in responses:
                    res_json = json.loads(json.dumps(res))
                    result = json.loads(json.dumps(res_json.get('event'),
                                                   skipkeys=True))

                    # if content of file is hex, then write using unhexify
                    if is_hex(result["FILE_CONTENT"]):
                        with open(output_file, "wb") as out:
                            out.write(
                                binascii.unhexlify(result["FILE_CONTENT"]))
                    else:
                        with open(output_file, "w") as out:
                            out.write(result["FILE_CONTENT"])
    else:
        print "Sensor ID does not exist."
        sys.exit(0)


# Tells the sensor to stop all network connectivity on the host
# It's network isolation, except LC comms to the backend
def isolate_node(manager, sensor_id):
    # Ensure sensor is online before sending command
    sensor = get_sensor(manager, sensor_id)

    if sensor is not None:
        try:
            future = sensor.request('segregate_network')
        except limacharlie.utils.LcApiException as exp:
            print 'Failed to connect to host'
            print exp
            sys.exit(0)
        else:
            responses = future.getNewResponses(timeout=10)
            if len(responses) == 0:
                print "Never got response"
            else:
                for res in responses:
                    res_json = json.loads(json.dumps(res))
                    result = json.loads(json.dumps(res_json.get('event'),
                                                   skipkeys=True))
                    # if u'ERROR is 0, successfully isolated
                    if result.get("ERROR") is 0:
                        print "True"
                    else:
                        print "False"
    else:
        print "Sensor ID does not exist."
        sys.exit(0)


# Tells the sensor to allow network connectivity again (after it was segregated)
def rejoin_node(manager, sensor_id):
    # Ensure sensor is online before sending command
    sensor = get_sensor(manager, sensor_id)

    if sensor is not None:
        try:
            future = sensor.request('rejoin_network')
        except limacharlie.utils.LcApiException as exp:
            print 'Failed to connect to host'
            print exp
            sys.exit(0)
        else:
            responses = future.getNewResponses(timeout=10)
            if len(responses) == 0:
                print "Never got response"
            else:
                for res in responses:
                    res_json = json.loads(json.dumps(res))
                    result = json.loads(json.dumps(res_json.get('event'),
                                                   skipkeys=True))
                    # if u'ERROR is 0, successfully isolated
                    if result.get("ERROR") is 0:
                        print "True"
                    else:
                        print "False"
    else:
        print "Sensor ID does not exist."
        sys.exit(0)


# parses command line arguments and return a dictionary
# uses argparser flags and their values to store in dic
def parse_args(args):
    usage1 = "\n1:- LimaCharlieCommands.py [-h] -o <OID> -a <API_KEY> -s <SID>" \
             " -c <COMMAND> -args < file_path=<PATH> pid=<PID> output_file_path" \
             "=<PATH> . . . >"

    usage2 = "\n2:- LimaCharlieCommands.py --show_commands"
    parser = argparse.ArgumentParser(description="LimaCharlie Commands API",
                                     usage=usage1 + usage2)

    parser.add_argument('-o', '--organization_id', type=str, metavar='',
                        required=True, help='Organization ID')

    parser.add_argument('-a', '--api_key', type=str, metavar='',
                        required=True, help='API Key')

    parser.add_argument('-s', '--sensor_id', type=str, metavar='',
                        required=True, help='Sensor ID')

    parser.add_argument('-c', '--command', type=str, metavar='',
                        required=True, help='Command To Send')

    parser.add_argument('-args', '--arguments', type=str, metavar='',
                        nargs='+', help='Command Line Arguments')
    return_args = parser.parse_args(args)

    return vars(return_args)


# parse COMMAND arguments into a dictionary
def get_parameters(args_list):
    ret = {}
    for x in args_list:
        key, value = x.split("=")
        ret[key] = value
    return ret


# Driver method
# Assumes arguments are as follows:
# argv[1] = OID (Organisation ID)
# argv[2] = API_KEY
# argv[3] = SENSOR_ID, id of sensor to send command to
# argv[4] = COMMAND_TYPE, type of command to send
# argv[5] = COMMAND_ARG, any argument needed for command

if __name__ == "__main__":

    required_args = None
    extra_args = None

    # list all the commands this script can perform
    if "--show_commands" in sys.argv:
        print "file_del \n"
        print "file_info \n"
        print "os_kill_process \n"
        print "os_processes \n"
    else:
        required_args = parse_args(sys.argv[1:])

    # put arguments for the commands in a dictionary
    if required_args["arguments"] is not None:
        extra_args = get_parameters(required_args["arguments"])

    # Extract arguments
    OID = required_args["organization_id"]
    API_KEY = required_args["api_key"]
    SENSOR_ID = required_args["sensor_id"]
    COMMAND_TYPE = required_args["command"]

    # Set up LimaCharlie Manager
    man = limacharlie.Manager(OID, API_KEY, is_interactive=True,
                              inv_id=str(uuid.uuid4()))

    # check if sensor is online first, then continue
    limacharlie_sensor = get_sensor(man, SENSOR_ID)
    if limacharlie_sensor is None or limacharlie_sensor.isOnline() is False:
        sys.stdout.write("sensor is offline")
        sys.exit(0)

    # Based on Command type, execute command
    if COMMAND_TYPE == "file_del":
        if extra_args is None or "file_path" not in extra_args:
            print "file_del command requires a file path"
            sys.exit(0)
        delete_file(man, SENSOR_ID, extra_args["file_path"])
    elif COMMAND_TYPE == "os_kill_process":
        if extra_args is None or "pid" not in extra_args:
            print "os_kill_process command requires a pid"
            sys.exit(0)
        kill_process(man, SENSOR_ID, extra_args["pid"])
    elif COMMAND_TYPE == "file_info":
        if extra_args is None or "file_path" not in extra_args:
            print "file_info command requires a file path"
            sys.exit(0)
        get_file_info(man, SENSOR_ID, extra_args["file_path"])
    elif COMMAND_TYPE == "file_get":
        if extra_args is None or "file_path" not in extra_args \
                or "output_file_path" not in extra_args:
            print "file_info command requires a file_path and output_file_path"
            sys.exit(0)
        get_file(man, SENSOR_ID, extra_args["file_path"],
                 extra_args["output_file_path"])
    elif COMMAND_TYPE == "os_processes":
        get_processes(man, SENSOR_ID)
    elif COMMAND_TYPE == "segregate_network":
        isolate_node(man, SENSOR_ID)
    elif COMMAND_TYPE == "rejoin_network":
        rejoin_node(man, SENSOR_ID)
