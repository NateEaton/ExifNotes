import json
import yaml
import sys
import getopt
import datetime

ROLL_KEY_MAPPING = {
    "Make": "cameraMake",
    "Model": "cameraModel",
    "UserComment": "userComment",
    "Artist": "artist",
    "Copyright": "copyright"
}

NOTE_KEY_MAPPING = {
    "FilmStockMake": "filmStockMake",
    "FilmStockModel": "filmStockModel",
    "FilmStockIso": "filmStockIso",
    "DevelopedDate": "developed",
    "DevelopedLab": "DevelopedLab",
    "DevelopedOrderNo": "DevelopedOrderno",
    "DevelopedRollNo": "DevelopedRollno"
}

FRAME_KEY_MAPPING = {
    "DateTimeOriginal":"date",
    "ShutterSpeedValue": "shutter",
    "ExposureTime": "shutter",
    "ApertureValue": "aperture",
    "FNumber": "aperture",
    "Location": "location",
    "FocalLength": "focalLength",
    "LightSource": "lightSource",
    "ImageDescription": "note"
}


def open_input_file(file_path):
    try:
        file = open(file_path, 'r')
        return file
    except IOError:
        print(f"Error: Failed to open '{file_path}'")
        return None


def escape_quotes(value):
    return value.replace('"', r'\"')


def strip_quotes(value):
    return value.replace('"', r'')

def convert_to_exif_datetime(datetime_string):

  # Parse the date and time string into a datetime object.
  datetime_object = datetime.datetime.strptime(datetime_string, "%Y-%m-%dT%H:%M")

  # Convert the datetime object to a string in the standard format for EXIF dates.
  exif_datetime_string = datetime_object.strftime("%Y:%m:%d %H:%M")

  return exif_datetime_string

def convert_datetime_to_date(date_string):

  # Parse the date and time string into a datetime object.
  datetime_object = datetime.datetime.strptime(date_string, "%Y-%m-%dT%H:%M")

  # Convert the datetime object to a string in the standard format for EXIF dates.
  exif_date_string = datetime_object.strftime("%Y-%m-%d")

  return exif_date_string

def convert_to_exif_dms(dd):

  # Convert the decimal degree coordinate to degrees, minutes, and seconds.
  degrees = int(dd)
  minutes = int((dd - degrees) * 60)
  seconds = ((dd - degrees) * 60 - minutes) * 60

  # Format the degrees, minutes, and seconds into a string.
  formatted_string = "{0} {1} {2:.5f}".format(degrees, minutes, seconds)

  return formatted_string

def flatten_json(data, stop_key):
    result = {}
    for key, value in data.items():
        if key == stop_key:
            break
        if isinstance(value, dict):
            nested = flatten_json(value, stop_key)
            for nested_key, nested_value in nested.items():
                result[key + nested_key.capitalize()] = nested_value
        else:
            result[key] = value
    return result

def parse_note_field(note, roll):
    try:
        yaml_data = yaml.safe_load(note)
        if isinstance(yaml_data, dict):
            flatten_yaml_keys("", yaml_data, roll)
    except yaml.YAMLError as e:
        print(f"Error parsing note as YAML: {e}")

def flatten_yaml_keys(prefix, yaml_data, roll):
    for key, value in yaml_data.items():
        if isinstance(value, dict):
            if prefix:
                nested_key = prefix + key.capitalize()
            else:
                nested_key = key.capitalize()
            flatten_yaml_keys(nested_key, value, roll)
        else:
            if prefix:
                roll[prefix + key.capitalize()] = value
            else:
                roll[key.capitalize()] = value

def create_user_comment(roll):
    text = ""
    for dict_key, json_key in NOTE_KEY_MAPPING.items():
        if json_key in roll:
            value = roll[json_key]
            if value is not None:
                if json_key == 'developed':
                    value = convert_datetime_to_date(value)
                if text != "":
                    text += " | "
                text += f"{dict_key}: {value}"
    if text is not None:
        roll['userComment'] = text

def write_exiftool_cmds(frame, roll, file_extension):
    # Write exiftool commands to stdout
    record = "exiftool"
    # Append each mapped tag from frame data
    for dict_key, json_key in FRAME_KEY_MAPPING.items():
        if json_key in frame:
            value = frame[json_key]
            if value is not None:
                if json_key == 'location':
                    longValue = frame[json_key]['longitude']
                    latValue = frame[json_key]['latitude']
                    if longValue is not None and latValue is not None:
                        if longValue > 0:
                            longRefTag = f' -GPSLongitudeRef="E"'
                        else:
                            longRefTag = f' -GPSLongitudeRef="W"'
                            longValue = longValue * -1
                        longValueStr = convert_to_exif_dms(longValue)
                        longTag = f' -GPSLongitude="{longValueStr}"'
                        if latValue > 0:
                            latRefTag = f' -GPSLatitudeRef="N"'
                        else:
                            latRefTag = f' -GPSLatitudeRef="S"'
                            latValue = longValue * -1
                        latValueStr = convert_to_exif_dms(latValue)
                        latTag = f' -GPSLatitude="{latValueStr}"'
                        record += longTag + longRefTag + latTag + latRefTag
                else:
                    if json_key =='date':
                        value = convert_to_exif_datetime(value)
                    if json_key == 'shutter':
                        value = strip_quotes(value)
                    if isinstance(value, str):
                        value = f'"{value}"'
                    record += f" -{dict_key}={value}"
    # Append each mapped tag from roll data
    for dict_key, json_key in ROLL_KEY_MAPPING.items():
        if json_key in roll:
            value = roll[json_key]
            if value is not None:
                if isinstance(value, str):
                    value = f'"{value}"'
                record += f" -{dict_key}={value}"
    # Append file name pattern with the given file extension
    record += f" *0{frame['count']}.{file_extension}"
    print(record)


def print_help():
    print('Usage: python exifnotes_json_parser.py [OPTIONS] [filename.json]')
    print('Options:')
    print('  -h, --help               Print help text')
    print('  -x, --ext <extension>    Set the file extension to use in exiftool commands (default: tif)')


def main():
    input_file_path = "exifnotes.json"
    file_extension = "tif"
    artist = "me"
    copyright = "©me"

    try:
        opts, args = getopt.getopt(sys.argv[1:], "hx:a:c:", ["help", "ext=", "artist=", "copyright="])
    except getopt.GetoptError:
        print("Invalid command line arguments. Use --help or -h for usage instructions.")
        return

    for opt, arg in opts:
        if opt in ("-h", "--help"):
            print("Usage: python exifnotes_json_parser.py [OPTIONS] [FILENAME.JSON]")
            print("Options:")
            print("  -h, --help                  Print help text")
            print("  -x, --ext=EXTENSION         Set the file extension (jpg, jpeg, tif, tiff)")
            print("  -a, --artist=ARTIST         Set the artist name")
            print("  -a, --copyright=COPYRIGHT   Set the copyright string")
            return
        elif opt in ("-x", "--ext"):
            if arg.lower() in ("jpg", "jpeg", "tif", "tiff"):
                file_extension = arg.lower()
            else:
                print("Invalid file extension. Use --help or -h for usage instructions.")
                return
        elif opt in ("-a", "--artist"):
            artist = arg
        elif opt in ("-c", "--copyright"):
            copyright = arg

    if len(args) > 0:
        input_file_path = args[0]

    if not input_file_path.endswith('.json'):
        print('Invalid input file format. File name should end with ".json".')
        return

    file = open_input_file(input_file_path)
    if not file:
        return

    data = json.load(file)
    file.close()
    if data is None:
        print('Empty input file:', input_file_path)
        return

    # Flatten the JSON data into the "roll" dictionary
    roll = flatten_json(data, 'frames')

    # Parse the "note" field if present
    if 'note' in data:
        parse_note_field(data['note'], roll)

    # Create value for UserComment containing film data forwhich there isn't a standard EXIF tag
    create_user_comment(roll)

    # Add personal data
    roll['artist'] = artist
    roll['copyright'] = copyright

    # Process each frame
    frames = data['frames']
    for frame in frames:
        write_exiftool_cmds(frame, roll, file_extension)
        print()  # Add a blank line between records


if __name__ == '__main__':
    main()
