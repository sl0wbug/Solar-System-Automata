# Solar System Automata

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84.svg?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Physics](https://img.shields.io/badge/Integrator-Symplectic%20Verlet-FF6F00.svg?style=flat)](https://en.wikipedia.org/wiki/Verlet_integration)
[![Tests](https://img.shields.io/badge/Unit%20Tests-Passing-brightgreen.svg?style=flat)]()

**Solar System Automata** is a high-performance, hardware-accelerated N-body gravitational simulation engine built entirely in Kotlin and Jetpack Compose for Android. It simulates real astronomical multi-body gravitational mechanics, full planetary satellite systems with 288 confirmed moons, inelastic physical collisions, and interactive celestial body manipulation at a silky-smooth 60/120 FPS with a zero-allocation hot loop.

---

## 🌟 Key Features

### 🌌 1. Complete Solar System & 288 Confirmed Moons
- **297 Total Celestial Bodies**: The Sun, all 8 major planets, and all 288 confirmed natural satellites audited across the Solar System:
  - **Earth**: Luna
  - **Mars**: Phobos, Deimos
  - **Jupiter**: 95 confirmed moons (Io, Europa, Ganymede, Callisto, and minor satellite groups)
  - **Saturn**: 146 confirmed moons (Titan, Enceladus, Mimas, Iapetus, Rhea, Dione, etc.)
  - **Uranus**: 28 confirmed moons (Titania, Oberon, Umbriel, Ariel, Miranda, etc.)
  - **Neptune**: 16 confirmed moons (Triton, Proteus, Nereid, etc.)
- **Hill Sphere Distribution**: Moons are procedurally positioned inside their parent planet's gravitational Hill sphere ($r_H = a_p \sqrt[3]{M_p / 3 M_\odot}$) with golden-angle Keplerian circular velocities.

### 📐 2. Stevens' Psychophysical Power-Law Visual Scaling
- Solves the dynamic range problem of rendering microscopic moons ($R \sim 0.001\,R_\oplus$) alongside giant stars ($R \sim 109\,R_\oplus$) on a mobile screen:
  $$R_{\text{screen}} = 2.5 \cdot \left(\frac{R}{R_\oplus}\right)^{0.60} \cdot (\text{zoom})^{0.15} + 1.0\,\text{px}$$
- Ensures sub-pixel visibility, monotonic perceptual proportions, and zero runtime layout recalculation.

### ⚡ 3. Zero-Allocation N-Body Physics Engine
- **Symplectic Velocity Verlet Integrator**: Delivers energy-conserving long-term numerical stability for complex orbital mechanics.
- **Flat Primitive Structure-of-Arrays (SoA)**: Internal positions, velocities, masses, and accelerations are maintained in flat contiguous primitive arrays (`DoubleArray`), eliminating object allocations and GC pauses during the simulation loop.
- **Wait-Free Triple Buffer**: Physics calculations run on a dedicated background coroutine and publish immutable `RenderSnapshot` states to the UI thread without lock contention.
- **Register-Hoisted Acceleration**: Inner $O(N^2)$ pairwise loops cache coordinate registers to minimize memory bus pressure across hundreds of bodies.

### 💥 4. Inelastic Collisions & Mass Aggregation
- **Momentum & Mass Conservation**: Bodies that touch undergo inelastic coalescence, conserving total linear momentum ($\vec{P} = \sum m_i \vec{v}_i$) and volume-equivalent physical density.
- **$O(1)$ Swap-and-Pop Deletion**: Merged or ejected bodies are compacted immediately in the state buffers in constant time without array copying or fragmentation.
- **Dynamic Shockwave Rings**: Collisions emit expanding translucent dissipation rings with radial alpha falloff.

### 🔭 5. Glassmorphic Celestial Inspector & HUD
- **Real-Time Telemetry**: Tap any planet, star, or moon to inspect its live orbital velocity ($\text{AU/s}$), distance from barycenter ($\text{AU}$), mass ($M_\oplus$), and physical radius ($R_\oplus$).
- **Camera Follow Mode**: Lock onto fast-moving moons or planets with smooth centroid-tracking interpolation.
- **Ejection Controls**: Instantaneously fling or remove bodies from the system with automatic trail buffer cleanup.
- **Continuous Time Warp**: Seamlessly adjust simulation speed from $0.25\times$ to $100\times$ warp speed with a responsive logarithmic slider.
- **Slingshot Sandbox**: Drag and fling custom asteroids into orbit with interactive trajectory prediction vectors.

### 🪐 6. Canonical Multi-Body Scenario Presets
1. **Solar System**: Calibrated real-world system with 297 bodies in Astronomical Units (AU) and solar masses ($G = 39.478\,\text{AU}^3 / (M_\odot \cdot \text{yr}^2)$).
2. **Figure-8 Choreography**: The legendary Moore-Chenciner-Montgomery equal-mass 3-body solution tracing an unbroken figure-8 path.
3. **Binary Star & Circumbinary Planet**: Tatooine-like planetary system orbiting twin co-rotating stars.
4. **Lagrange Points & Trojans**: Gravitationally stable $L_4$ and $L_5$ Trojan asteroid swarms orbiting with Jupiter.
5. **Chaotic 3-Body (Burrau's Problem)**: Pythagorean 3-4-5 mass system demonstrating extreme multi-body sensitivity and gravitational ejections.

---

## 🏗️ Architecture Overview

```mermaid
graph TD
    subgraph Physics Engine [Background Coroutine]
        A[PhysicsState - Flat SoA Buffers] -->|dt Verlet Step| B[OrbitalIntegrator]
        B -->|Pairwise Gravity & Softening| B
        B -->|Collision Resolution & Coalescence| C[Inelastic Collisions]
        C -->|Swap-and-Pop O1| A
        A -->|Triple Buffer Publish| D[RenderSnapshot]
    end

    subgraph Hardware-Accelerated Viewport [Jetpack Compose]
        D -->|Draw Phase Invalidation| E[SimulationCanvas]
        F[CameraState - Pan & Pinch Zoom] -->|Focal Invariant World-to-Screen| E
        G[CelestialVisualScale - Stevens Power Law] --> E
        H[OrbitalTrailBuffer - Circular Ring Buffers] --> E
        I[StarfieldBuffer - Parallax Starfield] --> E
    end

    subgraph UI Overlay & Inspector
        J[SimulationControlsOverlay] -->|Time Warp / Reset / Presets| Physics Engine
        K[BodyInspectorCard] -->|Track / Eject| Physics Engine
        L[HitTester] -->|Touch Raycast| K
    end
```

---

## 📂 Project Structure

```
SolarSystemAutomata/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/droidlinkstd/solarsystemautomata/
│   │   │   │   ├── data/                 # Room DB entities and repository
│   │   │   │   ├── domain/physics/       # Velocity Verlet integrator, physics state, presets
│   │   │   │   ├── ui/
│   │   │   │   │   ├── camera/           # Centroid-invariant camera viewport & gestures
│   │   │   │   │   ├── inspector/        # Glassmorphic telemetry inspector card
│   │   │   │   │   ├── interaction/      # Hit testing & interactive slingshot state
│   │   │   │   │   ├── overlay/          # HUD controls, speed slider, preset switcher
│   │   │   │   │   ├── rendering/        # Canvas draw scope, trail buffer, visual scaling
│   │   │   │   │   └── theme/            # Sci-Fi neon dark aesthetic tokens
│   │   │   │   └── MainActivity.kt       # Edge-to-edge Compose entry point
│   │   └── test/                         # Unit tests covering integrators, collisions & moons
│   └── build.gradle.kts                  # Android configuration, ProGuard / R8 rules
├── gems/                                 # Architectural design logs & milestone synchronizations
│   ├── CANVAS_RENDERING_LOG.md
│   ├── COLLISION_PHYSICS_LOG.md
│   ├── INTERACTIVE_SANDBOX_LOG.md
│   ├── SCENARIO_PRESETS_LOG.md
│   ├── BODY_INSPECTOR_LOG.md
│   └── PLANETARY_MOONS_LOG.md
└── README.md
```

---

## 🛠️ Tech Stack & Tooling

- **Language**: [Kotlin 2.0](https://kotlinlang.org) (with modern Kotlin Compiler options)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3
- **Rendering**: Hardware-accelerated native Compose Canvas (`DrawScope` draw-phase invalidation without recomposition)
- **Persistence**: [Android Room 2.7.1](https://developer.android.com/training/data-storage/room) for celestial state persistence
- **Target Platform**: Android API 24+ (Compiled with Android 16 / SDK 36)
- **Code Shrinker**: ProGuard / R8 with release optimizations enabled

---

## 🚀 Building & Testing

### Prerequisites
- JDK 21
- Android Studio Ladybug / Meerkat or Android SDK Platform 36

### Run Unit Tests
```bash
./gradlew test
```

### Build Debug APK
```bash
./gradlew assembleDebug
```

---

## 📜 License
Solar System Automata is open-source software crafted with physics, geometry, and passion.
