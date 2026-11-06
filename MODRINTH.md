# Aerie

**Creative flight at home, for building. Not for fighting.**

Building something big in survival is a chore without flight: scaffolding, pillaring up and down, one missed step and you fall. Aerie lets you build a magical shrine at your base. Once it's attuned, you can fly like in creative mode, but only in the chunks around your home. Leave the area or pick a fight, and you're back on the ground.

It's the comfort of creative building without breaking survival. Your base is where you build, and nobody dungeon-crawls there anyway.

<!-- SCREENSHOT 1 (hero): flying in survival, looking down at the shrine.
     Upload to the gallery, then replace the URL below. -->
![Flying in survival above an attuned Aerie shrine](https://cdn.modrinth.com/REPLACE_ME/flying.png)

## How it works

1. **Build the Four-Pillar Shrine.** It's a 5×5 square: an *Aerie Core* on an *Aerie Plinth* in the middle, and on each corner a pillar of Plinth, *Aerie Column* and *Aerie Focus*. The other spaces inside are free to decorate.
2. **Right-click the Core to claim it.**
3. **Let it attune.** It takes about 5 minutes, while enchanting glyphs stream from the four Foci into the Core.
4. **Fly.** Double-tap jump anywhere in the Core's chunk, from bedrock to build height.

<!-- SCREENSHOT 2: your setup screenshot, the finished shrine up close. -->
![The Four-Pillar Shrine](https://cdn.modrinth.com/REPLACE_ME/shrine.png)

## Bigger builds: Relay Totems

One chunk not enough for your castle? Place a **Relay Totem** (Plinth, Column, *Relay Crown*) in a neighbouring chunk to extend the flight area. Chain them outward up to 5 chunks from the Core. Each relay belongs to the player who built it, so nobody can borrow your chain.

## Built to stay fair

- **Only the owner flies.** Sneak-right-click to lock the Core to yourself. Operators can unlock any Core.
- **No flying fights.** Hit a monster or get hit by one and you drop instantly, with no flight for 30 seconds.
- **Gentle landings.** Leave the area, break the shrine or switch it off, and you get a few seconds of slow falling instead of a fatal drop. Logging out mid-air is safe too.
- **You see the edge.** A violet wall of particles shows the boundary as you fly near it.
- **Not a portable cheat.** The shrine is 14 blocks and takes 5 minutes to attune. Moving it means rebuilding and attuning again. Create contraptions can't pick it up.
- **Plays nice with other mods.** Aerie only ever takes away flight it gave you. Creative mode, Ars Nouveau's Tablet of Flight and similar keep working.
- **Redstone off switch.** A signal into the Core pauses the whole network.

## Cost

Available after the Nether, well before the End. No End items, no netherite.

| Block | Made from |
|---|---|
| Aerie Plinth ×2 | 8 polished blackstone, 1 blaze rod |
| Aerie Column ×2 | 6 quartz pillars, 2 gold ingots, 1 phantom membrane |
| Aerie Focus | 1 ender pearl, 4 amethyst shards, 4 gold ingots |
| Aerie Core | 4 ender pearls, 4 diamonds, 1 phantom membrane |
| Aerie Relay Crown | 4 ender pearls, 4 gold ingots, 1 phantom membrane |

A full shrine costs about **8 ender pearls and 4 diamonds**. Each relay costs about **4 ender pearls**. Recipes are normal datapack files, so pack makers can rebalance them.

## Guide book

Craft the **Aerie Guide** from a book and a feather. It explains the rules and shows both structures in 3D. Press *visualize* to place a ghost preview right where you want to build.

## Configuration

Everything is configurable per world (`serverconfig/aerie-server.toml`), and in singleplayer from the Mods screen:

| Setting | Default |
|---|---|
| Attunement time | 300 s |
| Combat lockout | 30 s |
| Slow-fall grace | 3 s |
| Max relay distance | 5 chunks |
| Diagonal relay neighbours | on |
| Operator permission level | 2 |
| Border particles | on |

## Requirements

- Minecraft **1.21.1**, **NeoForge**
- **[Patchouli](https://modrinth.com/mod/patchouli)**, on both client and server
