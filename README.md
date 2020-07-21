# gitbucket-githooks-plugin

A GitBucket plugin that enables a limited set of `~/.git/hooks`. 

## Usage

This GitBucket plugin is intended to be a one-and-done solution for enabling support for the [built-in hook functionality in Git](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks). 

To begin using the plugin:

1. Download the latest release
2. Install in the `$GITBUCKET_HOME/plugins` directory
3. Restart GitBucket
4. (Optional) Create your hook executable if you haven't already. Keep it somewhere handy for reference later.
5. Place a *[supported hook](#supported-hooks)* in the bare repository on the server where GitBucket is running. This should be under `<repo_path>/.git/hooks/` unless you configured a different one.

<h2 id="supported-hooks">Supported Hooks</h2>

Only a limited set of Git hooks are available in the latest release. These include:

- [post-receive](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks#_post_receive)

### Available Environment Variables

The environment variables exposed to the scripts are:

| Variable		| Description				|
| ------------- 	| ------------- 			|
| OWNER			| the owner of the repository		|
| REPOSITORY_NAME	| the name of the repository		|
| BRANCH_NAME		| the branch name being committed	|
| SHA			| the hash of the commit		|
| COMMIT_MESSAGE	| the commit message			|
| COMMIT_USERNAME	| the username of the committer		|
| PUSHER		| the pusher				|

The script is executed from the root directory of the remote repository. In other words, the working directory is: `$GITBUCKET_HOME/repositories/{owner}/{repositoryName}.git/`.

Here is an example `post-receive` hook:

```bash
#!C:/Program Files/Git/usr/bin/sh.exe

#   author: Marshal Hayes 
#
#   Available environment variables:
# 
#       $OWNER              : String        the owner of the repository
#       $REPOSITORY_DIR     : String        the location of the repository on the remote
#       $REPOSITORY_NAME    : String        the name of the repository
#       $BRANCH_NAME        : String        the name of the branch pushed to
#       $SHA                : String        the sha hash
#       $COMMIT_MESSAGE     : String        the commit message
#       $COMMIT_USERNAME    : String        the username of the committer
#       $PUSHER             : String        the name of the pusher
#

export OWNER="$OWNER" REPOSITORY_DIR="$REPOSITORY_DIR" REPOSITORY_NAME="$REPOSITORY_NAME" BRANCH_NAME="$BRANCH_NAME" SHA="$SHA" COMMIT_MESSAGE="$COMMIT_MESSAGE" COMMIT_USERNAME="$COMMIT_USERNAME" PUSHER="$PUSHER"

if [[ $BRANCH_NAME == "master" ]] || [[ $BRANCH_NAME == "release" ]]
then
    exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\\hooks\\BUILD.ps1 >> .\\hooks\\output 2>&1
fi
```

## Building from Source

Installation from source is also an option when installing this plugin. 
To build from source, ensure the following are true first:

- Java is installed [jdk.java.net/14](https://jdk.java.net/14/)
- SBT is installed [scala-sbt.org](https://www.scala-sbt.org/)

Then, continue:

Download the source: `git clone https://github.com/marshalhayes/gitbucket-githooks-plugin && cd gitbucket-githooks-plugin`
Execute `sbt package` to package the source into a `.jar` file

## Security Concerns

This plugin *can* be dangerous if used improperly. But my thought is: if someone has access to place files directly on your server, then they clearly know what they're doing and so does the person who gave them access.

Keep in mind that the Git hooks are executable scripts and can perform essentially any action if given the proper access. Think `sudo rm -rf /` ... I can only imagine what that would do to your server. 

**To limit actions available in the script, look into limiting the user that is running the server.**
