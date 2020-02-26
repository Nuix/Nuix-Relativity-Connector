require 'open3'

class RelativityLoadfileUpload
  def self.loadfileUpload(clientVersion, settingsFile, loadfile, workspaceId, folderId, password)
    puts "Starting RelativityLoadfileUpload with client version #{clientVersion}"
    
    clientPath = File.join(File.dirname(__FILE__),"Clients",clientVersion,"Client","bin","Release","Client.exe").gsub("/","\\")
    puts "#{clientPath} #{settingsFile} #{loadfile} #{workspaceId} #{folderId} [password]"

    if File.exist?(clientPath)
      Open3.popen3(clientPath, settingsFile, loadfile, workspaceId, folderId, password) do |stdin, stdout, stderr, thread|
        while line=stdout.gets do 
          puts(line)
        end
        while line=stderr.gets do 
          STDERR.puts(line)
        end
        if !thread.value.success?
          raise "The Relativity Upload Client encountered an error. See logs for details."
        end
      end

    else
      raise "The Relativity Upload Client for Relativity version #{clientVersion} could not be found under #{clientPath}. Please see documentation instructions on how to build the client."
    end
  end
end
