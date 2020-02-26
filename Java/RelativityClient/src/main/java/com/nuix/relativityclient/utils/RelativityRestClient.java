package com.nuix.relativityclient.utils;

import com.nuix.relativityclient.relativitytypes.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelativityRestClient {
    private static final Logger LOGGER = LogManager.getLogger(RelativityRestClient.class.getName());

    private final Client client;
    private String hostUrl;
    private String authCredentials;

    public RelativityRestClient() {
        client = ClientBuilder.newClient();
    }

    public void setHostUrl(String hostUrl) {
        LOGGER.info("Settings hostUrl: " + hostUrl);
        this.hostUrl = hostUrl;
    }

    public void buildAndSetAuthCredentials(String username, char[] password) {
        byte[] usernamePasswordBytes = (username + ":" + new String(password)).getBytes();
        this.authCredentials = Base64.getEncoder().encodeToString(usernamePasswordBytes);
    }

    public String getVersion() throws IOException {
        Response response = getInvocationBuilder("rsapi").get();
        return getEntityWithChecks(response, new GenericType<Version>() {}).getVersion();
    }

    public PagedResponse<Workspace> getAvailableWorkspaces() throws IOException {
        Response response = getInvocationBuilder("Relativity/Workspace").get();
        return getEntityWithChecks(response, new GenericType<PagedResponse<Workspace>>() {});
    }

    public Folder getWorkspaceRootFolder(long workspaceArtifactId) throws IOException {
        Map<String, Long> payload = new HashMap<>();
        payload.put("workspaceArtifactID", workspaceArtifactId);

        Response response = getInvocationBuilder("api/Relativity.Services.Folder.IFolderModule/Folder%20Manager/GetWorkspaceRootAsync").post(Entity.json(payload));
        return getEntityWithChecks(response, new GenericType<Folder>() {});
    }

    //{workspaceArtifactID, parentID}
    public List<Folder> getSubFolders(long workspaceArtifactId, long parentId) throws IOException {
        Map<String, Long> payload = new HashMap<>();
        payload.put("workspaceArtifactID", workspaceArtifactId);
        payload.put("parentID", parentId);

        Response response = getInvocationBuilder("api/Relativity.Services.Folder.IFolderModule/Folder%20Manager/GetChildrenAsync").post(Entity.json(payload));
        return getEntityWithChecks(response, new GenericType<List<Folder>>() {});
    }

    public List<Field> getWorkspaceFields(long workspaceArtifactId) throws IOException {
        String fieldQuery = "Fields Query: {\n\tcondition: " + Field.condition + "\n\tfields: " + String.join(", ", Field.fields) + "\n}";
        LOGGER.info(fieldQuery);

        Map<String, Object> payload = new HashMap<>();
        payload.put("condition", Field.condition);
        payload.put("fields", Field.fields);

        Response response = getInvocationBuilder("Workspace/" + workspaceArtifactId + "/Field/QueryResult").post(Entity.json(payload));
        List<Field> fields = getEntityWithChecks(response, new GenericType<PagedResponse<Field>>() {}).getResults();

        fields.removeIf(field -> Field.blackList.contains(field.getRelativityTextIdentifier()));
        fields.sort((fieldA, fieldB) -> fieldA.getRelativityTextIdentifier().compareToIgnoreCase(fieldB.getRelativityTextIdentifier()));

        return fields;
    }

    private Invocation.Builder getInvocationBuilder(String url) {
        return client.target(hostUrl + "/Relativity.REST/" + url)
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", "Basic " + authCredentials)
            .header("X-CSRF-Header", "");
    }

    private <ResponseType> ResponseType getEntityWithChecks(Response response, GenericType<ResponseType> entityType) throws IOException {
        int status = response.getStatus();
        switch (status) {
            case 401:
                throw new NotAuthorizedException("Cannot connect to Relativity with supplied credentials.");
            case 403:
                throw new NotAuthorizedException("User does not have permissions to perform operation in Relativity.");
            case 200:
            case 201:
            case 202:
                return response.readEntity(entityType);
            default:
                String stringResponse = response.readEntity(String.class);
                throw new IOException("Responded with HTTP/" + status + " " + stringResponse);
        }
    }
}
