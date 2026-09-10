# Solar System Automata - Inelastic Collisions, Swap-and-Pop Compaction & Shockwaves
**Branch**: `feature/collision-physics`  
**Base Commit**: `a31d2b7` (Milestone 1 merged into `main`)  
**Status**: Milestone 3 Completed & Verified  
**Tracked For**: Gemini Vibe Coding Sync  

---

## 1. Feature Map & Architectural Summary
1. **Accurate Astronomical Unit Scale Conversion**:
   - Celestial body radii are defined in Earth Radii ($R_\oplus$).
   - Coordinates are defined in Astronomical Units (AU).
   - $\text{EARTH\_RADIUS\_TO\_AU} = 4.25875 \times 10^{-5}$.
   - Collision condition: $\Delta x^2 + \Delta y^2 \le \left((R_i + R_j) \cdot \text{EARTH\_RADIUS\_TO\_AU}\right)^2$.
2. **Physical Conservation Laws (Inelastic Merger)**:
   - Total Mass: $M_{\text{new}} = m_{\text{winner}} + m_{\text{absorbed}}$.
   - Linear Momentum Conservation: $\vec{v}_{\text{new}} = \frac{m_w \vec{v}_w + m_a \vec{v}_a}{M_{\text{new}}}$.
   - Center of Mass (Barycenter) Position: $\vec{r}_{\text{new}} = \frac{m_w \vec{r}_w + m_a \vec{r}_a}{M_{\text{new}}}$.
   - Volume Conservation: $R_{\text{new}} = \sqrt[3]{R_w^3 + R_a^3}$.
   - The larger mass absorbs the smaller mass (preserves name, color, and identity).
3. **Zero-Allocation In-Place Compaction ($O(1)$ Swap-and-Pop)**:
   - When body $A$ is absorbed, the last active body (`count - 1`) is copied into slot $A$.
   - The vacated last slot is zeroed out and `count` is decremented.
   - Zero heap allocations, zero object boxing, and zero GC pressure in the hot loop.
4. **Camera & Trail Buffer Synchronization**:
   - If the followed body was absorbed, `cameraState.stopFollowing()` is invoked.
   - If the followed body was at `count - 1` and swapped into slot $A$, `cameraState.followedBodyIndex = A`.
   - `OrbitalTrailBuffer.swapAndPop(absorbedIndex, swappedIndex)` moves historical trail points seamlessly to slot $A$.
5. **Zero-Allocation Shockwave FX (`ShockwaveBuffer.kt`)**:
   - Pre-allocated circular ring buffer managing up to 16 active expanding impact ripples.
   - Renders expanding, alpha-decaying rings beneath celestial bodies in `SimulationCanvas.kt`.
6. **Asynchronous Room Deletion**:
   - Absorbed bodies are deleted asynchronously from Room database by name via `CelestialBodyRepository` on `Dispatchers.IO`.

```mermaid
graph TD
    A[Velocity Verlet Integration Step] --> B[resolveCollisions Check Pairwise Distance]
    B -->|distSq <= collisionRadiusSq| C[Inelastic Collision Resolution]
    C --> D[Conserve Momentum, Mass, Barycenter & Volume]
    D --> E[O(1) Swap-and-Pop Compaction]
    E --> F[Trigger Shockwave in ShockwaveBuffer]
    E --> G[Sync Camera Follow & OrbitalTrailBuffer]
    E --> H[Async Delete Absorbed Body in Room via IO]
    E --> I[Recalculate Pairwise Accelerations with New Mass/Count]
    I --> J[Publish Updated RenderSnapshot]
```

---

## 2. Mathematical Formulations & Invariants

### Unit Scale Invariant
$$R_{\text{col}} = (R_i + R_j) \cdot 4.25875 \times 10^{-5}\,\text{AU}$$
$$\Delta x^2 + \Delta y^2 \le R_{\text{col}}^2$$

### Conservation of Linear Momentum
$$\vec{P}_{\text{initial}} = m_1 \vec{v}_1 + m_2 \vec{v}_2 = \vec{P}_{\text{final}} = M_{\text{new}} \vec{v}_{\text{new}}$$
$$\vec{v}_{\text{new}} = \frac{m_1 \vec{v}_1 + m_2 \vec{v}_2}{m_1 + m_2}$$

### Volume Conservation
$$V_{\text{new}} = V_1 + V_2 \implies \frac{4}{3}\pi R_{\text{new}}^3 = \frac{4}{3}\pi (R_1^3 + R_2^3) \implies R_{\text{new}} = \sqrt[3]{R_1^3 + R_2^3}$$

---

## 3. Verification & Test Suite
- `CollisionPhysicsTest`:
  - `inelasticCollisionConservesTotalLinearMomentum`: PASSED ($< 10^{-9}$ tolerance).
  - `inelasticCollisionConservesTotalMass`: PASSED ($M_{\text{final}} = M_{\text{initial}}$).
  - `inelasticCollisionConservesVolume`: PASSED ($R = \sqrt[3]{R_1^3 + R_2^3}$).
  - `barycentricPositionIsCenterOfMass`: PASSED.
  - `swapAndPopMaintainsArrayIntegrityAndZeroesVacatedSlot`: PASSED ($O(1)$ compaction).
  - `cameraFollowAndTrailBufferSyncOnCompaction`: PASSED.
  - `distantBodiesDoNotCollideUnderUnitScaleConversion`: PASSED.
- Pre-existing suites (`SlingshotStateTest`, `HitTesterTest`, `CameraStateTest`, `OrbitalIntegratorTest`): ALL PASSED.
- Gradle build: BUILD SUCCESSFUL.
