# Turtabilty

> Slow and steady armor colors.  
> Built by **Tzy**, dedicated to **Turt**.

`Turtabilty` is a Fabric mod for Minecraft `1.21.8` that changes armor colors based on durability with clean, customizable transitions.

## Tiny Turtle

```text
   ___     _
 /`   `\  / )
|  .-.  |/ /
| |   | / /
| |   |/ /
| |   / /
| |  / /
| | / /
|_|/ /
(____/
```

## Features

- Durability-based armor tinting
- Section color mode and single-base-color mode
- Transition modes: `STEP`, `SMOOTH`, `FADE`
- Toggle for full-durability tint
- Material toggles (leather/chainmail/iron/golden/diamond/netherite/turtle/modded)
- Mod Menu + Cloth Config support
- Turt easter eggs for special item names

## Version

- Minecraft: `1.21.8`
- Fabric Loader: `0.18.4+`
- Fabric API: `0.136.1+1.21.8`

## Build

```powershell
./gradlew build
```

Main jar:

- `build/libs/turtabilty-1.0.0.jar`

## Config

- Main config: `config/turtabilty.json`
- Legacy config `config/turtaiblty.json` is auto-migrated

## Publish To GitHub

```powershell
git init
git add .
git commit -m "Initial release: Turtabilty 1.0.0"
git branch -M main
git remote add origin https://github.com/<your-user>/<your-repo>.git
git push -u origin main
```

Release tag:

```powershell
git tag -a v1.0.0 -m "Turtabilty v1.0.0"
git push origin v1.0.0
```

## License

`CC0-1.0` (see `LICENSE`)
