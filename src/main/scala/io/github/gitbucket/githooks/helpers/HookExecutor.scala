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
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.nio.file.Path

object HookExecutor {
  def executeHook(
      hook: String,
      owner: String,
      repositoryName: String,
      branchName: String,
      sha: String,
      commitMessage: String,
      commitUserName: String,
      pusher: String,
      repositoryDir: String,
      config: org.eclipse.jgit.lib.Config
  )(implicit executionContext: ExecutionContext): Future[Int] = {
    val pathToHookScript =
      HookExecutor.getHooksPath(hook, repositoryDir, config)

    if (Files.exists(pathToHookScript)) {
      var shebang: String = ""

      Using.resource(
        new BufferedReader(new FileReader(pathToHookScript.toString()))
      ) { reader =>
        shebang = reader.readLine()

        if (shebang != null && shebang.length() > 2) {
          shebang = shebang.substring(2, shebang.length())
        }
      }

      var processBuilder: ProcessBuilder = null

      if (shebang.length() > 0) {
        processBuilder =
          new ProcessBuilder(shebang, pathToHookScript.toString())
      } else {
        processBuilder = new ProcessBuilder(pathToHookScript.toString())
      }

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

      // Set the working directory to the remote repository
      processBuilder
        .directory(new File(repositoryDir))
        .inheritIO()

      return Future {
        processBuilder
          .start()
          .waitFor()
      }(executionContext)
    }

    return null
  }

  private def getHooksPath(
      hook: String,
      repositoryDir: String,
      config: org.eclipse.jgit.lib.Config
  ): Path = {
    val CONFIG_CORE_KEY =
      org.eclipse.jgit.lib.ConfigConstants.CONFIG_CORE_SECTION
    val HOOKS_PATH_KEY =
      org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_HOOKS_PATH

    var hooksPath = config.getString(CONFIG_CORE_KEY, null, HOOKS_PATH_KEY)

    if (hooksPath == null) {
      hooksPath = Paths.get(repositoryDir, "hooks").toString()
    }

    return Paths
      .get(hooksPath, hook)
      .toAbsolutePath()
  }
}
