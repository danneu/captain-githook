
# captain-githook

Inspired by https://github.com/logsol/Github-Auto-Deploy

Ol Githook is a simple server you can run on your remote server that will receive Git POST hooks from Github/Bitbucket and automatically update your repositories, optionally executing scripts.

## Usage

- Create a `~/captain-githook` directory.

```
~/
└── captain-githook/
    └── config.edn
```

- List your repositories in `~/captain-githook/config.edn`:

``` clojure
{:repos [{:url "git@bitbucket.org:danneu/klobb.git"}
         {:url "https://github.com:danneu/darkstrap.git"}]}
```

- Launch the captain out to sea:

```
$ java -jar captain-githook.jar <PORT>

Captain Githook is preparing to set sail.
---> Checking /home/danneu/captain-githook... Exists
---> Checking /home/danneu/captain-githook/config.edn... Exists
     - Found 2 repo(s)
---> Syncing ssh://git@bitbucket.org/danneu/klobb.git...
     Cloning into 'klobb'... Done.
---> Syncing https://github.com/danneu/darkstrap.git...
     Cloning into 'darkstrap'... Done.
```

- The captain will clone your repositories if they haven't yet been cloned.

```
~/
└── captain-githook/
    ├── bitbucket/
    │   └── danneu/
    │       └── klobb/
    │           └── ...
    ├── github/
    │   └── danneu/
    │       └── darkstrap/
    │           └── ...
    └── config.edn/
```

- Add `http://your.server:<PORT>` as a POST hook to Bitbucket and Github.
- Bitbucket and Github will now notify the captain whenever you update a repository.
- When the captain receives a notification, he runs `git pull origin` for the appropriate repository.
- (Optional) He then runs `~/captain-githook/{provider}/{owner}/{repository}/githook-deploy` which can contain arbitrary shell commands. (Unimplemented)

## Purpose

I'm new to running my own VPS and so far have been manually rsyncing up my changes to the multiple apps that I have running on it. It's tedious and I'm ready to graduate to a better workflow.

I wanted an automated, simple solution for my modest needs.

I found https://github.com/logsol/Github-Auto-Deploy after a google search and decided to implement the idea myself.

## Install & Launch

    $ git clone https://github.com/danneu/captain-github
    $ git 
    $ lein uberjar
    $ java -jar target/captain-githook.jar <PORT>
    
You can now direct POST hooks to `http://your.server:<PORT>`. 

The captain awaits.

## Misc details

### ~/captain-githook

Whenever captain-githook is launched, he creates his mighty vessel of the sea (a directory) if it doesn't exist:

    ~/captain-githook
    
This is where the good captain keeps your repositories.

### ~/captain-githook/config.edn

The captain doesn't have anything to do unless you provide at least one repository url in `~/captain-githook/config.edn`.

For example:

``` clojure
{:repos [{:url "git@bitbucket.org:danneu/klobb.git"}
         {:url "git@github.com:danneu/darkstrap.git"}]}
```

Given the above config, once captain-githook is launched, he will create this directory structure, cloning the repos if it hasn't yet:

    ~/
      - captain-githook/
        - bitbucket/
          - danneu/
            - klobb/
        - github/
          - danneu/
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
