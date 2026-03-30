# Gaussian Splat Studio

A cross-platform desktop application for creating and viewing [3D Gaussian Splats](https://repo-sam.inria.fr/fungraph/3d-gaussian-splatting/) — built with JavaFX 21 and Maven.

Takes images or video as input, runs the full reconstruction pipeline (frame extraction → camera pose estimation → Gaussian Splat training), and provides an interactive 3D viewer for the results.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [First Run Setup](#first-run-setup)
- [Workflows](#workflows)
  - [Creating a Project from Images](#creating-a-project-from-images)
  - [Creating a Project from Video](#creating-a-project-from-video)
  - [Running the Full Pipeline](#running-the-full-pipeline)
  - [Training Manually](#training-manually)
  - [Viewing a Gaussian Splat](#viewing-a-gaussian-splat)
  - [Merging Splats](#merging-splats)
- [Training Parameters](#training-parameters)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Building & Running](#building--running)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Tool | Version | Purpose | Install |
|------|---------|---------|---------|
| **Java JDK** | 21+ | Application runtime | [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/) |
| **Maven** | 3.9+ | Build tool | [maven.apache.org](https://maven.apache.org/download.cgi) |
| **COLMAP** | 3.8+ | Camera pose estimation (Structure-from-Motion) | [colmap.github.io](https://colmap.github.io/install.html) |
| **Python** | 3.8+ | Runs the 3DGS training script | [python.org](https://www.python.org/downloads/) |
| **Gaussian Splatting repo** | — | Training scripts | `git clone https://github.com/graphdeco-inria/gaussian-splatting` |
| **FFmpeg** | 5.0+ | Video frame extraction | [ffmpeg.org](https://ffmpeg.org/download.html) |

### Required Hardware

- **NVIDIA GPU** with CUDA support (training requires significant VRAM — 8 GB+ recommended)
- CUDA toolkit installed and compatible with your GPU drivers

### Optional

- **Conda** (Anaconda/Miniconda) — for isolated Python environment management

### Setting Up the Gaussian Splatting Environment

```bash
# Clone the repo
git clone https://github.com/graphdeco-inria/gaussian-splatting.git
cd gaussian-splatting

# Option A: Using Conda (recommended)
conda create -n gaussian_splatting python=3.8
conda activate gaussian_splatting
pip install -r requirements.txt

# Option B: Using venv
python -m venv venv
# Windows: venv\Scripts\activate
# Linux/macOS: source venv/bin/activate
pip install -r requirements.txt
```

---

## Installation

```bash
git clone https://github.com/your-username/gaussian-splat.git
cd gaussian-splat
mvn clean compile
```

---

## First Run Setup

1. **Launch the application:**
   ```bash
   cd gaussian-splat
   mvn javafx:run -pl app
   ```

2. **Open Settings** — click `File → Settings` or the **Settings** toolbar button.

3. **Configure tool paths** — either click **Auto-Detect All** to scan your system PATH, or manually browse to each executable:

   | Field | What to set |
   |-------|-------------|
   | FFmpeg | Path to `ffmpeg` (or `ffmpeg.exe` on Windows) |
   | COLMAP | Path to `colmap` (or `colmap.exe`) |
   | Python | Path to the Python interpreter with 3DGS dependencies installed |
   | Gaussian Splatting Repository | Folder where you cloned `gaussian-splatting` |
   | Conda (optional) | Path to `conda` executable |
   | Conda Env Name (optional) | e.g. `gaussian_splatting` |
   | GPU Device | GPU index (0 for first GPU, 1 for second, etc.) |

4. Click **Apply** to save. Settings are stored at `~/.gsplat/settings.json`.

---

## Workflows

### Creating a Project from Images

Best for when you already have a set of photos taken from multiple angles around a subject.

1. Click **File → New Project** (or `Ctrl+N`).
2. Enter a **project name** and choose a **location** on disk.
3. Select **Images** as the input type.
4. Click **Add Images...** and select your photos (supports PNG, JPG, JPEG, TIFF, BMP).
   - Aim for 50–200 images with good overlap between views.
   - Avoid blurry, over/under-exposed, or otherwise low-quality images.
5. Click **Finish** — the project directory structure is created.

### Creating a Project from Video

Best for when you have a video walkthrough or orbit of a subject.

1. Click **File → New Project**.
2. Enter a **project name** and **location**.
3. Select **Video** as the input type.
4. Click **Browse...** and select your video file (MP4, AVI, MOV, MKV, WEBM).
5. Set the **Frame extraction FPS** (default: 2.0).
   - Lower FPS (1–2) = fewer frames, faster processing, may miss detail.
   - Higher FPS (5–10) = more frames, better coverage, slower COLMAP.
   - For a slow orbit, 2 FPS is usually sufficient.
6. Click **Finish**.

### Running the Full Pipeline

The automated pipeline runs all stages end-to-end:

```
Video → FFmpeg (frame extraction) → COLMAP (SfM) → 3DGS Training → .ply splat file
```

If you started from images, the FFmpeg step is skipped automatically.

**To run:** (Pipeline integration is available via the services — connect it through the Training panel or programmatically via `PipelineService`.)

The pipeline stages and their approximate progress:
| Stage | Progress | What happens |
|-------|----------|-------------|
| Frame Extraction | 0–20% | FFmpeg extracts PNG frames from video at the configured FPS |
| COLMAP Feature Extraction | 20–30% | Detects visual features (SIFT) in each image |
| COLMAP Matching | 30–40% | Finds matching features between image pairs |
| COLMAP Mapping | 40–50% | Reconstructs 3D point cloud and camera poses |
| 3DGS Training | 50–100% | Trains the Gaussian Splat model (GPU-intensive) |

### Training Manually

For more control over the training process:

1. Switch to the **Training** tab.
2. Set the **Source Path** — point to your COLMAP output directory (the one containing the `sparse/` folder).
3. Set the **Output Path** — where the trained model will be saved.
4. Adjust [training parameters](#training-parameters) as needed (or keep defaults).
5. Click **Start Training**.
6. Monitor progress:
   - **Progress bar** and **iteration counter** update in real time.
   - **Loss chart** — should decrease over time (lower = better reconstruction).
   - **PSNR chart** — should increase over time (higher dB = better quality).
   - **Training log** — shows raw output from the training script.
7. You can **Pause** (preserves checkpoint for later resumption) or **Cancel** training at any time.

### Viewing a Gaussian Splat

1. Switch to the **Viewer** tab.
2. Click **Open Splat...** in the toolbar.
3. Select a `.ply`, `.ksplat`, or `.splat` file.
4. The splat loads in the interactive 3D viewer. Use your mouse to orbit, zoom, and pan.
5. Use the sidebar controls:

   | Control | Range | Effect |
   |---------|-------|--------|
   | Transparency | 0–100% | Adjusts splat opacity |
   | Point Scale | 0.1–5.0× | Scales the Gaussian splat size |
   | Camera Speed | 0.1–5.0× | Controls navigation speed |
   | Reset Camera | — | Returns to default view position |

### Merging Splats

The `PlyMerger` utility can combine two PLY splat files:

- Both files must have the same property layout (same number/types of attributes per vertex).
- The result is a concatenation of both point clouds into a single `.ply` file.
- For best results, the two splats should be aligned (same coordinate system) before merging.

---

## Training Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| **Iterations** | 30,000 | Total training iterations. More = better quality, slower. 7k is a fast preview, 30k is standard. |
| **Position LR (init)** | 0.00016 | Initial learning rate for Gaussian positions |
| **Position LR (final)** | 0.0000016 | Final learning rate for positions (decays over training) |
| **Scaling LR** | 0.005 | Learning rate for Gaussian scale/size |
| **Rotation LR** | 0.001 | Learning rate for Gaussian orientation |
| **Opacity LR** | 0.05 | Learning rate for Gaussian transparency |
| **Densify Grad Threshold** | 0.0002 | Gradient threshold for adding new Gaussians |
| **Densify From Iter** | 500 | Start densification at this iteration |
| **Densify Until Iter** | 15,000 | Stop densification at this iteration |
| **Densification Interval** | 100 | Densify every N iterations |
| **Anti-aliasing** | Off | Enable EWA filtering for anti-aliased rendering |
| **Optimizer** | `default` | `default` or `sparse_adam` (2.7× faster) |
| **Train/Test Split** | Off | Hold out images for evaluation metrics |

**Tips:**
- For a quick preview, try 7,000 iterations.
- For production quality, use 30,000+ iterations.
- If results look "blobby," try lowering the densify gradient threshold.
- The `sparse_adam` optimizer is significantly faster with minimal quality loss.

---

## Project Structure

When you create a project, the following directory structure is created:

```
MyProject/
├── input/          # Source images (or extracted video frames)
├── colmap/         # COLMAP workspace
│   ├── database.db # Feature database
│   └── sparse/     # Reconstructed camera poses & 3D points
│       └── 0/
│           ├── cameras.bin
│           ├── images.bin
│           └── points3D.bin
├── output/         # Trained model files
│   └── point_cloud/
│       └── iteration_30000/
│           └── point_cloud.ply  ← this is your Gaussian Splat
├── splats/         # Exported/converted splat files
└── project.json    # Project metadata
```

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  JavaFX UI (Java 21, Maven, JPMS)               │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ Project  │ │ Training │ │  Viewer          │ │
│  │ Manager  │ │ Panel    │ │  (WebView +      │ │
│  │ + Wizard │ │ + Charts │ │  GaussianSplats  │ │
│  │          │ │          │ │  3D / Three.js)  │ │
│  └────┬─────┘ └────┬─────┘ └──────────────────┘ │
│  ┌────▼─────────────▼──────────────────────────┐ │
│  │ Process Orchestrator (ProcessBuilder)       │ │
│  │ Async execution, stdout/stderr streaming,   │ │
│  │ cancellation, conda env activation          │ │
│  └─────────────────────────────────────────────┘ │
└───────────┬──────────────┬──────────────┬────────┘
       ┌────▼────┐   ┌─────▼─────┐  ┌────▼────┐
       │ FFmpeg  │   │  COLMAP   │  │ Python  │
       │ (native)│   │  (native) │  │  3DGS   │
       └─────────┘   └───────────┘  └─────────┘
```

**Key design decisions:**
- **JavaFX WebView** for the 3D viewer — hosts GaussianSplats3D (Three.js-based) via an embedded HTTP server on localhost.
- **ProcessBuilder** for all external tool invocation — no JNI or native code needed.
- **Observable properties** — services expose JavaFX observable properties (progress, loss, PSNR, iteration) that the UI binds to for live updates.
- **`~/.gsplat/settings.json`** — persistent app-wide configuration.

### Source Code Layout

```
gaussian-splat/
├── pom.xml                              # Parent POM
├── app/
│   ├── pom.xml                          # App module POM
│   └── src/main/
│       ├── java/
│       │   ├── module-info.java
│       │   └── net/talaatharb/gsplat/
│       │       ├── App.java             # Entry point
│       │       ├── ui/                  # FXML controllers
│       │       ├── model/               # Data classes
│       │       ├── service/             # Pipeline & tool services
│       │       └── util/                # Log parsing, PLY merging
│       └── resources/
│           ├── fxml/                    # UI layouts
│           ├── css/styles.css           # Dark theme
│           └── web/viewer.html          # 3D splat viewer
```

---

## Building & Running

```bash
# Compile
mvn clean compile

# Run the application
mvn javafx:run -pl app

# Package as JAR
mvn clean package
```

> **PowerShell note:** If passing Maven `-D` properties with dots or commas, use `mvn --% ...` to avoid shell parsing issues.

---

## Troubleshooting

### Application won't launch
- Ensure you have **JDK 21+** installed: `java -version`
- Ensure **Maven 3.9+** is installed: `mvn -version`
- Run `mvn clean compile` first to verify the build succeeds.

### COLMAP fails with "too few images"
- COLMAP needs sufficient overlap between images. Ensure you have at least 20–30 images with good coverage of the scene.
- Images should have at least 60–70% overlap between consecutive views.

### Training fails immediately
- Verify your Python environment has all gaussian-splatting dependencies installed.
- Check that `CUDA_VISIBLE_DEVICES` is set to a valid GPU index in Settings.
- Ensure your GPU has enough VRAM (8 GB minimum recommended).
- Check the training log in the Training panel for specific error messages.

### Viewer shows blank or errors
- The viewer requires an internet connection on first load (to fetch Three.js and GaussianSplats3D from CDN).
- Ensure your firewall isn't blocking localhost connections.
- Try loading a known-good `.ply` file to rule out file format issues.

### FFmpeg frame extraction is slow
- This is normal for large video files. Lower the FPS setting to extract fewer frames.
- SSD storage significantly speeds up frame writing.

### Settings aren't saving
- Check that `~/.gsplat/` directory is writable.
- On Windows, this is typically `C:\Users\<username>\.gsplat\settings.json`.

### "Python or GS repo path not configured"
- Open **Settings** and ensure both **Python** and **Gaussian Splatting Repository** paths are set.
- Click **Auto-Detect All** or browse manually to the correct paths.
