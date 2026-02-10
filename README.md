# JugaPlatform

JugaPlatform is a portrait-only Kotlin Android game designed to reproduce the **feel** of classic Gravity Defied gameplay using original code and assets.

## Controls (8-way D-pad)
Bottom-center virtual D-pad with 8 sectors:
- **UP** = throttle (rear wheel motor)
- **DOWN** = brake
- **LEFT** = lean back
- **RIGHT** = lean forward
- Diagonals combine actions (e.g. UP+RIGHT = throttle + lean forward)

Hardware keys also work: DPAD up/down/left/right.

## Gameplay/physics overview
- Fixed timestep simulation (`1/60`) + accumulator loop.
- Bike model = chassis + rear wheel + front wheel.
- Distance constraints (spring-like) keep bike shape stable.
- Wheel-vs-polyline collision against level segments.
- Traction via tangential friction impulse; rear wheel motor torque drives climbing.
- Brake increases damping/friction response.
- Lean torque gives balance control on ground and stronger air control.
- Crash when head point collides hard with track.

Core files:
- `physics/Vec2.kt`
- `physics/RigidBody2D.kt`
- `physics/Constraint.kt`
- `physics/Collision.kt`
- `physics/PhysicsWorld.kt`
- `game/GameLoop.kt`
- `game/GameState.kt`
- `game/GameRenderer.kt`
- `game/GameSurfaceView.kt`

## Level JSON schema
Levels live at `app/src/main/assets/levels/level_01.json ... level_20.json`.

```json
{
  "id": 1,
  "name": "Trail 1",
  "start": {"x": 2.0, "y": 6.6},
  "finishX": 64.0,
  "medals": {"gold": 11.95, "silver": 16.15, "bronze": 22.35},
  "segments": [
    {"x1":0,"y1":8.2,"x2":4,"y2":8.52}
  ],
  "checkpoints": [
    {"x":21.76,"y":7.9}
  ]
}
```

## Adding levels
1. Add a new JSON file into `assets/levels/`.
2. Keep polyline continuity for main ride path.
3. Set medal thresholds in seconds.
4. Increase max level clamp if adding beyond 20.

## Tuning knobs
Primary feel parameters are in `PhysicsWorld.kt`:
- gravity
- motor/brake force
- lean ground/air torque
- friction
- solver iterations
- damping

