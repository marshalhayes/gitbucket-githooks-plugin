package io.github.gitbucket.githook.hook

import scala.util.Using
import gitbucket.core.model.Profile._
import gitbucket.core.plugin.ReceiveHook
import gitbucket.core.util.Directory.getRepositoryDir
import gitbucket.core.util.JGitUtil
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}
import profile.api._

import gitbucket.core.service._
import profile.blockingApi._

import java.io.{File, FileReader, BufferedReader}
import java.nio.file.{Files, Paths}

import io.github.gitbucket.githook.helpers.HookExecutor

/*

    Tie into GitBucket's ReceiveHook class.

    On post-receive, determine whether we need to execute the .git/hooks/post-receive hook.
    The script will only execute if the ReceiveCommand.Result was successful (see http://download.eclipse.org/jgit/docs/jgit-2.0.0.201206130900-r/apidocs/org/eclipse/jgit/transport/ReceiveCommand.Result.html#OK)

 */
class CommitHook
    extends ReceiveHook
    with RepositoryService
    with AccountService
    with CommitStatusService
    with SystemSettingsService {
  override def postReceive(
      owner: String,
      repository: String,
      receivePack: ReceivePack,
      command: ReceiveCommand,
      pusher: String,
      mergePullRequest: Boolean
  )(implicit session: Session): Unit = {
    val branch = command.getRefName.stripPrefix("refs/heads/")
    val repositoryDir = getRepositoryDir(owner, repository)

    if (
      branch != command.getRefName && command.getType != ReceiveCommand.Type.DELETE
    ) {
      getRepository(owner, repository).foreach { repositoryInfo =>
        Using.resource(Git.open(getRepositoryDir(owner, repository))) { git =>
          val sha = command.getNewId.name
          val revCommit = JGitUtil.getRevCommitFromId(git, command.getNewId)

          val config = git.getRepository().getConfig()

          HookExecutor.executeHook(
            hook = "post-receive",
            owner = owner,
            repositoryName = repository,
            branchName = branch,
            sha = sha,
            commitMessage = revCommit.getShortMessage,
            commitUserName = revCommit.getCommitterIdent.getName,
            pusher = pusher,
            repositoryDir = repositoryDir.getAbsolutePath().toString(),
            config = config
          )
        }
      }
    }
  }
}
