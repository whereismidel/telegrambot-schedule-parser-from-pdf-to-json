# Schedule bot

Required environment variables: `BOT_USERNAME`, `BOT_TOKEN`, `GOOGLE_CREDENTIALS`

## Build locally

Build:

```bash
$ docker build -t schedulebot . -f Dockerfile
```

Run:

```bash
$ docker run --env BOT_NAME=bot_name --env BOT_TOKEN=bot_token --env GOOGLE_CREDENTIALS="credentials.json" -it schedulebot
```

## Run from Docker Packages

```bash
$ docker run --env BOT_NAME=bot_name --env BOT_TOKEN=bot_token --env GOOGLE_CREDENTIALS="credentials.json" -it ghcr.io/whereismidel/shedulebotjava:main
```
