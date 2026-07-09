v1.1 status: DONE - "complete the Wish system" milestone:
  - Threat Score replaces "most Wishes" for Hunter Event selection:
    (wishes * 5) + (bounty * 1) + (killStreak * 3), weights configurable
    under hunter-event.threat-weights in config.yml.
  - Kill streak tracking added to Bounty/PlayerData/DB (increments on kill,
    resets on any death, PvP or not).
  - Wish Shrine + unredeemed Wish Token flow: killing a player takes one of
    their Wishes immediately, but the killer only gets a physical,
    PDC-tagged "Wish Token" item, not the Wish itself. Must be carried back
    to the configured wish-shrine location and redeemed (right-click near
    it, or /wish redeem) to actually become an owned Wish. If the token
    carrier dies first, the new killer gets the token instead of it
    scattering as a normal death drop.

Next up, in rough priority order (per the user's latest list):
- Player Data: already saves coins/wishes/blessings/bounty/kill-streak: add
  hunter wins/survivals and crystals-claimed counters once those systems
  exist.
- Live Scoreboard (wishes/coins/bounty/hunter timer)
- Wish Crystals: random world spawns, broadcast coordinates, PvP hotspot,
  first-claim rewards (coins/wishes/loot)
- Shop + GUI system (Shop, Blessings, Stats, Leaderboard, Admin Panel menus)
- Leaderboards (bounty leaderboard exists via /bounty top; needs a proper
  GUI + wish-count/kill-streak/coins leaderboards)
- /outlawadmin: givewish, removewish, setwish, givecoins, setcoins,
  resetplayer, debug, backup, restore, crystal spawn
- PlaceholderAPI support
- Anti-abuse: repeat-victim cooldown, combat logging penalty, alt protection
- Season system: automatic resets, Hall of Fame, Outlaw King, rewards
- Reputation system (Fame/Infamy)
- QoL: join messages, boss bars, action bars, tab list, death messages,
  sounds/particles

Known design tradeoff worth flagging: an unredeemed Wish Token is a normal
physical item, so it can theoretically be destroyed (lava, void, explosion)
before it's redeemed - which technically breaks "Wishes cannot be
destroyed." Treated as intentional risk/reward tension for now; revisit if
it causes complaints (e.g. could make tokens explosion-proof/fire-proof).
