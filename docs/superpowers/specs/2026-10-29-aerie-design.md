# Aerie — design

Date: 2026-10-29
Status: draft for review

## Intent

Building in survival is tedious without creative flight. Aerie lets one player fly like in creative, but only inside the chunks around their home base. That is where building happens, and it is the place where flight does not break survival balance.

The main risk is abuse: taking flight into dungeons or fights. Each design choice below exists to make Aerie great for building at home and useless anywhere else.

## Goals

- Creative-style flight (double-tap space) for exactly one player, in a set of chunks around a fixed shrine.
- Available after the Nether, well before the End. It costs blaze, diamonds, ender pearls and phantom membranes. It is never cheap, but it is never late-game either.
- Expandable to cover a large house through cheaper relay totems in neighbouring chunks.
- Every number is configurable by the server admin, and in singleplayer through the mod's config screen.

## Non-goals

- No portable item, armour, curio or wearable.
- No End items and no netherite in any recipe.
- No running fuel or power cost. The price is the build cost plus setup time.
- No height cap: flight works over the full height of a covered chunk.
- No open-sky requirement. The user rejected it for visual reasons.

## Anti-goals (failure even if it "works")

- Flight usable inside a dungeon, a structure far from home, or a fight.
- A single block that can be picked up and carried somewhere else.
- Clashing with other flight sources (creative mode, Ars Nouveau's Tablet of Flight, jetpacks). Aerie must never revoke flight it did not grant.
- Generic blocks such as "ender pearl block" that clash with other mods. Every block is Aerie-branded.

## Accepted gaps (the user chose these deliberately)

- **Shrine kits are portable.** Blocks drop normally, and a player may own several Cores. Someone could set up a shrine near a dungeon and wait out the charge. The combat cutoff and the setup time are considered enough of a deterrent.
- **No structure or dimension limits.** If a dungeon lies inside covered chunks, for example a stronghold under the base, flight works there. Shrines work in every dimension.

## Constraints

- NeoForge 1.21.1, Java 21. Match the user's other mod at `D:/git/distanthorizonsjourneymap`: ModDevGradle, Parchment, NeoForge `21.1.262`, and JDK at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` (the system default Java is 25 and will not work).
- Hard dependency: **Patchouli**, which is in the user's pack as `Patchouli-1.21.1-93-NEOFORGE`. It is used for the multiblock definitions, validation and the guide book. No other library dependencies.
- Has to coexist with the user's pack, notably Create 6.0.10, Create Aeronautics, and Ars Nouveau.
- The mod must be installed on both server and client (it adds blocks).

## Decided by the user

### Name
**Aerie**, mod id `aerie`. Blocks are named "Aerie …".

### Blocks
| Block | Role |
|---|---|
| Aerie Plinth | base block, used in shrine and relay |
| Aerie Column | middle pillar block, used in shrine and relay |
| Aerie Focus | top of each shrine corner pillar (1 ender pearl each) |
| Aerie Core | centre of the shrine. Holds ownership, charge and the network |
| Aerie Relay Crown | top of a relay totem. Its recipe totals 4 ender pearls |

### Base shrine — "Four-Pillar Shrine", 5×5, 3 tall
Side view (front), with the bottom row on the ground:
```
F . . . F
K . C . K
P . P . P
```
Top view: the Core sits at the centre on a Plinth. Each corner has a pillar made of Plinth, then Column, then Focus. The cells marked `.` are not checked, so the player can build or decorate there freely.
Total: 1 Core, 5 Plinths, 4 Columns, 4 Foci = 14 blocks. Target cost is about 8 ender pearls (4 in the Core, 1 per Focus).

### Relay totem — 1×1, 3 tall
A Plinth, then a Column, then a Relay Crown. The recipes for one totem add up to 4 ender pearls.

### Coverage
- The shrine covers the chunk its Core is in. That chunk is the "core chunk".
- A complete relay totem covers its own chunk, but only if:
  - its chunk neighbours a chunk that is already covered (so relays chain outward from the core chunk), and
  - its chunk is at most `maxRelayDistance` (default 5) chunks from the core chunk.
- Flight works over the full vertical height of every covered chunk.

### Attunement (charging)
- Once the shrine is complete, the Core charges for `chargeTimeSeconds` (default 300, which is 5 real minutes).
- While it charges, particles visibly build up: they flow from the Foci into the Core and grow stronger as the charge rises. Finishing the charge plays a short burst and a sound.
- Breaking or removing any shrine block stops flight and resets the charge to 0.

### Ownership
- One owner per Core. Only the owner can fly, and nobody else gets flight from it.
- Right-click on an unlocked Core makes the clicker the owner. A change of owner resets the charge to 0.
- Shift+right-click by the current owner locks the Core to them. Doing it again unlocks it. While it is locked, right-clicks by other players do not change the owner.
- Operators can unlock a Core they don't own with shift+right-click, and can then lock it to another player.
- Relays follow their Core's owner and lock state.

### Combat cutoff
- Flight is removed instantly when a monster damages the owner, or when the owner damages a monster in melee, with projectiles or in any other way. That applies even mid-air: fall damage is intended and there is no slow falling.
- Flight then stays blocked for `combatLockoutSeconds` (default 30).

### Leaving the area
- When the owner flies out of covered chunks, Aerie removes flight and gives `leaveGraceSeconds` (default 3) of Slow Falling.
- *Facilitator call:* the same grace applies to every non-combat loss of flight: a shrine block broken, a redstone off-signal, a change of owner, or a relay lost. Only the combat cutoff drops the player with no grace.

### Border particles
- While the owner is flying near the outer edge of the covered area, particles mark the boundary.

### Redstone
- A redstone signal into the Core switches off the Core and all its relays.

### Config
Every timing, limit and range is a config value, editable by the server admin and in singleplayer.

### Recipes
Normal datapack recipe JSONs, so pack makers and admins can rebalance them without code.

## Calls made by the brainstorm facilitator (not confirmed by the user — easy to change)

1. **Flight mechanism:** a transient modifier on NeoForge's `NeoForgeMod.CREATIVE_FLIGHT` attribute, under an Aerie-specific id. Aerie never touches `abilities.mayfly` directly. Removing the modifier only drops Aerie's flight, so creative mode or other mods' flight is untouched. When the modifier is removed and nothing else grants flight, the player falls.
2. **"Monster"** is any entity that implements `net.minecraft.world.entity.monster.Enemy`. The cutoff triggers in two cases:
   - the owner is the target and the damage's direct or causing entity is a monster, or
   - the owner is the causing entity (directly, or through arrows and similar) and the target is a monster.

   Players, neutral mobs and environmental damage do not trigger it. A mob only targeting the owner, without any damage, does not count either.
3. **Neighbouring** includes diagonals (8-neighbour). **Distance** is Chebyshev, which makes a square of radius 5 chunks around the core chunk. Both are configurable: `diagonalAdjacency=true`, `maxRelayDistance=5`.
4. **Relays activate instantly** once they are complete, connected and their Core is fully charged. They have no charge of their own.
5. **A relay belongs to exactly one Core.** That is the first Core whose network it connects to. The Core is stored on the relay and shown in its status message. Another player's Core can never use it, so there is no freeloading on someone else's relay chain. If the bound Core is gone, the relay rebinds to the next Core it connects to. A player has flight in chunk X if a charged, unpowered Core owned by them covers X.
6. **Charge only advances while the core chunk is loaded.** Progress is saved in the Core's block entity, so it survives restarts and unloads.
7. **Redstone pauses, it does not reset.** The charge is kept, and the network resumes when the signal goes away.
8. **Anti-move protection:**
   - All Aerie blocks go in the `create:non_movable` block tag, so Create contraptions and Aeronautics ships cannot assemble them. This is data only, with no compile dependency on Create.
   - As a backstop, the Core also stores the position it was attuned at. If it ever loads at a different position, the charge resets.
9. **Status feedback:** right-clicking the Core, or a Relay Crown, shows an action-bar message to the clicker. Examples: "Charging 63%", "Active — owner: X (locked)", "Incomplete shrine", "Disabled by redstone", "Relay: not connected".
10. **Operator** means permission level ≥ `opPermissionLevel` (default 2).
11. **Guide book:** a small Patchouli book, "Aerie Guide", crafted from a book and a feather. It has one page per structure with Patchouli's multiblock preview, plus a short rules page.
12. **Starting recipes**, all tunable through the datapack:
    - Plinth ×2: 8 polished blackstone around 1 blaze rod
    - Column ×2: 6 quartz pillar + 2 gold ingot + 1 phantom membrane
    - Focus: 1 ender pearl, 4 amethyst shards, 4 gold ingots
    - Core: 4 ender pearls, 4 diamonds, 1 phantom membrane (centre)
    - Relay Crown: 4 ender pearls, 4 gold ingots, 1 phantom membrane (centre)
    - Base total is about 8 pearls, 4 diamonds, 3 blaze rods, 4 membranes. A relay is about 4 pearls, 1 blaze rod, 2 membranes.
13. **Repo:** `D:/git/aerie`, licence MIT, group `com.loaderb0t.aerie`, author LoaderB0T, all matching the user's other mod.
14. **Creative and spectator players** are ignored by all Aerie logic.

## Left to the builder

- Internal architecture: how the network of covered chunks is stored and recomputed. A per-level `SavedData` registry of Cores and relays is suggested, so flight checks don't depend on loaded block entities. Also how often player checks run.
- Exact particle types, colours, sounds and the shapes of the block models.
- Textures. Simple, hand-made 16×16 textures are fine for the first version. The look is magic, ender purple and gold on dark blackstone.
- Config file layout and key names. Use NeoForge `ModConfigSpec` as a SERVER config so it is per-world and synced.
- Test approach. GameTests where practical, otherwise a manual test checklist in `runClient`.
