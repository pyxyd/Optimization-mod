Optimization for Tacz
=====================

A merged optimization / quality-of-life add-on for Timeless and Classics Zero (TaCZ).
It only changes how things look and feel -- no new attachment slots, no new items, and
nothing is written into the gun packs.

Links
-----
  GitHub   : https://github.com/pyxyd/Optimization-mod
  Modrinth : https://modrinth.com/mod/optimization-forge

The in-game Mods list also shows the GitHub link (Forge's displayURL field).

Requirements
------------
  Minecraft 1.20.1
  Forge 47.4.26 (any 47.x should work)
  TaCZ -- Timeless and Classics Zero 1.1.8+


Features
--------

1) Custom bullet holes
   The bullet hole on a block uses your own texture, floating one layer above the
   vanilla hole, then fades out. Which guns it applies to is configurable.

2) One-shot kill for chosen guns
   Guns listed in oneshot.guns kill any non-player mob on hit (default: the AWM).
   Players are never affected unless you turn that on.

3) Hybrid aim
   Tap the aim key to toggle aiming, hold it to aim only while held. Works with both
   mouse and keyboard bindings.

4) Auto-aim-before-fire
   Holding the fire button aims first and only then shoots, so the shot always leaves
   the barrel while already scoped. Per-gun on/off panel in game (default key: O).

5) Hit marker
   A single X flashes next to the crosshair. The shape never changes -- only the
   colour does:
       white  = hit a non-head hitbox
       orange = headshot
       red    = that shot killed the target (takes priority over orange)
   Plus a quiet hit-confirm sound and a very small 90 ms camera kick.

6) Hip-fire crosshair
   A Delta-Force style hollow cross drawn by this mod, with the gap driven by the
   real TaCZ hip-fire inaccuracy of the gun you are holding. It hides while aiming.

7) Crouch-aim: recoil only, no spread
   While crouching AND fully scoped in, bullet spread is forced to zero. Recoil,
   sound and damage are untouched.

8) Configurable gun knockback
   The gun pack data has bullet.knockback set to 1 on every gun, so hits actually
   push the target. (Data change, not code -- see the gun packs under tacz\.)


Configuration
-------------
Everything lives in one file:

    config/taczopt.properties

It hot-reloads within one second -- no game restart needed for config changes.
The file is rewritten with comments on first run and lists every key.

NOTE: gun pack data (tacz\...\guns\*_data.json) is read only at game STARTUP, so
changing anything there does require a full restart.


Source
------
The source tree ships separately as a zip:
    src\        Java sources
    resources\  mods.toml, pack.mcmeta, mixins json, assets, icon
    tools\      build-time checkers (see below)
    build.ps1   one-shot build script

Building requires JDK 17 and a Forge 1.20.1-47.4.26 installation; build.ps1 locates
the JDK and the Forge/SRG jars by itself. It refuses to produce a jar unless all of
these pass:

    ModsTomlCheck      parses mods.toml with the same library Forge uses
    MixinsCheck        every @Mixin class is registered in the mixins json
    InjectionCheck     walks TaCZ's bytecode to prove the mixin's target and its
                       parameter order still match
    CrosshairCheck     crosshair geometry, spread mapping, stance priority
    ConfigUpgradeCheck legacy config values get upgraded, dead keys get removed
    HitFeedbackCheck   marker geometry and the white / orange / red colour rules

License: MIT. The full text ships with the source repository:
    https://github.com/pyxyd/Optimization-mod/blob/main/LICENSE

The mod icon is based on TaCZ's own logo, and the TaCZ word mark remains the property
of the TaCZ project -- the MIT license grants no rights to it.
