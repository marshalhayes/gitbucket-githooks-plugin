package io.github.gitbucket.githook.hook

import gitbucket.core.model.Profile._
import gitbucket.core.plugin.ReceiveHook
import gitbucket.core.service._
import gitbucket.core.util.Directory.getRepositoryDir
import gitbucket.core.util.JGitUtil
import io.github.gitbucket.githook.helpers.HookExecutor
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}
import profile.blockingApi._

import scala.util.Using

class CommitHook
    extends ReceiveHook
    with RepositoryService
    with AccountService
    with CommitStatusService
    with SystemSettingsService {

  override def preReceive(
      owner: String,
      repository: String,
      receivePack: ReceivePack,
      command: ReceiveCommand,
      pusher: String,
      mergePullRequest: Boolean
  )(implicit
      session: Session
  ): Option[String] = {
    implicit val ec: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.global

    val branch = command.getRefName.stripPrefix("refs/heads/")
    val repositoryDir = getRepositoryDir(owner, repository)

    var output: Option[String] = None

    if (
      branch != command.getRefName && command.getType != ReceiveCommand.Type.DELETE
    ) {
      getRepository(owner, repository).foreach { _ =>
        Using.resource(Git.open(getRepositoryDir(owner, repository))) { git =>
          val sha = command.getNewId.name
          val revCommit = JGitUtil.getRevCommitFromId(git, command.getNewId)

          val config = git.getRepository.getConfig

          val (exitCode, stdout, stderr) = HookExecutor.executeHook(
            hook = "pre-receive",
            owner = owner,
            repositoryName = repository,
            branchName = branch,
            sha = sha,
            commitMessage = revCommit.getShortMessage,
            commitUserName = revCommit.getCommitterIdent.getName,
            pusher = pusher,
            repositoryDir = repositoryDir.getAbsolutePath,
            config = config
          )

          if (exitCode != 0)
          {
            output = Option(stdout + stderr)
          }

          output = None
        }
      }
    }

    output
  }

  override def postReceive(
      owner: String,
      repository: String,
      receivePack: ReceivePack,
      command: ReceiveCommand,
      pusher: String,
      mergePullRequest: Boolean
  )(implicit session: Session): Unit = {
    implicit val ec: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.global

    val branch = command.getRefName.stripPrefix("refs/heads/")
    val repositoryDir = getRepositoryDir(owner, repository)

    if (
      branch != command.getRefName && command.getType != ReceiveCommand.Type.DELETE
    ) {
      getRepository(owner, repository).foreach { _ =>
        Using.resource(Git.open(getRepositoryDir(owner, repository))) { git =>
          val sha = command.getNewId.name
          val revCommit = JGitUtil.getRevCommitFromId(git, command.getNewId)

          val config = git.getRepository.getConfig

          HookExecutor.executeHook(
            hook = "post-receive",
            owner = owner,
            repositoryName = repository,
            branchName = branch,
            sha = sha,
            commitMessage = revCommit.getShortMessage,
            commitUserName = revCommit.getCommitterIdent.getName,
            pusher = pusher,
            repositoryDir = repositoryDir.getAbsolutePath,
            config = config
          )
        }
      }
    }
  }
}
