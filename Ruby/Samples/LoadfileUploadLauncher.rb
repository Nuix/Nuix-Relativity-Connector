script_directory = File.dirname(__FILE__)
require File.join(script_directory,"..","RelativityLoadfileUpload","RelativityLoadfileUpload.rb").gsub("/","\\")

RelativityLoadfileUpload.loadfileUpload("10.2",File.join(script_directory,"LoadfileUploadSettings.json").gsub("/","\\"),"C:\\Data\\Export\\loadfile.dat",1018882,1003697,"Secret1234!")