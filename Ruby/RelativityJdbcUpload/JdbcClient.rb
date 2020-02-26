require 'set'

class JdbcClient

  def initialize(server_name, server_port, server_instance, server_domain, username, password)
    # Build connection string
    connection_string = "jdbc:jtds:sqlserver://#{server_name}"
    connection_string += ":#{server_port}" unless server_port.nil?
    connection_string += ";instance=#{server_instance}" unless server_instance.nil? || server_instance.length == 0
    connection_string += ";domain=#{server_domain}" unless server_domain.nil? || server_domain.length == 0

    @connection = java.sql.DriverManager.get_connection(connection_string, username, password)
  end

  def server_name
    @server_name
  end

  def data_grid_url
    @data_grid_url
  end

  def file_share_url
    @file_share_url
  end

  def workspace_artifact_id=(artifact_id)
    @database_name = "[EDDS#{artifact_id}].[EDDSDBO]"
  end

  def get_case_artifact_id
    select_query = "SELECT [ArtifactID] FROM #{@database_name}.[Artifact] WHERE [ArtifactTypeId] = 8"

    statement = @connection.create_statement
    result_set = statement.execute_query(select_query)

    if result_set.next
      result_set.get_int(1)
    else
      raise java.sql.SQLException.new("Could not find case Artifact ID")
    end
  end

  def fetch_workspace_resource_server(artifact_id)
    select_query = "SELECT"\
      " [ServerID]"\
      ",[DefaultFileLocationCodeArtifactId]"\
      ",[DataGridFileShareResourceServerArtifactID]"\
      "FROM [EDDS].[eddsdbo].[Case] WHERE [ArtifactID] = ?"

    prepared_statement = @connection.prepare_statement(select_query)
    prepared_statement.set_int(1, artifact_id)

    result_set = prepared_statement.execute_query
    result_set.next

    server_id = result_set.get_int(1)
    file_share_id = result_set.get_int(2)
    data_grid_id = result_set.get_int(3)

    @server_name = get_resource_server_property(server_id, "Name")
    @file_share_url = get_resource_server_property(file_share_id, "URL")
    @data_grid_url = get_resource_server_property(data_grid_id, "URL")
  end

  def get_field_columns
    select_query = "SELECT"\
      " [Field].[ArtifactID]"\
      ",[Field].[FieldArtifactTypeID]"\
      ",[Field].[DisplayName]"\
      ",[Field].[ArtifactViewFieldID]"\
      ",[ArtifactViewField].[ColumnName]"\
      ",[ArtifactViewField].[ItemListType]"\
      ",[Field].[CodeTypeID]"\
      " FROM #{@database_name}.[Field] AS [Field]"\
      " INNER JOIN #{@database_name}.[ArtifactViewField] as [ArtifactViewField]"\
      " ON [Field].[ArtifactViewFieldID] = [ArtifactViewField].[ArtifactViewFieldID]"\
      " WHERE [Field].[FieldArtifactTypeID] = 10"

    statement = @connection.create_statement
    result_set = statement.execute_query(select_query)

    field_columns = {}
    while result_set.next do
      field_artifact_id = result_set.get_int(1)
      field_name = result_set.get_string(3)
      column_name = result_set.get_string(5)
      column_type = result_set.get_string(6)
      code_type_id = result_set.get_int(7)

      field_columns[field_name] = {
          :column_name => column_name,
          :column_type => column_type,
          :field_artifact_id => field_artifact_id,
          :code_type_id => code_type_id > 0 ? code_type_id : nil
      }
    end

    field_columns
  end

  def get_document_artifact_id(doc_id, identifier)
    select_query = "SELECT [ArtifactID] FROM #{@database_name}.[Document] WHERE [#{identifier[:column_name]}] = ?"

    prepared_statement = @connection.prepare_statement(select_query)
    prepared_statement.set_string(1, doc_id)

    result_set = prepared_statement.execute_query
    if result_set.next
      result_set.get_int(1)
    else
      raise java.sql.SQLException.new("Document does not exist in workspace")
    end
  end

  def create_artifact(artifact_type, parent_artifact_id, container_id, text_identifier)
    insert_query = "INSERT INTO #{@database_name}.[Artifact]"\
      " ([ArtifactTypeID]"\
      ",[ParentArtifactID]"\
      ",[AccessControlListID]"\
      ",[AccessControlListIsInherited]"\
      ",[CreatedOn]"\
      ",[LastModifiedOn]"\
      ",[LastModifiedBy]"\
      ",[CreatedBy]"\
      ",[ContainerID]"\
      ",[Keywords]"\
      ",[Notes]"\
      ",[DeleteFlag]"\
      ",[TextIdentifier])"\
      " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

    prepared_statement = @connection.prepare_statement(insert_query, java.sql.Statement.RETURN_GENERATED_KEYS)
    prepared_statement.set_int(1, artifact_type)
    prepared_statement.set_int(2, parent_artifact_id)
    prepared_statement.set_int(3, 1)
    prepared_statement.set_int(4, 1)
    prepared_statement.set_date(5, java.sql.Date.new(Time.now.to_f * 1000))
    prepared_statement.set_date(6, java.sql.Date.new(Time.now.to_f * 1000))
    prepared_statement.set_int(7, 777)
    prepared_statement.set_int(8, 777)
    prepared_statement.set_int(9, container_id)
    prepared_statement.set_string(10, "")
    prepared_statement.set_string(11, "")
    prepared_statement.set_boolean(12, false)
    prepared_statement.set_string(13, text_identifier)

    affected_rows = prepared_statement.execute_update
    if affected_rows == 0
      raise java.sql.SQLException.new("Creating Artifact failed, no rows affected")
    end

    generated_keys = prepared_statement.get_generated_keys

    if generated_keys.next
      generated_keys.get_int(1)
    else
      raise java.sql.SQLException.new("Creating Artifact failed, no ID obtained")
    end
  end

  def set_document_choice_values(document_artifact_id, code_type_id, container_id, values, overlay)
    if overlay
      delete_statement = "DELETE FROM #{@database_name}.[ZCodeArtifact_#{code_type_id}] WHERE [AssociatedArtifactID] = ?"

      delete_association_statement = @connection.prepare_statement(delete_statement)
      delete_association_statement.set_int(1, document_artifact_id)
      delete_association_statement.execute_update
    end

    values.each do |value|
      select_query = "SELECT [ArtifactID] FROM #{@database_name}.[Code] WHERE [CodeTypeID] = ? AND [Name] = ?"

      select_code_id = @connection.prepare_statement(select_query)
      select_code_id.set_int(1, code_type_id)
      select_code_id.set_string(2, value)

      result_set = select_code_id.execute_query
      choice_artifact_id = nil
      while result_set.next do
        choice_artifact_id = result_set.get_int(1)
      end

      # Track new choice option
      if choice_artifact_id.nil?
        choice_artifact_id = create_artifact(7, container_id, container_id, value)

        choice_insert_query = "INSERT INTO #{@database_name}.[Code]"\
          " ([ArtifactID]"\
          ",[CodeTypeID]"\
          ",[Order]"\
          ",[IsActive]"\
          ",[UpdateInSearchEngine]"\
          ",[Name])"\
          " VALUES (?, ?, ?, ?, ?, ?)"

        insert_new_choice_statement = @connection.prepare_statement(choice_insert_query)
        insert_new_choice_statement.set_int(1, choice_artifact_id)
        insert_new_choice_statement.set_int(2, code_type_id)
        insert_new_choice_statement.set_int(3, 0)
        insert_new_choice_statement.set_boolean(4, true)
        insert_new_choice_statement.set_boolean(5, false)
        insert_new_choice_statement.set_string(6, value)
        insert_new_choice_statement.execute_update
      end

      # Set choice on document
      insert_query = "INSERT INTO #{@database_name}.[ZCodeArtifact_#{code_type_id}]"\
        " ([CodeArtifactID]"\
        ",[AssociatedArtifactID])"\
        " VALUES (?, ?)"

      insert_choice_to_document_statement = @connection.prepare_statement(insert_query)
      insert_choice_to_document_statement.set_int(1, choice_artifact_id)
      insert_choice_to_document_statement.set_int(2, document_artifact_id)
      insert_choice_to_document_statement.execute_update
    end
  end

  def add_document_record(artifact_id, parent_artifact_id, fields, has_native, file_icon, supported_by_viewer, relativity_native_type)
    insert_query = "INSERT INTO #{@database_name}.[Document]"\
      " ([ArtifactID]"\
      ",[AccessControlListID_D]"\
      ",[ParentArtifactID_D]"\
      ",[HasNative]"\
      ",[HasAnnotations]"\
      ",[HasInlineTags]"\
      ",[FileIcon]"\
      ",[SupportedByViewer]"\
      ",[RelativityNativeType]"

    values_sub_query = "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?"
    fields.each_key do |fieldName|
      insert_query << ",[#{fieldName}]"
      values_sub_query << ", ?"
    end

    insert_query << ") #{values_sub_query})"

    prepared_statement = @connection.prepare_statement(insert_query)
    prepared_statement.set_int(1, artifact_id)
    prepared_statement.set_int(2, 1)
    prepared_statement.set_int(3, parent_artifact_id)
    prepared_statement.set_boolean(4, has_native)
    prepared_statement.set_boolean(5, false)
    prepared_statement.set_boolean(6, false)
    prepared_statement.set_string(7, file_icon)
    prepared_statement.set_object(8, supported_by_viewer)
    prepared_statement.set_object(9, relativity_native_type)

    field_position = 10
    incorrectly_formatted_fields = Set.new

    fields.each do |fieldName, fieldValue|
      begin
        prepared_statement.set_object(field_position, fieldValue)
      rescue java.sql.SQLException
        begin
          prepared_statement.set_string(field_position, fieldValue.to_s)
        rescue java.sql.SQLException
          # Cannot set value as object
          incorrectly_formatted_fields.add(fieldName)
          prepared_statement.set_object(field_position, nil)
        end
      end
      field_position += 1
    end

    affected_rows = prepared_statement.execute_update
    if affected_rows == 0
      raise java.sql.SQLException("Inserting Document failed, no rows affected")
    end

    incorrectly_formatted_fields
  end

  def get_item_ancestors(artifact_id)
    select_query = "SELECT [AncestorArtifactID] FROM #{@database_name}.[ArtifactAncestry] WHERE [ArtifactID] = ?"

    prepared_statement = @connection.prepare_statement(select_query)
    prepared_statement.setInt(1, artifact_id)

    result_set = prepared_statement.execute_query

    ancestors = []
    while result_set.next
      ancestors << result_set.get_int(1)
    end

    ancestors
  end

  def set_ancestors(artifact_id, ancestor_ids)
    insert_query = "INSERT INTO #{@database_name}.[ArtifactAncestry]"\
      " ([ArtifactID]"\
      ",[AncestorArtifactID])"\
      " VALUES (?, ?)"

    prepared_statement = @connection.prepareStatement(insert_query)
    ancestor_ids.each do |ancestor_id|
      prepared_statement.set_int(1, artifact_id)
      prepared_statement.set_int(2, ancestor_id)

      prepared_statement.add_batch
    end

    prepared_statement.execute_batch
  end

  def set_data_grid_file_mapping(artifact_id, field_artifact_id, file_location, file_size, overlay)
    if overlay
      drop_query = "DELETE FROM #{@database_name}.[DataGridFileMapping] WHERE [ArtifactID] = ? AND [FieldArtifactID] = ?"

      drop_statement = @connection.prepare_statement(drop_query)
      drop_statement.set_int(1, artifact_id)
      drop_statement.set_int(2, field_artifact_id)

      drop_statement.execute_update
    end

    insert_query = "INSERT INTO #{@database_name}.[DataGridFileMapping]"\
      " ([ArtifactID]"\
      ",[FieldArtifactID]"\
      ",[FileLocation]"\
      ",[FileSize])"\
      " VALUES (?, ?, ?, ?)"

    insert_statement = @connection.prepare_statement(insert_query)
    insert_statement.set_int(1, artifact_id)
    insert_statement.set_int(2, field_artifact_id)
    insert_statement.set_string(3, file_location)
    insert_statement.set_long(4, file_size)

    affected_rows = insert_statement.execute_update
    if affected_rows == 0
      raise java.sql.SQLException.new("Setting data grid file mapping failed, no rows affected")
    end
  end

  def set_file_share_file_mapping(artifact_id, guid, filename, identifier, file_location, file_size, overlay)
    if overlay
      drop_query = "DELETE FROM #{@database_name}.[File] WHERE [DocumentArtifactID] = ?"

      drop_statement = @connection.prepare_statement(drop_query)
      drop_statement.set_int(1, artifact_id)

      drop_statement.execute_update
    end

    insert_query = "INSERT INTO #{@database_name}.[File]"\
      " ([Guid]"\
      ",[DocumentArtifactID]"\
      ",[Filename]"\
      ",[Order]"\
      ",[Type]"\
      ",[Rotation]"\
      ",[Identifier]"\
      ",[Location]"\
      ",[InRepository]"\
      ",[Size]"\
      ",[Details]"\
      ",[Billable]"\
      " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

    insert_statement = @connection.prepare_statement(insert_query)
    insert_statement.set_string(1, guid)
    insert_statement.set_int(2, artifact_id)
    insert_statement.set_string(3, filename)
    insert_statement.set_int(4, 0)
    insert_statement.set_int(5, 0)
    insert_statement.set_int(6, -1)
    insert_statement.set_string(7, identifier)
    insert_statement.set_string(8, file_location)
    insert_statement.set_boolean(9, true)
    insert_statement.set_long(10, file_size)
    insert_statement.set_object(11, nil)
    insert_statement.set_boolean(12, true)

    affected_rows = insert_statement.execute_update
    if affected_rows == 0
      raise java.SQLException.new("Setting fileshare file mapping failed, no rows affected")
    end
  end

  def update_document_record(artifact_id, fields, has_native)
    update_query = "UPDATE #{@database_name}.[Document] SET [HasNative] = ?"

    fields.each_key do |fieldName|
      update_query << ",[#{fieldName}] = ?"
    end
    update_query << " WHERE [ArtifactID] = ?"

    update_statement = @connection.prepare_statement(update_query)
    update_statement.set_boolean(1, has_native)

    field_position = 2
    incorrectly_formatted_fields = Set.new

    fields.each do |fieldName, fieldValue|
      begin
        update_statement.set_object(field_position, fieldValue)
      rescue java.sql.SQLException
        begin
          update_statement.set_string(field_position, fieldValue)
        rescue java.sql.SQLException
          # Cannot set value as object
          incorrectly_formatted_fields.add(fieldName)
          update_statement.set_object(field_position, nil)
        end
      end
      field_position += 1
    end

    update_statement.set_int(field_position, artifact_id)

    affected_rows = update_statement.execute_update
    if affected_rows == 0
      raise java.sql.SQLException.new("Updating Document failed, no rows affected")
    end

   incorrectly_formatted_fields
  end

  private def get_resource_server_property(artifact_id, property_name)
    select_query = "SELECT [#{property_name}] FROM [EDDS].[eddsdbo].[ResourceServer] WHERE [ArtifactId] = ?"

    prepared_statement = @connection.prepare_statement(select_query)
    prepared_statement.set_int(1, artifact_id)

    result_set = prepared_statement.execute_query
    result_set.next ? result_set.get_string(1) : nil
  end

end