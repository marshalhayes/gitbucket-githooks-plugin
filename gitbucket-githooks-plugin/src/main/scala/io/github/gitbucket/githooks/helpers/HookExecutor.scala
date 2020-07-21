package io.github.gitbucket.githook.helpers

import scala.util.Using
import gitbucket.core.model.Profile._
import gitbucket.core.plugin.PullRequestHook
import gitbucket.core.util.Directory.getRepositoryDir
import gitbucket.core.util.JGitUtil
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}
import profile.api._

import gitbucket.core.service._
import profile.blockingApi._

import java.io.{File, FileReader, BufferedReader}
import java.nio.file.{Files, Paths}

object HookExecutor {
    def executeHooks(hook: String, owner: String, repositoryName: String, branchName: String, sha: String, commitMessage: String, commitUserName: String, pusher: String, repositoryDir: String, config: org.eclipse.jgit.lib.Config) {
            val CONFIG_CORE_KEY = org.eclipse.jgit.lib.ConfigConstants.CONFIG_CORE_SECTION
            val HOOKS_PATH_KEY  = org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_HOOKS_PATH
            
            var hooksPath = config.getString(CONFIG_CORE_KEY, null, HOOKS_PATH_KEY)

            if (hooksPath == null) {
                hooksPath = Paths.get(repositoryDir, "hooks").toString()
            }

            val pathToHookScript = Paths.get(hooksPath, hook).toAbsolutePath()

            if (Files.exists(pathToHookScript)) {
                var shebang : String = ""

                Using.resource(new BufferedReader(new FileReader(pathToHookScript.toString()))) { reader => 
                    shebang = reader.readLine()

                    if (shebang != null && shebang.length() > 2) {
                        shebang = shebang.substring(2, shebang.length())
                    }
                }

                try {
                    var processBuilder : ProcessBuilder = null;

                    if (shebang.length() > 0) {
                        processBuilder = new ProcessBuilder(shebang, pathToHookScript.toString())
                    } else {
                        processBuilder = new ProcessBuilder(pathToHookScript.toString())
                    }

                    if (processBuilder != null) {
                        // Add variables in the scope of the hook script
                        val environment = processBuilder.environment()

                        environment.put("OWNER", owner)
                        environment.put("REPOSITORY_NAME", repositoryName)
                        environment.put("REPOSITORY_DIR", repositoryDir)
                        environment.put("BRANCH_NAME", branchName)
                        environment.put("SHA", sha)
                        environment.put("COMMIT_MESSAGE", commitMessage)
                        environment.put("COMMIT_USERNAME", commitUserName)
                        environment.put("PUSHER", pusher)

                        // Set the working directory the remote repository
                        processBuilder.directory(new File(repositoryDir))

                        val outputPath = Paths.get(hooksPath, "output")
                        val outputFile = outputPath.toFile()

                        processBuilder.redirectError(outputFile)
                        processBuilder.redirectOutput(outputFile)

                        val runner = processBuilder.start()
                    }
                }
                catch {
                    case e: Exception => e.printStackTrace()
                }
            }
        }
}