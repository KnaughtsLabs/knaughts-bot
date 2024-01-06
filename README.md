# Knaughts Bot
<img src="https://i.imgur.com/MLaEUFy.png" alt="The Knaughts logo." />
Knaughts bot is an intuitive note-taking bot for Discord. With Knaughts, you can conveniently create, view, delete and edit your encrypted notes.

Check out our [website](https://knaughts.xyz) and [Invite Knaughts to your Discord server](https://knaughts.xyz/invite).

## Usage
- `/note` - create a new note
- `/notes [id]` - get your notes (pass in an id to get just a specific note). The id is given when you create a note.
- `/about` - get info & links relating to Knaughts bot.

## About
Knaughts bot is created in Java with the [JDA](https://github.com/discord-jda/JDA) Discord wrapper. 
It uses [Pocketbase](https://pocketbase.io) as its database and [lazysodium](https://github.com/terl/lazysodium-java) for encryption.

## Prerequesites
- Java 17
- Maven

## Build
Knaughts is built with Java 17 and Maven.
```
mvn clean
```
```
mvn package
```
```
java -cp target/KnaughtsBot-1.0.jar xyz.knaughts.KnaughtsBot
```
