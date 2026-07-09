# OutlawSMP

A Paper 1.21.x plugin implementing the core OutlawSMP gameplay loop:
Wishes, Blessings, PvP stealing, Bounties, Coins, and the Hunter Event.

This project began life as "WishHunters" and was renamed/expanded into
OutlawSMP — package `com.outlawsmp`, main class `OutlawSMP`.

## Status (v1.0, in progress)

Implemented and working:

- **SQLite persistence** (`DatabaseManager`) — `players` + `wishes` tables,
  connection opened on enable, closed on disable, shaded `sqlite-jdbc` driver
  (relocated to `com.outlawsmp.libs.sqlite`) so no external dependency is
  needed on the server.
- **PlayerData** — full in-memory model: coins, bounty, list of owned
  Wishes, and which Wishes' blessings are currently active.
- **WishManager** — rolls weighted-random blessings for new Wishes (Common /
  Rare / Epic / Legendary), grants starting Wishes to new players, transfers
  a Wish (with its blessing) from victim to killer on a PvP kill, and
  applies/removes live blessing effects (potion effects + a namespaced
  max-health `AttributeModifier` for the "+2/+4 Hearts" blessings).
- **Join/Quit persistence** — `PlayerJoinListener` loads (or creates) a
  player's data asynchronously and re-applies their active blessings (and
  hands them a Hunter's Compass if an event is currently running);
  `PlayerQuitListener` saves and unloads them, and cancels the Hunter Event
  if The Hunted disconnects. `onDisable` does a final synchronous save-all.
- **PvP Wish stealing** — `PlayerDeathListener` steals exactly one Wish from
  the victim (if they have any) on a PvP kill; 0-Wish players simply lose all
  blessings until they steal one back, no ban.
- **BountyManager** — every PvP kill grows the killer's bounty
  (`kill-bounty-increase` in config.yml); killing a player with a bounty pays
  the killer that many coins and resets the victim's bounty to zero.
- **EconomyManager** — simple coin ledger (deposit/withdraw/set), completely
  separate from Wishes. Wishes are never purchasable.
- **Hunter Event** (`HunterManager`) — every `hunter-event.interval-minutes`,
  the online player with the highest **Threat Score** —
  `(wishes × 5) + (bounty × 1) + (killStreak × 3)`, weights configurable
  under `hunter-event.threat-weights` — becomes The Hunted. This rewards
  aggressive, dangerous play over just quietly hoarding Wishes. Everyone
  online gets a "Hunter's Compass" that live-tracks them, and a broadcast
  announces it. Surviving the configured duration pays out
  `survive-reward-coins`. Killing The Hunted pays the killer
  `kill-bonus-coins` on top of the normal bounty payout and Wish steal that
  any PvP kill already triggers. Handles mid-event disconnects and admin
  start/stop cleanly.
- **Wish Shrine + unredeemed Wish Tokens** — Wishes no longer change hands
  instantly on a kill. The victim loses a Wish immediately, but the killer
  only receives a physical, stealable "unredeemed Wish Token" item encoding
  that Wish's blessing. It must be carried back to the configured
  `wish-shrine` location and redeemed (right-click near it, or `/wish
  redeem`) before it becomes an owned Wish and its blessing can be
  activated. If the token carrier dies before redeeming, whoever kills them
  gets the token instead — a second layer of PvP risk on top of the kill
  itself. Epic/Legendary redemptions are broadcast server-wide.
- **Kill streaks** — tracked per player (`Bounty.killStreak`), incremented on
  a PvP kill and reset on any death (PvP or not). Feeds into Threat Score
  and is shown in `/stats`.
- **Commands**: `/wish [list|redeem|activate <#>|deactivate <#>]` (blessing
  changes are restricted to a configurable radius around spawn), `/balance
  [player]`, `/bounty [player|top]`, `/stats [player]`, `/hunter` (event
  status), and `/outlawadmin <reload|event start [player]|event stop>`.

Not yet implemented (from the original OutlawSMP wishlist, tracked in
`NEXT_STEPS.md`): Wish Crystals, Shop/GUI system, full player statistics
(hunter wins/survivals, crystals claimed, playtime), scoreboard,
PlaceholderAPI, seasons, reputation/infamy, anti-abuse
(cooldowns/combat logging), and the rest of `/outlawadmin`.

## Building

This environment has no outbound access to Maven Central, so the plugin jar
is built via GitHub Actions (`.github/workflows/build.yml`) on every push.
Locally, once you have Maven + a Spigot/Paper 1.21.1 API artifact available:

```
mvn package
```

The shaded jar is produced at `target/OutlawSMP.jar`.

## Config

See `src/main/resources/config.yml` for `starting-wishes`,
`max-active-blessings`, `blessing-change-radius`, `starting-coins`,
`kill-bounty-increase`, `minimum-kill-payout`, the `hunter-event.*` block
(including `threat-weights`), the `wish-shrine.*` block, and the database
filename.
