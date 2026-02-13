# Squat Detection: Algorithm & Research-Level Extensions

## Algorithm Design (Current Implementation)

Squat detection uses **hip and head vertical displacement only** (no joint-angle math). This keeps the pipeline simple, robust to pose-estimation noise on knees/ankles, and **scale-invariant** so it works at different camera distances.

1. **Signal**  
   Normalized depth = (hip drop + head drop) / 2, divided by `refHeight = imageHeight * 0.45`. Depth is in [0, 1]; same thresholds work for near/far camera.

2. **Dynamic baseline**  
   When depth stays below a small threshold for 5 frames, we treat the pose as “standing” and update baseline hip Y and head Y (running average). Re-calibrated after each rep to limit drift.

3. **Exponential smoothing**  
   EMA on raw depth (`SQUAT_EMA_ALPHA = 0.25`) to reduce jitter.

4. **Movement velocity**  
   Velocity = smoothedDepth − prevDepth per frame. Used to require intentional motion: e.g. negative velocity to enter GOING_DOWN, positive to leave AT_BOTTOM (avoids counting tiny wobbles).

5. **Minimum bottom hold (0.2 s)**  
   Transition AT_BOTTOM → GOING_UP only after 200 ms at bottom. Reduces half-reps and bounces.

6. **Half-rep rejection**  
   A rep counts only if (maxDepth − minDepth) in that rep ≥ `SQUAT_MIN_DEPTH_RANGE_FOR_REP` and the bottom hold was satisfied.

7. **State machine**  
   UP → GOING_DOWN (depth + velocity) → AT_BOTTOM → [hold 0.2 s] → GOING_UP (velocity) → UP; rep validated at UP with cooldown.

---

## Suggestions for Research-Level Conversion

- **Person-specific calibration**  
  Store baseline (and optionally thresholds) per user or session; optionally one-time “stand still” and “squat once” calibration to set ref depth range.

- **Adaptive thresholds**  
  Learn `SQUAT_BOTTOM_DEPTH_THRESHOLD`, `SQUAT_MIN_DEPTH_RANGE_FOR_REP`, and velocity thresholds from the first N reps (e.g. percentiles of observed depth and velocity), then fix or slowly adapt.

- **Ground truth and metrics**  
  - Synchronize with force plate, IMU, or manual rep labels.  
  - Report precision, recall, F1, and (if possible) agreement (e.g. Cohen’s kappa) with human raters or other sensors.  
  - Publish a small dataset: pose sequences + rep labels + camera distance/angle.

- **Multi-modal fusion**  
  Fuse with depth camera (e.g. ToF) or wearable IMU for robustness and to study which modality contributes most in which conditions.

- **Ablation studies**  
  Compare: displacement-only (current) vs. displacement + velocity vs. displacement + hold time; report effect on precision/recall and false positives.

- **Camera distance / FOV**  
  Test at 1 m, 2 m, 3 m (and different FOVs); confirm that normalized depth keeps performance stable; if not, add a simple per-session scale factor (e.g. from estimated body height in frame).

- **Rep quality**  
  Extend the state machine or post-processing to label “shallow”, “good”, “deep” (e.g. by depth percentiles) and optionally “too fast” (velocity or time-in-phase) for coaching or research outcome variables.

These steps turn the current production logic into a reproducible, measurable pipeline suitable for a research report or dataset paper.
