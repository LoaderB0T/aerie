# Aerie

A NeoForge 1.21.1 mod: build a shrine at your base and get creative flight in the chunks around it. It's for building, not for fighting.

See the [design](docs/superpowers/specs/2026-10-29-aerie-design.md).

## Install

Requires [Patchouli](https://modrinth.com/mod/patchouli) on both server and client.

```bash
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./gradlew build
```

Copy `build/libs/aerie-<version>.jar` into the instance's `mods` folder, or use `./gradlew installToProfile -PinstallDir="<instance>/mods"`.

Craft the **Aerie Guide** from a book and a feather. It shows both structures and can place a ghost preview.

## Config

Server config `serverconfig/aerie-server.toml`, per world. In singleplayer it's also editable from the Mods screen.

| Key | Default | Meaning |
|---|---|---|
| `chargeTimeSeconds` | 300 | attunement time once the shrine is complete |
| `combatLockoutSeconds` | 30 | flight blocked after hitting, or being hit by, a monster |
| `leaveGraceSeconds` | 3 | slow falling when flight ends for any non-combat reason |
| `maxRelayDistance` | 5 | max chunks between a relay and its Core |
| `diagonalAdjacency` | true | diagonal chunks count as neighbours for relay chains |
| `opPermissionLevel` | 2 | needed to lock or unlock other players' Cores |
| `borderParticles` | true | particles at the area edge while flying |
| `checkIntervalTicks` | 10 | how often each player's flight is re-checked |

Recipes are normal datapack JSON under `data/aerie/recipe/`.

## Tests

```bash
./gradlew test               # pure rules: ownership, charge, relay network
./gradlew runGameTestServer  # headless in-world tests: shrine, relays, flight, combat cutoff, data
```

## Manual test

Run with `./gradlew runClient`, with `chargeTimeSeconds=20` in `run/saves/<world>/serverconfig/aerie-server.toml`. M7 needs a second player.

- [ ] **M1** The guide's shrine page shows a 3D preview, and "visualize" places a ghost structure.
- [ ] **M2** A charged shrine lets you double-tap space to fly anywhere in the core chunk, including y=-60 and y=300. Purple particles flow into the Core while it charges, and a beacon sound plays when it finishes.
- [ ] **M3** Flying across the chunk edge gives about 3 s of slow falling, so you land safely. Back inside, you can fly again. Particles mark the edge as you approach it.
- [ ] **M4** A connected relay chunk allows flight, and so does a diagonal relay. The 6th chunk out in a straight chain does not. Right-clicking a crown shows its status.
- [ ] **M5** A zombie or skeleton hit while flying drops you instantly, with no slow falling. You can't fly for 30 s, even inside the area.
- [ ] **M6** Shooting a zombie with a bow while hovering drops you instantly.
- [ ] **M7** Player 2 can't fly in your area. Once you lock your Core, their right-click shows "locked to". An op shift-clicks to unlock, player 2 claims it, and the charge restarts at 0%.
- [ ] **M8** A lever on the Core drops flight (with slow falling) and keeps the percentage. Turning the lever off lets you fly again at once.
- [ ] **M9** Logging out mid-flight and back in outside the area means no flight. Dying and respawning in the area brings flight back. Going through a Nether portal means no flight on the other side.
- [ ] **M10** Creative to survival and back: creative flight is untouched, and in survival inside the area you can fly again within half a second.
- [ ] **M11** With Create installed, a contraption can't pick up an Aerie block.
