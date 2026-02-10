# JugaPlatform

Original 2D side-scrolling motorcycle physics game for Android (portrait only), inspired by the feel of classic gravity bike games.

## Controls
A single bottom virtual D-pad (8 directions):
- Up = Throttle
- Down = Brake
- Left = Lean back
- Right = Lean forward
- Diagonals combine these actions.

Hardware keys are also supported: DPAD UP/DOWN/LEFT/RIGHT (diagonals via combos).

## Features
- 20 easy offline levels (`app/src/main/assets/levels`)
- Fixed timestep physics loop with `SurfaceView + Canvas`
- Bike model with frame + 2 wheels + constraints
- Best times stored using DataStore Preferences
- Level progression and medals (gold/silver/bronze)
- Ghost replay (saved in app files as JSON snapshots)
- Optional checkpoints (+1.0s penalty on respawn)
- Share results via Android Sharesheet + FileProvider image attachment
- Haptics on crash/finish

## Level JSON schema
```json
{
  "id": 1,
  "name": "Easy Ride 1",
  "start": {"x": 2.0, "y": 6.0},
  "finishX": 60.0,
  "medals": {"gold": 10.9, "silver": 15.1, "bronze": 21.3},
  "segments": [{"x1":0,"y1":8,"x2":8,"y2":8}],
  "checkpoints": [{"x":22.0,"y":7.8}]
}
```

## Ghost format
Ghost files are stored in app internal files dir as `ghost_<level>.json`, each entry:
- `t`: elapsed time sample
- `fx`, `fy`, `fa`: frame position + angle
- `bx`, `by`: rear wheel
- `nx`, `ny`: front wheel

Sampling rate is approximately 20 Hz (`0.05s`).

## Extending levels
1. Add new JSON in `assets/levels/` using same schema.
2. Increase max level clamp in `GameActivity` / `ResultsActivity` if needed.
3. Tune medal thresholds in each level file.
