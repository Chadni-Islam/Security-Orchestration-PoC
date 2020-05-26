import limacharlie
import os
import json
import uuid
from argparse import ArgumentError
from unittest import TestCase
from src.main.python.LimaCharlieConstructor import extract_basename
from src.main.python.LimaCharlieConstructor import is_hex
from src.main.python.LimaCharlieConstructor import get_sensor
from src.main.python.LimaCharlieConstructor import parse_args


# gets the parent directory of a given path for a given level
def get_parent_dir(path, level=1):
    return os.path.normpath(os.path.join(path, *([".."] * level)))


# expects the fileName to be in src/main/resources
def get_json_data(file_name, resources = "test_resources"):
    current_dir = os.path.dirname(__file__)
    file_path = get_parent_dir(current_dir, 2) + "\\test\\resources" \
                                                 "\\cybersecurity2\\"
    if resources is not "test_resources":
        file_path = get_parent_dir(current_dir, 2) + "\\main\\resources\\"

    if not os.path.exists(file_path + file_name):
        return None

    # Concatenate path and given file name
    with open(file_path + file_name) as f:
        data = json.load(f)

    return data


# Test cases for the extract_basename in the LimaCharlieConstructor class
class TestExtractBaseName(TestCase):

    # Test to make sure it can get the base name given a windows absolute path
    def test_extract_basename_windows(self):
        windows_file_path = "C:\Documents\Newsletters\Summer2018.pdf"
        actual = extract_basename(windows_file_path)
        expected = "Summer2018.pdf"
        self.assertEqual(actual, expected)

    # Test to make sure it can get the base name given a linux absolute path
    def test_extract_basename_linux(self):
        linux_file_path = "This/Is/A/Linux/File/Path/To/hello.txt"
        actual = extract_basename(linux_file_path)
        expected = "hello.txt"
        self.assertEqual(actual, expected)

    # Test to make sure it can get the base name from a weird file path
    def test_extract_basename_weird(self):
        weird_file_path = "This/Is\A/weird/file./.path/hello.txt"
        actual = extract_basename(weird_file_path)
        expected = "hello.txt"
        self.assertEqual(actual, expected)


# Test cases for the is_hex in the LimaCharlieConstructor class
class TestIsHex(TestCase):

    # Test to make sure it returns false for alphabets only
    def test_alphabets(self):
        alphabets = "abcdefghijklmnopqrstuvwxyz"
        actual = is_hex(alphabets)
        expected = False
        self.assertEqual(actual, expected)

    # Test to make sure it returns false for alphabet chars > F
    def test_bad_hex(self):
        alphabets = "ABC78DEGHZ"
        actual = is_hex(alphabets)
        expected = False
        self.assertEqual(actual, expected)

    # Test to make sure it returns true for correct hex strings
    def test_is_hex_alphabets(self):
        alphabets = "ABCDE12345"
        actual = is_hex(alphabets)
        expected = True
        self.assertEqual(actual, expected)


# Test cases for the is_hex in the LimaCharlieConstructor class
class TestGetSensor(TestCase):

    # initial class variable setup for the unit test methods
    @classmethod
    def setUpClass(cls):
        cls.registered_sensors = get_json_data("LimaCharlieSensors.json")
        cls.api_oid = get_json_data("LimaCharlieCredentials.json", "not_test")
        cls.manager = limacharlie.Manager(cls.api_oid["OID"],
                                          cls.api_oid["API_KEY"],
                                          is_interactive=True,
                                          inv_id=str(uuid.uuid4()))

    # Test to make sure it get_sensor existing_sensor can be fetched
    def test_registered_sensor(self):
        existing_sensor = self.registered_sensors.values()[0]
        actual = get_sensor(self.manager, existing_sensor)
        expected = None
        self.assertNotEqual(actual, expected)

    # Test to make sure it get_sensor returns None for random sensor ID
    def test_unregistered_sensor(self):
        # some random sensor ID
        random_sensor = "31755ff5-370b-9efd-b94b-2a66f6554d22"
        actual = get_sensor(self.manager, random_sensor)
        expected = None
        self.assertEqual(actual, expected)


# tests arg parser for incorrect and correct arg length
# also tests the return values from arg parser
class TestParseArgs(TestCase):

    # arg parser needs at least 4 arguments
    # these arguments are oid, api key, sensor id, command
    # it also passes if extra args is given i.e. file_path
    def test_acceptable_length(self):
        # incorrect length args
        incorrect1 = ["-o", "123"]
        incorrect2 = ["-o", "123", "-a", "345"]
        incorrect3 = ["-o", "123", "-a", "345", "-s", " abcd-1234"]

        # correct length args
        correct1 = ["-o", "123", "-a", "345", "-s", " abcd-1234",
                    "-c", "file_del"]
        correct2 = ["-o", "123", "-a", "345", "-s", " abcd-1234", "-c",
                    "file_del", "-args", "filename=hack.txt"]

        argv = [incorrect1, incorrect2, incorrect3, correct1, correct2]

        # first three elements are wrong, should raise errors
        for i in range(5):
            if i > 2:
                args = parse_args(argv[i])
                self.assertEqual(5, len(args))
            else:
                try:
                    self.assertRaises(ArgumentError, parse_args(argv[i]))
                except SystemExit as err:
                    self.assertEqual(err.code, 2)

    # test the values in the returned dictionary
    def test_return_values(self):
        argv = ["-o", "123", "-a", "345", "-s", "abcd-1234", "-c", "file_del"]
        args = parse_args(argv);
        self.assertEqual(args.get("organization_id"), "123")
        self.assertEqual(args.get("api_key"), "345")
        self.assertEqual(args.get("sensor_id"), "abcd-1234")
        self.assertEqual(args.get("command"), "file_del")
        self.assertEqual(args.get("arguments"), None)

        # add extra arg file_name and expect to be returned
        argv.extend(["-args", "file_name=hack.txt"])
        args = parse_args(argv);
        self.assertEqual(args.get("arguments"), ['file_name=hack.txt'])
