1) In main first two try/catch (line 19 and 45) are reading JSON configuration and JSON response. You can change it to any way that the program will get that JSON
2) In Configuration.java there are some parameters that you asked for in previous iteration, but they are not configurable in curent JSON config. If you need, I can change that.
3) Names for columns in table head are from JSONresponse.fields. but without "_".
4) About PageHeader: you can configure each line of header in NewJsonConfigurationRequest.
4.1) If you keep "field" empty, than "value" centered in cell. For example, I kept it empty for report name.
4.2) "value" should not be null. But you can make it " ". For example, I added green empty cell below report name, using " " "value".
5) Main line 103 - warning message if there is more columns than max in configuration. But report still be created.
