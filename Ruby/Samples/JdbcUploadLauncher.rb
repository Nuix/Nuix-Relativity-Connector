script_directory = File.dirname(__FILE__)
require File.join(script_directory, "..", "RelativityJdbcUpload", "RelativityJdbcUpload.rb").gsub("/","\\")

case_location = "C:\\Cases\\Case 1"
settings_file_location = File.join(script_directory,"JdbcUploadSettings.json").gsub("/","\\")

if current_case.nil?
	nuix_case = utilities.get_case_factory.open(case_location)
else
	nuix_case = current_case
end

args = {
	:nuix_case => nuix_case,
	:module_settings_file => settings_file_location,
	:workspace_artifact_id => 1018882,
	:folder_artifact_id => 1003697,
	:production_set_name => "Production00001",
	:sql_password => "Secret1234!"
}

RelativityJdbcUpload.new(args).run