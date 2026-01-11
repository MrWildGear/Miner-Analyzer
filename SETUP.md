# Setup Instructions

## Prerequisites

### 1. Install Rust (Required for Tauri)

Since Tauri requires Rust, you need to install it first:

**Windows:**
1. Install Visual Studio Build Tools (required for Rust on Windows):
   - Download from: https://visualstudio.microsoft.com/downloads/#build-tools-for-visual-studio-2022
   - During installation, select "Desktop development with C++" workload
   - This installs the MSVC linker that Rust needs
2. Download and run the Rust installer from: https://rustup.rs/
3. Or use winget: `winget install Rustlang.Rustup`
4. After installation, restart your terminal/IDE

**Verify installation:**
```bash
cargo --version
rustc --version
```

### 2. Node.js (Already Installed ✅)

You have Node.js v22.20.0 installed - that's perfect!

## Setup Steps

### Step 1: Install Dependencies

```bash
npm install
```

This will install all the required packages including:
- React and TypeScript dependencies
- Tauri CLI and plugins
- UI components (shadcn/ui)
- Tailwind CSS

### Step 2: Add App Icons (Optional for now)

You can start the project without icons, but to build a final release, you'll need:

- `src-tauri/icons/32x32.png`
- `src-tauri/icons/128x128.png`
- `src-tauri/icons/128x128@2x.png` (256x256)
- `src-tauri/icons/icon.ico` (Windows)
- `src-tauri/icons/icon.icns` (macOS)

See `src-tauri/icons/README.md` for details.

### Step 3: Run in Development Mode

```bash
npm run tauri:dev
```

This will:
1. Start the Vite dev server (frontend)
2. Compile the Rust backend
3. Launch the Tauri application window

**Note:** The first build will take a few minutes as it compiles Rust dependencies.

## Troubleshooting

### If Rust is not installed:

1. Install Rust from https://rustup.rs/
2. Restart your terminal/IDE
3. Run `npm run tauri:dev` again

### If you get permission errors:

On Windows, make sure you're running the terminal as Administrator if needed.

### If you get "link.exe not found" error:

This means Visual Studio Build Tools are not installed. On Windows, Rust requires the MSVC linker.

**Solution:**
1. Install Visual Studio Build Tools: https://visualstudio.microsoft.com/downloads/#build-tools-for-visual-studio-2022
2. During installation, make sure to select "Desktop development with C++" workload
3. After installation, restart your terminal/IDE
4. Run `npm run tauri:dev` again

### If build fails:

- Make sure Rust is properly installed: `cargo --version`
- Make sure Visual Studio Build Tools are installed (see above)
- Try cleaning and reinstalling: 
  ```bash
  npm cache clean --force
  npm install
  ```

## Available Commands

- `npm run dev` - Run Vite dev server only (no Tauri window)
- `npm run tauri:dev` - Run full Tauri app in development mode
- `npm run build` - Build frontend only
- `npm run tauri:build` - Build complete Tauri application
- `npm run typecheck` - Type-check TypeScript code
- `npm run lint` - Run ESLint
- `npm run format` - Format code with Prettier

## Next Steps

Once the app is running:
1. Select miner type (ORE, Modulated, or Ice)
2. Copy item stats from EVE Online (Ctrl+C)
3. The app will automatically analyze and display results!
