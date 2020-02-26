require 'set'
require 'json'
require_relative 'JdbcClient'

module NativeFileCopyMode
  DO_NOT_IMPORT_NATIVE_FILES = 0
  COPY_FILES = 1
  SET_FILE_LINKS = 2
end

module OverwriteMode
  APPEND = 0
  OVERLAY = 1
end

class RelativityJdbcUpload

  def initialize(args)
    nuix_case = args[:nuix_case]
    production_set_name = args[:production_set_name]
    sql_password = args[:sql_password]

    @workspace_artifact_id = args[:workspace_artifact_id]
    @folder_artifact_id = args[:folder_artifact_id]

    # Read module settings from JSON file or JSON
    if args[:module_settings].nil? && !args[:module_settings_file].nil?
      settings_file = File.open(args[:module_settings_file])
      @module_settings = JSON.load(settings_file)
    else
      @module_settings = args[:module_settings]
    end

    # Module settings and Nuix case nil checks
    raise "No module settings were provided" if @module_settings.nil?
    raise "This module requires a case" if nuix_case.nil?

    # Initialize warnings/error tracker sets
    @documents_with_warnings = Set.new
    @documents_with_errors = Set.new

    # Find production set in given nuix_case
    @production_set = nuix_case.find_production_set_by_name(production_set_name)
    raise "Cannot find production set #{production_set_name} in case" if @production_set.nil?

    # Populate @doc_ids, @begin_group_ids, @end_group_ids map
    analyze_family_relationships
    puts "Detected #{@begin_group_ids.size} document famil#{@begin_group_ids.size > 1 ? 'ies' : 'y'} in the production set"

    # Find metadata profile in given nuix_case
    metadata_profile_name = @module_settings["fieldsSettings"]["metadataProfileName"]
    metadata_profile = nuix_case.get_metadata_profile_store.get_metadata_profile(metadata_profile_name)
    raise "Cannot find metadata profile #{metadata_profile_name}" if metadata_profile.nil?

    # Populate metadata items map
    @metadata_items_map = {}
    metadata_profile.get_metadata.each do |metadataItem|
      @metadata_items_map[metadataItem.get_name] = metadataItem
    end

    # Retrieve sql_settings and instantiate jdbc_client
    sql_settings = @module_settings["sqlSettings"]
    server_name = sql_settings["serverName"]
    server_port = sql_settings["serverPort"]
    server_instance = sql_settings["instanceName"]
    server_domain = sql_settings["domain"]
    sql_username = sql_settings["username"]

    @jdbc_client = JdbcClient.new(server_name, server_port, server_instance, server_domain, sql_username, sql_password)
  end

  # Main method for jdbcUpload
  def run
    puts "Starting JDBC upload"

    @jdbc_client.workspace_artifact_id = @workspace_artifact_id
    upload_settings = @module_settings["uploadSettings"]
    mapping_list = @module_settings["fieldsSettings"]["FieldList"]

    copy_native = upload_settings["nativeCopyMode"] == NativeFileCopyMode::COPY_FILES
    has_native = copy_native || upload_settings["nativeCopyMode"] == NativeFileCopyMode::SET_FILE_LINKS

    # copy_text = true if mapping_list contains a mapping with metadata_item "TEXTPATH"
    text_field_mapping = mapping_list.detect { |mapping| mapping["loadfileColumn"] == "TEXTPATH" }
    copy_text = !text_field_mapping.nil?

    # Get Relativity workspace field columns
    field_columns = @jdbc_client.get_field_columns
    puts "Detected #{field_columns.size} document field columns in Relativity"

    # Get text_field_artifact_id from field columns
    text_field_mapped_field_name = text_field_mapping["workspaceColumn"]
    text_field_artifact_id = field_columns[text_field_mapped_field_name][:field_artifact_id]

    # Remove mapping with metadata item TEXTPATH because it has already been handled above
    filtered_mapping_list = mapping_list.reject { |mapping| mapping["loadfileColumn"] == "TEXTPATH" }

    begin
      @jdbc_client.fetch_workspace_resource_server(@workspace_artifact_id)
      puts "Workspace server: #{@jdbc_client.server_name}"

      puts "File share URL: #{@jdbc_client.file_share_url}" if copy_native
      puts "Data grid URL: #{@jdbc_client.data_grid_url}" if copy_text

    rescue => e
      STDERR.puts "Cannot get workspace resource server, upload of text and native files will be disabled, #{e.message}"
      @documents_with_warnings.merge(@doc_ids.values)
      copy_native = false
      copy_text = false
    end

    case_artifact_id = @jdbc_client.get_case_artifact_id
    puts "Case ArtifactID: #{case_artifact_id}"

    # Find identifier mapping in mapping list
    identifier_mapping = mapping_list.detect { |mapping| mapping["identifier"] }
    raise "Mapping missing identifier field. Please add identifier field to mapping, for example DocID or Control Number" if identifier_mapping.nil?

    # Find respective field column from Relativity workspace
    identifier_field_name = identifier_mapping["workspaceColumn"]
    identifier_field_column = field_columns[identifier_field_name]
    raise "Specified identifier field #{identifier_field_name} could not be found in Relativity workspace" if identifier_field_column.nil?

    folder_ancestors = @jdbc_client.get_item_ancestors(@folder_artifact_id)
    folder_ancestors << @folder_artifact_id
    puts "Got destination folder"

    overwrite_mode = upload_settings["overwriteMode"]
    production_set_items_artifact_ids = {}

    if overwrite_mode == OverwriteMode::APPEND
      puts "Reserving artifact IDs"

      @production_set.get_production_set_items.each do |production_set_item|
        doc_id = production_set_item.get_document_number.to_string

        begin
          artifact_id = @jdbc_client.create_artifact(10, @folder_artifact_id, case_artifact_id, doc_id)
          production_set_items_artifact_ids[production_set_item] = artifact_id
        rescue java.sql.SQLException => e
          STDERR.puts "Document #{doc_id} error, cannot create artifact, #{e.get_message}"
          @documents_with_errors.add(doc_id)
        end
      end

    elsif overwrite_mode == OverwriteMode::OVERLAY
      puts "Mapping documents to artifact IDs"

      @production_set.get_production_set_items.each do |production_set_item|
        doc_id = production_set_item.get_document_number.to_string

        begin
          artifact_id = @jdbc_client.get_document_artifact_id(doc_id, identifier_field_column)
          production_set_items_artifact_ids[production_set_item] = artifact_id
        rescue java.sql.SQLException => e
          STDERR.puts "Document #{doc_id} error, cannot create artifact, #{e.get_message}"
          @documents_with_errors.add(doc_id)
        end
      end

    else
      raise "Unsupported overwrite mode #{overwrite_mode}"
    end

    case overwrite_mode
    when OverwriteMode::APPEND
      puts "Inserting documents"
    when OverwriteMode::OVERLAY
      puts "Updating documents"
    else
      raise "Unsupported overwrite mode #{overwrite_mode}"
    end

    production_set_items_artifact_ids.each do |production_set_item, artifact_id|
      doc_id = production_set_item.get_document_number.to_string

      fields = {}
      filtered_mapping_list.each do |mapping|
        field_name = mapping["workspaceColumn"]
        field_column = field_columns[field_name]

        if !field_column.nil?
          metadata_item_name = mapping["loadfileColumn"]
          field_column_type = field_column[:column_type]
          field_column_name = field_column[:column_name]

          # Skip single/multi- choice fields from document table
          next if %w[CodeText MultiText].include? field_column_type

          field_value = get_field_value(production_set_item, metadata_item_name)
          fields[field_column_name] = field_value

        else
          STDERR.puts "Field name #{field_name} could not be found in SQL database"
          @documents_with_warnings.add(doc_id)
        end
      end

      begin
        if overwrite_mode == OverwriteMode::APPEND
          item_type = production_set_item.get_item.get_type
          file_icon = "#{doc_id}.#{item_type.get_preferred_extension}"
          supported_by_viewer = has_native ? true : nil
          relativity_native_type = has_native ? item_type.get_localised_name(java.util.Locale::ENGLISH) : nil

          incorrectly_formatted_fields = @jdbc_client.add_document_record(artifact_id, @folder_artifact_id, fields, has_native, file_icon, supported_by_viewer, relativity_native_type)
          @jdbc_client.set_ancestors(artifact_id, folder_ancestors)

        elsif overwrite_mode == OverwriteMode::OVERLAY
          incorrectly_formatted_fields = @jdbc_client.update_document_record(artifact_id, fields, has_native)
        else
          raise "Unsupported overwrite mode #{overwrite_mode}"
        end

        if incorrectly_formatted_fields.size > 0
          STDERR.puts "Document #{doc_id} warning, incorrect format for field(s) #{incorrectly_formatted_fields.to_a.join(', ')}"
          @documents_with_warnings.add(doc_id)
        end

        # Handle single/multi- choice fields
        filtered_mapping_list.each do |mapping|
          field_name = mapping["workspaceColumn"]
          field_column = field_columns[field_name]

          unless field_column.nil?
            metadata_item_name = mapping["loadfileColumn"]
            field_column_type = field_column[:column_type]
            field_column_code_type_id = field_column[:code_type_id]

            overlay = overwrite_mode == OverwriteMode::OVERLAY
            field_value = get_field_value(production_set_item, metadata_item_name)

            begin
              if field_column_type == "CodeText"
                values = Set.new
                values.add(field_value) unless field_value.nil?

                @jdbc_client.set_document_choice_values(artifact_id, field_column_code_type_id, case_artifact_id, values, overlay)

              elsif field_column_type == "MultiText"
                values = Set.new
                values.merge(field_value.to_string.split(', ')) unless field_value.nil?

                @jdbc_client.set_document_choice_values(artifact_id, field_column_code_type_id, case_artifact_id, values, overlay)
              end

            rescue java.sql.SQLException => e
              STDERR.puts "Document #{doc_id} warning, cannot set #{field_column_type == "CodeText" ? "single" : field_column_type} choice field #{field_name}, #{e.get_message}"
              @documents_with_warnings.add(doc_id)
            end
          end
        end

      rescue java.sql.SQLException => e
        STDERR.puts "Document #{doc_id} error, #{e.get_message}"
        @documents_with_errors.add(doc_id)
      end

    end

    if copy_text
      database_name = "EDDS#{@workspace_artifact_id}"
      text_base_path = java.nio.file.Paths.get(
          @jdbc_client.data_grid_url,
          database_name,
          "relativity_#{@jdbc_client.server_name}_#{database_name}_10".downcase,
          "Fields.ExtractedText",
          java.util.UUID.randomUUID.to_string[0..2]
      )

      begin
        puts "Got text folder #{text_base_path.to_file.get_absolute_path}"
        java.nio.file.Files.create_directories(text_base_path)

        items_in_folder_count = 0
        text_base_sub_path = text_base_path.resolve(java.util.UUID.randomUUID.to_string[0..2])
        puts "Uploading text"

        production_set_items_artifact_ids.each do |production_set_item, artifact_id|
          # Maximum of 10000 files to a folder
          if items_in_folder_count % 10000 == 0
            text_base_sub_path = text_base_path.resolve(java.util.UUID.randomUUID.to_string[0..2])
            java.nio.file.Files.create_directories(text_base_sub_path)
          end
          items_in_folder_count += 1

          doc_id = production_set_item.get_document_number.to_string
          item = production_set_item.get_item
          text_file_path = text_base_sub_path.resolve("#{item.get_guid}.txt")

          begin
            char_sequences = [item.get_text_object]
            java.nio.file.Files.write(text_file_path, char_sequences, java.nio.charset.StandardCharsets.UTF_16LE)
            file_size = java.nio.file.Files.size(text_file_path)

            overlay = overwrite_mode == OverwriteMode::OVERLAY
            @jdbc_client.set_data_grid_file_mapping(artifact_id, text_field_artifact_id, text_file_path.to_uri, file_size, overlay)

          rescue java.io.IOException => e
            STDERR.puts "Document #{doc_id} warning, cannot upload extracted text, #{e.get_message}"
            @documents_with_warnings.add(doc_id)
          end
        end

      rescue java.io.IOException => e
        STDERR.puts "Cannot upload extracted text, #{e.get_message}"
      end
    end

    if copy_native
      database_name = "EDDS#{@workspace_artifact_id}"
      native_base_path = java.nio.file.Paths.get(
          @jdbc_client.file_share_url,
          database_name,
          "RV_#{java.util.UUID.randomUUID.to_string.downcase}"
      )

      begin
        puts "Got natives folder #{native_base_path.to_file.get_absolute_path}"
        java.nio.file.Files.create_directories(native_base_path)

        items_in_folder_count = 0
        native_base_sub_path = native_base_path.resolve(java.util.UUID.randomUUID.to_string[0..2])
        puts "Uploading natives"

        production_set_items_artifact_ids.each do |production_set_item, artifact_id|
          # Maximum of 10000 files to a folder
          if items_in_folder_count % 10000 == 0
            native_base_sub_path = native_base_path.resolve(java.util.UUID.randomUUID.to_string[0..2])
            java.nio.file.Files.create_directories(native_base_sub_path)
          end
          items_in_folder_count += 1

          doc_id = production_set_item.get_document_number.to_string
          item = production_set_item.get_item
          native_file_path = native_base_sub_path.resolve(item.get_guid)

          begin
            item_binary_data = item.get_binary.get_binary_data

            item_binary_data.copy_to(native_file_path)
            file_size = item_binary_data.get_length

            filename = "#{doc_id}.#{item.get_type.get_preferred_extension}"
            identifier = "DOC#{artifact_id}_NATIVE"
            overlay = overwrite_mode == OverwriteMode::OVERLAY

            @jdbc_client.set_file_share_file_mapping(artifact_id, item.get_guid, filename, identifier, native_file_path.to_string, file_size, overlay)

          rescue java.io.IOException => e
            STDERR.puts "Document #{doc_id} warning, cannot upload native, #{e.get_message}"
            @documents_with_warnings.add(doc_id)
          end
        end

      rescue java.io.IOException => e
        STDERR.puts "Cannot upload natives, #{e.get_message}"
        @documents_with_warnings.merge(@doc_ids.values)
      end

    elsif has_native
      puts "Setting native links"

      production_set_items_artifact_ids.each do |production_set_item, artifact_id|
        doc_id = production_set_item.get_document_number.to_string
        item = production_set_item.get_item

        begin
          item_binary = item.get_binary

          unless item_binary.is_stored
            STDERR.puts "Document #{doc_id} warning, cannot upload native, native is not stored"
            @documents_with_warnings.add(doc_id)
            next
          end

          stored_path = item_binary.get_stored_path
          if stored_path.nil?
            STDERR.puts "Document #{doc_id} warning, cannot upload native, native is not stored outside case"
            @documents_with_warnings.add(doc_id)
            next
          end

          file_size = item_binary.get_binary_data.get_length
          filename = "#{doc_id}.#{item.get_type.get_preferred_extension}"
          identifier = "DOC#{artifact_id}_NATIVE"
          overlay = overwrite_mode == OverwriteMode::OVERLAY

          @jdbc_client.set_file_share_file_mapping(artifact_id, item.get_guid, filename, identifier, stored_path.to_string, file_size, overlay)

        rescue => e
          STDERR.puts "Document #{doc_id} warning, cannot upload native, #{e.get_message}"
          @documents_with_warnings.add(doc_id)
        end
      end
    end

    # End of program
    puts "----------------------"
    puts "Total documents: #{@production_set.get_items.size}"

    puts "Documents with warnings: #{@documents_with_warnings.size}" if @documents_with_warnings.size > 0
    puts "Total with errors: #{@documents_with_errors.size}" if @documents_with_errors.size > 0

  rescue => e
    STDERR.puts "Unexcepted error occurred: #{e.message}", e
  ensure
    puts "Upload finished"
    puts "Review upload log for details"
  end

  private def get_field_value(production_set_item, metadata_item_name)
    doc_id = production_set_item.get_document_number.to_string
    top_level_guid = production_set_item.get_item.get_top_level_item&.get_guid

    case metadata_item_name
    when "DOCID", "BEGINBATES", "ENDBATES"
      return doc_id

    when "PARENT_DOCID"
      parent_item = production_set_item.get_item.get_parent
      until parent_item.nil? do
        parent_doc_id = @doc_ids[parent_item.get_guid]
        return parent_doc_id unless parent_doc_id.nil?

        parent_item = parent_item.get_parent
      end
      return ""

    when "ATTACH_DOCID"
      attach_doc_ids = []
      production_set_item.get_item.get_descendants.each do |descendant|
        descendant_guid = descendant.get_guid
        descendant_doc_id = @doc_ids[descendant_guid]

        attach_doc_ids << descendant_doc_id unless descendant_doc_id.nil?
      end
      return attach_doc_ids.join(', ')

    when "BEGINGROUP"
      return @begin_group_ids[top_level_guid] unless top_level_guid.nil?

    when "ENDGROUP"
      return @end_group_ids[top_level_guid] unless top_level_guid.nil?

    when "PAGECOUNT"
      page_count = production_set_item.get_print_preview&.get_page_count
      # If document does not have page count return 1
      return page_count.nil? ? 1 : page_count

    when "TEXTPATH"
      return production_set_item.get_item.get_text_object.to_string

    else
      metadata_item = @metadata_items_map[metadata_item_name]
      return metadata_item.evaluate_unformatted(production_set_item.get_item) unless metadata_item.nil?

      # If metadata_item is nil log error and track warning
      STDERR.puts "Field #{metadata_item_name} not found in metadata profile"
      @documents_with_warnings.add(doc_id)
    end

  rescue => e
    STDERR.puts "Document #{doc_id ||= ""} warning, cannot evaluate metadata field #{metadata_item_name}, #{e.message}"
    @documents_with_warnings.add(doc_id)
  end

  private def analyze_family_relationships
    @doc_ids, @begin_group_ids, @end_group_ids = {}, {}, {}

    @production_set.get_production_set_items.each do |production_set_item|
      item_guid = production_set_item.get_item.get_guid
      doc_id = production_set_item.get_document_number.to_string

      @doc_ids[item_guid] = doc_id

      top_level_guid = production_set_item.get_item.get_top_level_item&.get_guid
      next if top_level_guid.nil?

      begin_group_id = @begin_group_ids[top_level_guid]
      if begin_group_id.nil? || begin_group_id <=> doc_id > 0
        @begin_group_ids[top_level_guid] = doc_id
      end

      end_group_id = @end_group_ids[top_level_guid]
      if end_group_id.nil? || end_group_id <=> doc_id < 0
        @end_group_ids[top_level_guid] = doc_id
      end
    end
  end

end