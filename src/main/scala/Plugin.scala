import io.github.gitbucket.solidbase.model.Version
import gitbucket.core.plugin.ReceiveHook
import gitbucket.core.plugin.PullRequestHook

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "githooks"
  override val pluginName: String = "Git Hooks Plugin"

  override val description: String =
    "A plugin that enables the existing .git/hooks functionality."
  override val versions: List[Version] = List(
    new Version("1.0.0"),
    new Version("1.1.0"),
    new Version("1.1.1")
  )

  override val receiveHooks: Seq[ReceiveHook] = Seq(
    new io.github.gitbucket.githook.hook.CommitHook()
  )
}
