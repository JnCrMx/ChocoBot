# ChocoBot

ChocoBot is a simple Discord Bot written for only one server in first place, but then made public thanks to [@MGThePro](https://github.com/MGThePro).

## Configuration

```yaml
prefix: 'Prefix the Bot should use for commands'
database:
  type: 'sqlite'
  path: 'chocobot.sqlite'
reddit:
  enabled: whether memes from Reddit are enabled (defaults to true)
  username: 'Reddit username'
  password: 'Reddit password'
  appId: 'Reddit app id'
  appSecret: 'Reddit app secret'
discord:
  token: 'Discord access token'
github:
  enabled: whether GitHub issues are enabled (defaults to true)
  privateKey: 'Path to private key file'
  appId: GitHub App ID
  installationId: ID of App installation
  user: 'Username part of repository URL'
  repository: 'Repository part of repository URL'
server:
  commandChannel: 'ID of channel commands may be used in'
  warningChannel: 'ID of channel to post warnings in'
  remindChannel: 'ID of channel to post reminds in'
  mutedChannels:
    - 'IDs of channels that are muted for the subscription system'
  operatorRoles:
    - 'IDs of roles that are allowed to'
    - 'warn, unwarn, use commands everywhere, etc.'
```
