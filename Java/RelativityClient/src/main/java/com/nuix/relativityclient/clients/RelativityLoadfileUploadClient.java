package com.nuix.relativityclient.clients;

import com.nuix.relativityclient.model.ClientLauncherSettings;
import com.nuix.relativityclient.utils.ApplicationLogger;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RelativityLoadfileUploadClient implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(RelativityLoadfileUploadClient.class.getName());

    private ApplicationLogger applicationLogger;
    private ClientLauncherSettings clientLauncherSettings;
    private Thread stdOutputThread;
    private Thread stdErrorThread;
    private Process process;
    private Path clientPath;

    public RelativityLoadfileUploadClient(ApplicationLogger applicationLogger, ClientLauncherSettings clientLauncherSettings) {
        this.applicationLogger = applicationLogger;

        applicationLogger.addStateListener(()-> {
            if (process != null) {
                process.destroyForcibly();
            }

            if (stdOutputThread != null) {
                stdOutputThread.interrupt();
            }

            if (stdErrorThread != null) {
                stdErrorThread.interrupt();
            }
        });

        this.clientLauncherSettings = clientLauncherSettings;
        clientPath = Paths.get(clientLauncherSettings.getScriptPath(), "RelativityLoadfileUpload", "Clients", clientLauncherSettings.getRelativityVersion(), "Client", "bin", "Release", "Client.exe");
    }

    public Path getClientPath(){
        return clientPath;
    }

    private void handleStdOutput(BufferedReader bufferedReader) {
        stdOutputThread = new Thread(() -> {
            try {
                String s = null;
                while ((s = bufferedReader.readLine()) != null) {
                    applicationLogger.logInfo(s);
                    applicationLogger.setStatus(s,0,1);
                }
            } catch (IOException e) {
                applicationLogger.logError("Cannot get process stdout," + e.getMessage());
            }
        });
        stdOutputThread.setName("Relativity Loadfile Upload Client - StdOutput");
        stdOutputThread.start();
    }

    private void handleStdError(BufferedReader bufferedReader) {
        stdErrorThread = new Thread(() -> {
            try {
                String s = null;
                while ((s = bufferedReader.readLine()) != null) {
                    applicationLogger.logError(s);
                    applicationLogger.setStatus(s,0,1);
                }
            } catch (IOException e) {
                applicationLogger.logError("Cannot get process stderr," + e.getMessage());
            }
        });
        stdErrorThread.setName("Relativity Loadfile Upload Client - StdError");
        stdErrorThread.start();
    }


    @Override
    public void run() {
        applicationLogger.logInfo("Starting loadfile upload");
        try {
            Runtime runtime = Runtime.getRuntime();
            String[] command = {clientPath.toString(), clientLauncherSettings.getClientSettingsPath(), clientLauncherSettings.getLoadfilePath(), "" + clientLauncherSettings.getWorkspaceArtifactId(), "" + clientLauncherSettings.getFolderArtifactId(), clientLauncherSettings.getRelativityPassword()};
            process = runtime.exec(command);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            handleStdOutput(stdInput);

            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            handleStdError(stdError);

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            applicationLogger.logError("Cannot run upload client, see Nuix logs for details");
        }
        applicationLogger.notifyComplete();
    }
}
