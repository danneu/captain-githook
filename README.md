
# captain-githook

Inspired by https://github.com/logsol/Github-Auto-Deploy

Ol Githook is a simple server you can run on your remote server that will receive Git POST hooks from Github/Bitbucket and automatically update your repositories, optionally executing scripts.

- Tracks any repos you've listed in `~/captain-githook/config.edn`
- Clones repos into `~/captain-githook/{provider}/{repo-name}`
- Listens for Git POST hooks
- Syncs repos with `git pull origin` when notified by Github/Bitbucket
- (Optional) Runs `~/captain-githook/{provider}/{repo-name}/githook-deploy` if provided after any time it receives an update for that repo.

    Captain Githook is preparing to set sail.
    ---> Checking /home/danneu/captain-githook... Exists
    ---> Checking /home/danneu/captain-githook/config.edn... Exists
         - Found 1 repo(s)
    ---> Syncing ssh://git@bitbucket.org/danneu/captain-githook.git...
    Cloning into 'captain-githook'... Done.

## Install & Launch

    $ git clone ...
    $ lein uberjar
    $ java -jar target/captain-githook.jar <PORT>
    
You can now direct POST hooks to `http://example.com:PORT`. 

The captain awaits.
    
## Usage

### `~/captain-githook`

Whenever captain-githook is launched, he creates his mighty vessel of the sea (a directory) if it doesn't exist:

    ~/captain-githook
    
This is where the good captain keeps your repositories.

### `~/captain-githook/config.edn`

The captain doesn't have anything to do unless you provide at least one repository url in `~/captain-githook/config.edn`.

For example:

``` clojure
{:repos [{:url "git@bitbucket.org:danneu/klobb.git"}
         {:url "git@github.com:danneu/darkstrap.git"}]}
```

(Only tested it with Bitbucket so far)

Given the above config, once captain-githook is launched, he will create this directory structure, cloning the repos if it hasn't yet:

    ~/
      - captain-githook/
        - bitbucket/
          - klobb/
        - github/
          - darkstrap/
        
For each repo, he will:

- Run `git pull origin`.
- Execute a `githook-deploy` script located in each repo root. (Unimplemented)

If the directory structure already exists, he will just run a `git pull` to ensure things are up to date and run the deploy script if anything changed.

Then he sits and waits for incoming POST hooks.

### Waiting for POST hooks

Whenever captain-githook receives a POST hook, he will first ensure that it's coming from a repo listed in `config.edn`.

If a matching repo exists, he will run `git pull` from the repo directory and then execute the repo's `githook-deploy` script.

## githook-deploy script

You can commit an optional `githook-deploy` file to any repository that captain-githook will run for each repository:

- After he starts (all repositories)
- After he receives a POST hook (one repository)

The primary use-case for this script is to restart web processes to ensure they pick up the latest changes.

## Disclaimer

I wrote this at 4am.

I have poor sysadmin skills. To put things in perspect, this project is 1000x better than what I was doing before.
