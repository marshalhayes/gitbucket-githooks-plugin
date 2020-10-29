package io.github.gitbucket.githook.hook

import scala.util.Using
import gitbucket.core.model.Profile._
import gitbucket.core.model.Issue
import gitbucket.core.controller.Context
import gitbucket.core.plugin.PullRequestHook
import gitbucket.core.util.Directory.getRepositoryDir
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.util.JGitUtil
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}
import profile.api._

import gitbucket.core.service._
import profile.blockingApi._

import java.io.{File, FileReader, BufferedReader}
import java.nio.file.{Files, Paths}

import io.github.gitbucket.githook.helpers.HookExecutor

class PRHook extends PullRequestHook
  with PullRequestService with IssuesService with CommitsService with AccountService with WebHookService
  with WebHookPullRequestService with WebHookPullRequestReviewCommentService with ActivityService with MergeService
  with RepositoryService with LabelsService with PrioritiesService with MilestonesService {
    override def merged(issue: Issue, repository: RepositoryInfo)(implicit session: Session, context: Context): Unit = {
        if (issue.isPullRequest) {
            var (_, pullRequest) = getPullRequest(issue.userName, issue.repositoryName, issue.issueId).getOrElse((null, null))

            val prRepositoryDir = getRepositoryDir(pullRequest.requestUserName, pullRequest.requestRepositoryName)

            var config : org.eclipse.jgit.lib.StoredConfig = null
            var sha : String = null

            val revCommit = Using.resource(Git.open(prRepositoryDir)) { git => 
                val repo = git.getRepository()
                val id = repo.resolve(pullRequest.commitIdTo)

                config = repo.getConfig()
                sha = repo.resolve(pullRequest.branch).name

                JGitUtil.getRevCommitFromId(git, id)
            }

            var currentLoggedInAccount : gitbucket.core.model.Account = context.loginAccount.getOrElse(null)

            val pusher = if (currentLoggedInAccount != null) currentLoggedInAccount.userName else ""

            HookExecutor.executeHook(
                hook = "post-receive",
                owner = repository.owner,
                repositoryName = pullRequest.repositoryName,
                branchName = pullRequest.branch,
                sha = sha,
                commitMessage = revCommit.getShortMessage,
                commitUserName = revCommit.getCommitterIdent.getName,
                pusher = pusher,
                repositoryDir = prRepositoryDir.getAbsolutePath().toString(),
                config = config
            )
        }
    }
}