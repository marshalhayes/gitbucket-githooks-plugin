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
import scala.concurrent.ExecutionContext

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

    if (
      branch != command.getRefName && command.getType != ReceiveCommand.Type.DELETE
    ) {
      getRepository(owner, repository).foreach { repositoryInfo =>
        Using.resource(Git.open(getRepositoryDir(owner, repository))) { git =>
          val sha = command.getNewId.name
          val revCommit = JGitUtil.getRevCommitFromId(git, command.getNewId)

          val config = git.getRepository().getConfig()

          HookExecutor.executeHook(
            hook = "pre-receive",
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

    return null
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
